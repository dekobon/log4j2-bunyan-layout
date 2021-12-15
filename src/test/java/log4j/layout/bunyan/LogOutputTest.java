package log4j.layout.bunyan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogOutputTest {
    private static final String appName = "output-test";

    public static class FauxLogger {
        public String formatEvent(final LogEvent event, final Layout<?> layout) {
            return new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        }
    }

    private final FauxLogger fauxLogger = new FauxLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    BunyanJsonLayout instance() {
        return instance(false);
    }

    BunyanJsonLayout instance(final boolean includeAllContextProperties) {
        return instance(new KeyValuePair[0], null, includeAllContextProperties);
    }

    BunyanJsonLayout instance(final KeyValuePair[] additionalFields,
                              final BunyanThrowableFormat throwableFormat,
                              final boolean includeAllContextProperties) {
        final String eol = "\n";
        final Configuration config = new NullConfiguration();

        return BunyanJsonLayout.createLayout(additionalFields, throwableFormat,
                appName, eol,
                includeAllContextProperties, BunyanJsonLayout.DEFAULT_MAX_MESSAGE_LENGTH, config);
    }

    @Test
    void eventWithoutLevelIsNotLogged() {
        final BunyanJsonLayout layout = instance();
        final MutableLogEvent event = new MutableLogEvent();
        final Instant now = Instant.parse("2021-11-25T21:18:27.754Z");
        event.setTimeMillis(now.toEpochMilli());
        event.setLoggerName(getClass().getName());
        String messageText = "this is my message";
        FormattedMessage message = new FormattedMessage(messageText, "");
        event.setMessage(message);
        String json = fauxLogger.formatEvent(event, layout);
        assertTrue(json.isEmpty(), json);
    }

    @Test
    void eventWithoutNameIsNotLogged() {
        final BunyanJsonLayout layout = instance();
        final MutableLogEvent event = new MutableLogEvent();
        final Instant now = Instant.parse("2021-11-25T21:18:27.754Z");
        event.setTimeMillis(now.toEpochMilli());
        event.setLevel(Level.FATAL);
        String messageText = "this is my message";
        FormattedMessage message = new FormattedMessage(messageText, "");
        event.setMessage(message);
        String json = fauxLogger.formatEvent(event, layout);
        assertTrue(json.isEmpty(), json);
    }

    @Test
    void eventWithoutMessageIsNotLogged() {
        final BunyanJsonLayout layout = instance();
        final MutableLogEvent event = new MutableLogEvent();
        final Instant now = Instant.parse("2021-11-25T21:18:27.754Z");
        event.setTimeMillis(now.toEpochMilli());
        event.setLevel(Level.FATAL);
        String json = fauxLogger.formatEvent(event, layout);
        assertTrue(json.isEmpty(), json);
    }

    @Test
    void allLevelsMessages() throws IOException {
        Level[] levels = new Level[] {
                Level.OFF,
                Level.TRACE,
                Level.DEBUG,
                Level.INFO,
                Level.WARN,
                Level.ERROR,
                Level.FATAL,
                Level.getLevel("LVL99")
        };

        for (Level level : levels) {
            final BunyanJsonLayout layout = instance();
            final MutableLogEvent event = new MutableLogEvent();
            event.setTimeMillis(System.currentTimeMillis());
            event.setLoggerName(getClass().getName());
            event.setLevel(level);
            Throwable t = new RuntimeException("hi");
            String messageText = "this is my message";
            Message message = new FormattedMessage(messageText, "", t);
            event.setMessage(message);
            event.setThrown(t);
            String json = fauxLogger.formatEvent(event, layout);
            validateEvent(event, json);
        }
    }

    @Test
    void canLogNaughtyStrings() throws IOException {
        final BunyanJsonLayout layout = instance(true);

        try (NaughtyStrings naughtyStrings = new NaughtyStrings()) {
            final StringMap contextData = new SortedArrayStringMap();
            final long timestamp = System.currentTimeMillis();

            for (String string : naughtyStrings) {
                MutableLogEvent event = new MutableLogEvent();
                event.setTimeMillis(timestamp);
                event.setLevel(Level.INFO);
                event.setLoggerName(getClass().getName());
                Throwable t = new RuntimeException(string);
                Message message = new FakeMessage(string, t);
                event.setMessage(message);
                event.setThrown(t);
                event.setMarker(MarkerManager.getMarker(string));
                event.setSource(new StackTraceElement(string, string, string, 7));
                event.setThreadName(string);
                contextData.putValue(string, string);
                event.setContextData(contextData);
                ThreadContext.ContextStack stackData = new DefaultThreadContextStack(true);
                stackData.add(string);
                event.setContextStack(stackData);
                String json = fauxLogger.formatEvent(event, layout);
                contextData.clear();
                validateEvent(event, json);
            }
        }
    }

    @Test
    void wontInterpolateLog4jParamsInMessage() throws IOException {
        final BunyanJsonLayout layout = instance(true);
        final String badString = "Java version: ${java:version}";

        MutableLogEvent event = new MutableLogEvent();
        event.setTimeMillis(System.currentTimeMillis());
        event.setLevel(Level.INFO);
        event.setLoggerName(getClass().getName());
        Message message = new FormattedMessage(badString);
        event.setMessage(message);
        String json = fauxLogger.formatEvent(event, layout);
        final JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
        final String msgText = jsonNode.get("msg").asText();
        assertEquals(badString, msgText);
    }

    /**
     * Reproduces bug where an extra comma is inserted despite
     * properties=false.
     *
     * @see <a href="https://github.com/dekobon/log4j2-bunyan-layout/issues/1">#1</a>
     */
    @Test
    void jsonIsValidIfPropertiesAreDisabled() throws IOException {
        KeyValuePair keyValuePair = new KeyValuePair(
                "traceId", "$${ctx:traceId:-}");
        final BunyanJsonLayout layout = instance(
                new KeyValuePair[] { keyValuePair },
                null, false);
        final MutableLogEvent event = new MutableLogEvent();
        event.setTimeMillis(System.currentTimeMillis());
        event.setLoggerName(getClass().getName());
        event.setLevel(Level.INFO);
        event.setMessage(new SimpleMessage("hello"));
        StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("traceId", "c0160ca6-50ba-11ec-a64b-fbca1ea30083");
        event.setContextData(contextData);
        String json = fauxLogger.formatEvent(event, layout);
        validateEvent(event, json);
    }

    void validateEvent(final LogEvent event, final String json) throws IOException {
        if (event.getLevel().equals(Level.OFF)) {
            assertTrue(json.isEmpty(), "Nothing should be logged when level is OFF");
            return;
        }

        try {
            final JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
            assertEquals(0, jsonNode.get("v").asInt());

            // If the level is known we do a conversion to a bunyan integer level
            final Level level = event.getLevel();
            if (BunyanLevel.isKnownLevel(level)) {
                final int bunyanLevel = BunyanLevel.valueOf(level).bunyanLevel;
                assertEquals(bunyanLevel, jsonNode.get("level").asInt());
            // If unknown we leave the numeric level as is and append the level_name attribute
            } else {
                final int bunyanLevel = level.intLevel();
                assertEquals(bunyanLevel, jsonNode.get("level").asInt());
                assertEquals(level.name(), jsonNode.get("level_name").asText());
            }

            assertEquals(appName, jsonNode.get("name").asText());
            assertEquals(event.getLoggerName(), jsonNode.get("component").asText());
            String timestampStr = formatAsUTCTimestamp(event.getTimeMillis());
            Instant timestamp = Instant.parse(timestampStr);
            assertEquals(timestamp, Instant.parse(jsonNode.get("time").asText()));
            assertEquals(event.getMessage().getFormattedMessage(), jsonNode.get("msg").asText());
            assertTrue(jsonNode.hasNonNull("hostname"));
            assertTrue(jsonNode.hasNonNull("pid"));

            if (event.getMarker() != null && !event.getMarker().toString().isEmpty()) {
                Marker marker = event.getMarker();
                JsonNode jsonMarker = jsonNode.get("marker");
                assertNotNull(jsonMarker, String.format("expecting: [%s]", event.getMarker()));
                assertEquals(marker.toString(), jsonMarker.asText());
            }
            if (event.getThreadName() == null) {
                assertEquals("unknown", jsonNode.get("thread").asText());
            } else {
                String expected = String.format("%s[id=%d,priority=%d]", event.getThreadName(),
                        event.getThreadId(), event.getThreadPriority());
                assertEquals(expected, jsonNode.get("thread").asText());
            }
            if (event.getMessage().getThrowable() != null) {
                Throwable thrown = event.getMessage().getThrowable();
                assertTrue(jsonNode.hasNonNull("err"));
                JsonNode err = jsonNode.get("err");
                assertEquals(thrown.getMessage(), err.get("message").asText());
                assertEquals(thrown.getClass().getName(), err.get("name").asText());
            }
        } catch (AssertionError e) {
            System.err.println(json);
            throw e;
        }
    }

    static String formatAsUTCTimestamp(final long epochMillis) {
        final Instant instant = Instant.ofEpochMilli(epochMillis);
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
    }
}
