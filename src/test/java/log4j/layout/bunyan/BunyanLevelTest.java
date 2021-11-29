package log4j.layout.bunyan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BunyanLevelTest {
    @Test
    @DisplayName("Given log4j levels of 100-600, we can convert them to Bunyan levels")
    void canConvertFromKnownLog4jIntLevelsToBunyanLevels() {
        final Map<Integer, Integer> expectedConversions = new TreeMap<>();
        for (BunyanLevel level : BunyanLevel.values()) {
            expectedConversions.put(level.log4jLevel.intLevel(), level.bunyanLevel);
        }

        final List<String> errors = new LinkedList<>();

        for (BunyanLevel level : BunyanLevel.values()) {
            final int converted = BunyanLevel.toBunyanIntLevel(level.log4jLevel.intLevel());
            if (level.bunyanLevel != converted) {
                String msg = String.format("Unable to convert log4j level [%s] to bunyan level [actual=%d,expected=%d]",
                        level.log4jLevel, converted, level.bunyanLevel);
                errors.add(msg);
            }
        }

        if (!errors.isEmpty()) {
            Assertions.fail(String.join("\n", errors));
        }
    }
}
