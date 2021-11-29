package log4j.layout.bunyan;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BunyanJsonLayoutTest {
    @Test
    void canCreateInstanceWithDefaultEol() {
        final String eol = null;
        final Configuration config = new NullConfiguration();
        final KeyValuePair[] additionalFields = new KeyValuePair[0];
        final BunyanThrowableFormat throwableFormat = null;
        final boolean includeAllContextProperties = false;

        final BunyanJsonLayout instance = BunyanJsonLayout.createLayout(additionalFields, throwableFormat, eol,
                includeAllContextProperties, BunyanJsonLayout.DEFAULT_MAX_MESSAGE_LENGTH, config);
        assertNotNull(instance);
        final MutableLogEvent event = new MutableLogEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName(getClass().getName());
        event.setMessage(new SimpleMessage("default eol"));
        final byte[] log = instance.toByteArray(event);
        assertEquals((byte)'\n', log[log.length-1]);
    }

    @Test
    void canCreateInstanceWithUnixEol() {
        final String eol = "\\n";
        final Configuration config = new NullConfiguration();
        final KeyValuePair[] additionalFields = new KeyValuePair[0];
        final BunyanThrowableFormat throwableFormat = null;
        final boolean includeAllContextProperties = false;

        final BunyanJsonLayout instance = BunyanJsonLayout.createLayout(additionalFields, throwableFormat, eol,
                includeAllContextProperties, BunyanJsonLayout.DEFAULT_MAX_MESSAGE_LENGTH, config);
        assertNotNull(instance);
        final MutableLogEvent event = new MutableLogEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName(getClass().getName());
        event.setMessage(new SimpleMessage("default eol"));
        final byte[] log = instance.toByteArray(event);
        assertEquals((byte)'\n', log[log.length-1]);
    }

    @Test
    void canCreateInstanceWithWindowsEol() {
        final String eol = "\r\n";
        final Configuration config = new NullConfiguration();
        final KeyValuePair[] additionalFields = new KeyValuePair[0];
        final BunyanThrowableFormat throwableFormat = null;
        final boolean includeAllContextProperties = false;

        final BunyanJsonLayout instance = BunyanJsonLayout.createLayout(additionalFields, throwableFormat, eol,
                includeAllContextProperties, BunyanJsonLayout.DEFAULT_MAX_MESSAGE_LENGTH, config);
        assertNotNull(instance);
        final MutableLogEvent event = new MutableLogEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName(getClass().getName());
        event.setMessage(new SimpleMessage("default eol"));
        final byte[] log = instance.toByteArray(event);
        assertEquals((byte)'\r', log[log.length-2]);
        assertEquals((byte)'\n', log[log.length-1]);
    }
}
