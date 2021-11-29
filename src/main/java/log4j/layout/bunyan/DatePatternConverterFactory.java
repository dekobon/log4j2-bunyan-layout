package log4j.layout.bunyan;

import org.apache.logging.log4j.core.pattern.DatePatternConverter;

import java.time.ZoneOffset;
import java.util.TimeZone;

public class DatePatternConverterFactory {
    private static final String ISO_8601_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String UTC_TIMEZONE_ID = TimeZone.getTimeZone(ZoneOffset.UTC).getID();

    public static DatePatternConverter instance() {
        final String[] datePatternConverterOptions = new String[] { ISO_8601_UTC, UTC_TIMEZONE_ID};
        return DatePatternConverter.newInstance(datePatternConverterOptions);
    }
}
