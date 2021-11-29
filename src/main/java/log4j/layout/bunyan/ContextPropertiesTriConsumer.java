package log4j.layout.bunyan;

import com.dslplatform.json.JsonWriter;
import org.apache.logging.log4j.util.TriConsumer;

class ContextPropertiesTriConsumer implements TriConsumer<String, String, JsonWriter>  {
    private final ThreadLocal<StringBuilder> buffer;

    ContextPropertiesTriConsumer(final ThreadLocal<StringBuilder> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void accept(final String key, final String val, final JsonWriter writer) {
        final StringBuilder builder = buffer.get();
        if (builder.length() > 0) {
            final char separator = builder.charAt(0);
            writer.writeByte((byte)separator);
        } else {
            builder.append((char)JsonWriter.COMMA);
        }

        writer.writeString(key);
        writer.writeByte(JsonWriter.SEMI);
        writer.writeString(val);
    }
}
