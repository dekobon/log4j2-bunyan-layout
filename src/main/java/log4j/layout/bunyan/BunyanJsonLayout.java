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

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the Bunyan format JSON logging layout as a
 * Log4j {@link Layout}.
 *
 * @see <a href="https://github.com/trentm/node-bunyan">https://github.com/trentm/node-bunyan</a>
 */
@Plugin(name = "BunyanJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class BunyanJsonLayout implements Layout<String> {
    /**
     * Maximum size of log message (32,768 characters).
     */
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 32_768;
    /**
     * Reference to Log4j internal debugging logger.
     */
    private static final Logger LOGGER = StatusLogger.getLogger();
    /**
     * Content type of log output.
     */
    private static final String CONTENT_TYPE = "application/json; charset=utf8";
    /**
     * Thread local that supplies instances of {@link ByteBufferDestinationOutputStream}
     * so that we can have a unique instance of the class for each thread.
     */
    private static final ThreadLocal<ByteBufferDestinationOutputStream> outputStreamThreadLocal =
            ThreadLocal.withInitial(ByteBufferDestinationOutputStream.SUPPLIER);
    /**
     * JSON serialization library.
     */
    protected final DslJson<LogEvent> dslJson = new DslJson<>();

    @PluginFactory
    public static BunyanJsonLayout createLayout(
            @PluginElement("AdditionalField") final KeyValuePair[] additionalFields,
            @PluginElement("ThrowableFormat") final BunyanThrowableFormat throwableFormat,
            @PluginAttribute("endOfLine") final String lineSeparator,
            @PluginAttribute("properties") final boolean includeAllContextProperties,
            @PluginAttribute(value = "maxMessageLength", defaultInt = DEFAULT_MAX_MESSAGE_LENGTH) final int maxMessageLength,
            @PluginConfiguration Configuration configuration) {
        final String eol;
        if (lineSeparator == null) {
            eol = "\n";
        } else {
            eol = StringEscapeUtils.unescape(lineSeparator);
        }

        final ThrowablePatternConverter throwablePatternConverter =
                ThrowablePatternConverterFactory.instance(throwableFormat, eol, configuration);

        final StrSubstitutor strSubstitutor = configuration.getStrSubstitutor();
        final LogEventJsonWriter jsonWriter = new LogEventJsonWriter(throwablePatternConverter,
                additionalFields, strSubstitutor, includeAllContextProperties,
                eol.getBytes(StandardCharsets.UTF_8), maxMessageLength);

        return new BunyanJsonLayout(jsonWriter);
    }

    protected BunyanJsonLayout(final LogEventJsonWriter jsonWriter) {
        dslJson.registerWriter(LogEvent.class, jsonWriter);
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public byte[] getFooter() {
        return new byte[0];
    }

    @Override
    public byte[] getHeader() {
        return new byte[0];
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(1);
        result.put("version", "1.0");
        result.put("bunyan_version", "0");
        return result;
    }

    @Override
    public String toSerializable(LogEvent event) {
        try {
            final JsonWriter writer = dslJson.newWriter();
            dslJson.serialize(writer, event);
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Unable to serialize log event to bunyan format", e);
            return "";
        }
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        try {
            final JsonWriter writer = dslJson.newWriter();
            dslJson.serialize(writer, event);
            return writer.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Unable to serialize log event to bunyan format", e);
            return new byte[0];
        }
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        /* We reuse an OutputStream here because that is the only memory efficient data
         * structure that dsljson understands, and it is unfortunate. We reuse it by
         * just setting a new ByteBufferDestination over the previous value for each
         * invocation of this encode method. In order for this operation to be
         * thread-safe, we utilize a ThreadLocal variable so that we can be sure to
         * only have a single log4j.layout.bunyan.ByteBufferDestinationOutputStream per thread. */
        ByteBufferDestinationOutputStream stream = outputStreamThreadLocal.get();
        stream.setDestination(destination);

        try {
            dslJson.serialize(event, stream);
        } catch (IOException e) {
            LOGGER.error("Unable to serialize to bunyan format", e);
        }
    }
}
