/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package log4j.layout.bunyan;

import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.NumberConverter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LogEventJsonWriter implements JsonWriter.WriteObject<LogEvent> {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int BUNYAN_VERSION = 0;
    private static final byte[] VERSION_BYTES = String.format("\"v\":%d", BUNYAN_VERSION)
            .getBytes(StandardCharsets.UTF_8);
    private static final String HOSTNAME = lookupHostname();
    private static final byte[] PID_BYTES = String.format("\"pid\":%d", lookupPid())
            .getBytes(StandardCharsets.UTF_8);
    private static final ThreadLocal<StringBuilder> buffer = ThreadLocal.withInitial(StringBuilder::new);

    private final byte[] eol;
    private final int maxMessageLength;
    private final KeyValuePair[] dynamicAdditionalFields;
    private final KeyValuePair[] staticAdditionalFields;
    private final boolean includeAllContextProperties;
    private final StrSubstitutor strSubstitutor;
    private final ThrowablePatternConverter throwablePatternConverter;
    private final DatePatternConverter datePatternConverter = DatePatternConverterFactory.instance();
    private final ContextPropertiesTriConsumer contextPropertiesTriConsumer =
            new ContextPropertiesTriConsumer(buffer);

    public LogEventJsonWriter(final ThrowablePatternConverter throwablePatternConverter,
                              final KeyValuePair[] additionalFields,
                              final StrSubstitutor strSubstitutor,
                              final boolean includeAllContextProperties,
                              final byte[] eol,
                              final int maxMessageLength) {
        this.throwablePatternConverter = throwablePatternConverter;
        this.eol = eol;
        this.maxMessageLength = maxMessageLength;
        this.strSubstitutor = strSubstitutor;
        this.includeAllContextProperties = includeAllContextProperties;
        final List<KeyValuePair> dynamicFields = new LinkedList<>();
        final List<KeyValuePair> staticFields = new LinkedList<>();
        final Set<String> uniqueNames = new HashSet<>(additionalFields.length);
        for (KeyValuePair kv : additionalFields) {
            if (uniqueNames.contains(kv.getKey())) {
                String msg = String.format("Duplicate key (%s) specified for KeyValuePair", kv.getKey());
                throw new IllegalArgumentException(msg);
            } else {
                uniqueNames.add(kv.getKey());
            }
            if (isAdditionalFieldDynamic(kv.getValue())) {
                dynamicFields.add(kv);
            } else {
                staticFields.add(kv);
            }
        }
        this.dynamicAdditionalFields = new KeyValuePair[dynamicFields.size()];
        dynamicFields.toArray(this.dynamicAdditionalFields);
        this.staticAdditionalFields = new KeyValuePair[staticFields.size()];
        staticFields.toArray(this.staticAdditionalFields);
    }

    @Override
    public void write(final JsonWriter writer, final LogEvent event) {
        if (event == null) {
            LOGGER.error("null log event received");
            return;
        }
        if (event.getLoggerName() == null) {
            LOGGER.error("Logger name cannot be null");
            return;
        }
        if (event.getLevel() == null) {
            LOGGER.error("Logger level cannot be null");
            return;
        }
        if (event.getLevel().equals(Level.OFF)) {
            return;
        }
        if (event.getMessage() == null) {
            LOGGER.error("Logger message cannot be null");
            return;
        }
        if (maxMessageLength <= 0 && LOGGER.isWarnEnabled()) {
            LOGGER.warn("MaxMessageLength size is less than or equal to zero [maxMessageLength={}]",
                    maxMessageLength);
            return;
        }

        writer.writeByte(JsonWriter.OBJECT_START);

        writer.writeRaw(VERSION_BYTES, 0, VERSION_BYTES.length);
        writer.writeByte(JsonWriter.COMMA);
        writeLevel(writer, event.getLevel());
        writer.writeByte(JsonWriter.COMMA);
        writeLoggerName(writer, event);
        writer.writeByte(JsonWriter.COMMA);
        writeStringKeyVal(writer, "hostname", HOSTNAME);
        writer.writeByte(JsonWriter.COMMA);
        writer.writeRaw(PID_BYTES, 0, PID_BYTES.length);
        writer.writeByte(JsonWriter.COMMA);
        writeTime(writer, event);
        writer.writeByte(JsonWriter.COMMA);
        writeMessage(writer, event);
        writer.writeByte(JsonWriter.COMMA);
        if (writeMarker(writer, event.getMarker())) {
            writer.writeByte(JsonWriter.COMMA);
        }
        if (writeSource(writer, event.getSource())) {
            writer.writeByte(JsonWriter.COMMA);
        }
        if (writeContextStack(writer, event.getContextStack())) {
            writer.writeByte(JsonWriter.COMMA);
        }
        if (writeAdditionalFields(writer, event)) {
            writer.writeByte(JsonWriter.COMMA);
        }
        writeExtra(writer, event);

        writer.writeByte(JsonWriter.OBJECT_END);
        writer.writeRaw(this.eol, 0, this.eol.length);
    }

    protected void writeLoggerName(final JsonWriter writer, final LogEvent event) {
        writeKey(writer, "name");
        writer.writeString(event.getLoggerName());
    }

    protected void writeLevel(final JsonWriter writer, final Level level) {
        writer.writeString("level");
        writer.writeByte(JsonWriter.SEMI);
        final int bunyanIntLevel = BunyanLevel.toBunyanIntLevel(level);
        NumberConverter.serialize(bunyanIntLevel, writer);

        // Only add the levelStr property when we have a custom logger level
        if (!BunyanLevel.isKnownLevel(level)) {
            writer.writeByte(JsonWriter.COMMA);
            writeStringKeyVal(writer, "level_name", level.name());
        }
    }

    protected void writeMessage(final JsonWriter writer, LogEvent event) {
        final Message message = event.getMessage();
        writeKey(writer, "msg");
        final String formattedMessage = message.getFormattedMessage();
        if (formattedMessage.length() > maxMessageLength) {
            writer.writeString(formattedMessage.subSequence(0, maxMessageLength));
        } else {
            writer.writeString(formattedMessage);
        }

        final Throwable err = message.getThrowable();

        if (err != null) {
            writer.writeByte(JsonWriter.COMMA);
            writeKey(writer, "err");
            writer.writeByte(JsonWriter.OBJECT_START);
            writeStringKeyVal(writer, "message", err.getMessage());
            writer.writeByte(JsonWriter.COMMA);
            writeStringKeyVal(writer, "name", err.getClass().getName());
            writer.writeByte(JsonWriter.COMMA);
            if (event.getThrownProxy() != null) {
                writeKey(writer, "stack");
                writeStackTraceAsString(writer, event);
            }
            writer.writeByte(JsonWriter.OBJECT_END);
        }
    }

    protected void writeStackTraceAsString(final JsonWriter writer, final LogEvent event) {
        final StringBuilder builder = buffer.get();
        try {
            throwablePatternConverter.format(event, builder);
            writer.writeString(builder);
        } finally {
            builder.setLength(0);
        }
    }

    protected static boolean writeSource(final JsonWriter writer, final StackTraceElement element) {
        if (element == null || element.getFileName() == null) {
            return false;
        }
        writeKey(writer, "src");
        writer.writeByte(JsonWriter.OBJECT_START);
        writeKey(writer, "file");
        writer.writeString(element.getFileName());
        writer.writeByte(JsonWriter.COMMA);
        writeKey(writer, "line");
        NumberConverter.serialize(element.getLineNumber(), writer);
        writer.writeByte(JsonWriter.COMMA);
        writeKey(writer, "func");
        String classAndMethod = String.format("%s.%s", element.getClassName(), element.getMethodName());
        writer.writeString(classAndMethod);
        writer.writeByte(JsonWriter.OBJECT_END);

        return true;
    }

    protected static boolean writeMarker(final JsonWriter writer, final Marker marker) {
        if (marker == null || marker.toString().isEmpty()) {
            return false;
        }
        writeKey(writer, "marker");
        writer.writeString(marker.toString());

        return true;
    }

    protected static boolean writeContextStack(final JsonWriter writer, final ThreadContext.ContextStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        writer.writeString("context_stack");
        writer.writeByte(JsonWriter.SEMI);
        writer.writeByte(JsonWriter.ARRAY_START);
        final Iterator<String> itr = stack.iterator();
        while (itr.hasNext()) {
            writer.writeString(itr.next());
            if (itr.hasNext()) {
                writer.writeByte(JsonWriter.COMMA);
            }
        }
        writer.writeByte(JsonWriter.ARRAY_END);

        return true;
    }

    protected static void writeExtra(final JsonWriter writer, final LogEvent event) {
        final String threadDetail;
        if (event.getThreadName() != null) {
            threadDetail = String.format("%s[id=%d,priority=%d]", event.getThreadName(),
                    event.getThreadId(), event.getThreadPriority());
        } else {
            threadDetail = "unknown";
        }

        writeKey(writer, "thread");
        writer.writeString(threadDetail);
    }

    private boolean writeAdditionalFields(final JsonWriter writer, LogEvent event) {
        final boolean hasStaticFields = this.staticAdditionalFields.length > 0;
        final boolean hasDynamicFields = this.dynamicAdditionalFields.length > 0;
        final boolean hasProperties = this.includeAllContextProperties && event.getContextData() != null
                && !event.getContextData().isEmpty();
        if (!hasStaticFields && !hasDynamicFields && !hasProperties) {
            return false;
        }

        boolean noTrailingComma = false;
        for (int i = 0; i < this.staticAdditionalFields.length; i++) {
            final KeyValuePair kv = this.staticAdditionalFields[i];
            writer.writeString(kv.getKey());
            writer.writeByte(JsonWriter.SEMI);
            writer.writeString(kv.getValue());

            final boolean isElementBeforeLast = i < this.staticAdditionalFields.length - 1;
            if (isElementBeforeLast) {
                writer.writeByte(JsonWriter.COMMA);
            } else if (i == this.staticAdditionalFields.length - 1) {
                noTrailingComma = true;
            }
        }

        if (noTrailingComma && this.dynamicAdditionalFields.length > 0) {
            writer.writeByte(JsonWriter.COMMA);
        }

        for (int i = 0; i < this.dynamicAdditionalFields.length; i++) {
            final KeyValuePair kv = this.dynamicAdditionalFields[i];
            writer.writeString(kv.getKey());
            writer.writeByte(JsonWriter.SEMI);
            final String value = strSubstitutor.replace(event, kv.getValue());
            writer.writeString(value);

            final boolean isElementBeforeLast = i < this.dynamicAdditionalFields.length - 1;
            if (isElementBeforeLast) {
                writer.writeByte(JsonWriter.COMMA);
            } else if (i == this.dynamicAdditionalFields.length - 1) {
                noTrailingComma = true;
            }
        }

        final ReadOnlyStringMap contextData = event.getContextData();
        if (noTrailingComma && !contextData.isEmpty()) {
            writer.writeByte(JsonWriter.COMMA);
        }

        if (this.includeAllContextProperties) {
            try {
                contextData.forEach(contextPropertiesTriConsumer, writer);
            } finally {
                buffer.get().setLength(0);
            }
        }

        return true;
    }

    protected boolean isAdditionalFieldDynamic(final String val) {
        return val != null && val.startsWith("$") && val.endsWith("}");
    }

    protected static void writeStringKeyVal(final JsonWriter writer, final String key, final String val) {
        writeKey(writer, key);
        writer.writeString(val);
    }

    protected static void writeKey(final JsonWriter writer, final String key) {
        writer.writeByte(JsonWriter.QUOTE);
        writer.writeAscii(key);
        writer.writeByte(JsonWriter.QUOTE);
        writer.writeByte(JsonWriter.SEMI);
    }

    void writeTime(final JsonWriter writer, final LogEvent event) {
        final StringBuilder builder = buffer.get();

        try {
            writeKey(writer, "time");
            datePatternConverter.format(event, builder);
            writer.writeString(builder);
        } finally {
            builder.setLength(0);
        }
    }

    protected static String lookupHostname() {
        final String hostnameEnv = System.getenv("HOSTNAME");
        if (hostnameEnv != null && !hostnameEnv.isEmpty()) {
            return hostnameEnv;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    protected static long lookupPid() {
        // Most JVMs return this runtime name in the format of: pid@hostname
        final String runtimeMxBean = ManagementFactory.getRuntimeMXBean().getName();
        final int position = runtimeMxBean.indexOf('@');

        if (position >= 0) {
            final String pidAsString = runtimeMxBean.substring(0, position);
            try {
                return Long.parseLong(pidAsString);
            } catch (NumberFormatException ignored) {
                return -2;
            }
        }
        return -1;
    }
}
