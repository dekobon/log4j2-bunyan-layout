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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum class that provides conversion to Bunyan log levels from
 * Log4j2 log levels.
 */
public enum BunyanLevel {
    TRACE(Level.TRACE, 10),
    DEBUG(Level.DEBUG, 20),
    INFO(Level.INFO, 30),
    WARN(Level.WARN, 40),
    ERROR(Level.ERROR, 50),
    FATAL(Level.FATAL, 60);

    public static final Map<Level, BunyanLevel> log4jConversion;
    static {
        final Map<Level, BunyanLevel> conversions = new HashMap<>(BunyanLevel.values().length);
        conversions.put(Level.TRACE, TRACE);
        conversions.put(Level.DEBUG, DEBUG);
        conversions.put(Level.INFO, INFO);
        conversions.put(Level.WARN, WARN);
        conversions.put(Level.ERROR, ERROR);
        conversions.put(Level.FATAL, FATAL);
        log4jConversion = Collections.unmodifiableMap(conversions);
    }

    public final Level log4jLevel;
    public final int bunyanLevel;

    BunyanLevel(final Level log4jLevel, final int bunyanLevel) {
        this.log4jLevel = log4jLevel;
        this.bunyanLevel = bunyanLevel;
    }

    /**
     * Determines if a given Log4j2 log level is a known level by Log4j2.
     * @param level Log4j2 log level object
     * @return true if known, otherwise false
     */
    public static boolean isKnownLevel(final Level level) {
        return isKnownLevel(level.intLevel());
    }

    /**
     * Determines if a given Log4j2 log level is a known level by Log4j2.
     * @param log4jIntLevel Log4j2 numeric log level
     * @return true if known, otherwise false
     */
    public static boolean isKnownLevel(final int log4jIntLevel) {
        // Predefined level int values are factors of 100
        // ranging from 100-600.
        final boolean isFactorOfHundred = log4jIntLevel % 100 == 0;
        final boolean isWithinBounds = log4jIntLevel > 0 && log4jIntLevel <= StandardLevel.TRACE.intLevel();

        return isFactorOfHundred && isWithinBounds;
    }

    /**
     * Converts a numeric Log4j2 log level to a numeric Bunyan log level.
     * @param level Log4j2 log level object
     * @return numeric Bunyan log level
     */
    public static int toBunyanIntLevel(final Level level) {
        return toBunyanIntLevel(level.intLevel());
    }

    /**
     * Converts a numeric Log4j2 log level to a numeric Bunyan log level.
     * @param log4jIntLevel numeric Log4j2 log level
     * @return numeric Bunyan log level
     */
    public static int toBunyanIntLevel(final int log4jIntLevel) {
        if (!isKnownLevel(log4jIntLevel)) {
            return log4jIntLevel;
        }

        // Convert StandardLevel int value to Bunyan int value
        final int midValue = 40;
        final int factor = 10;
        final int reducedFactor = log4jIntLevel / factor; // eg 100 -> 10, 500 -> 50
        final int diffFromMidValue = reducedFactor - midValue; // eg 50 - 40
        final int flippedValue = -diffFromMidValue; // eg 10 -> -10
        @SuppressWarnings("UnnecessaryLocalVariable")
        final int addToMidValue = flippedValue + midValue - 10; // -10 + 40 - 10 -> 20

        return addToMidValue;
    }

    public static BunyanLevel valueOf(final Level level) {
        if (!isKnownLevel(level)) {
            String msg = String.format("Unknown log4j level: %s", level);
            throw new IllegalArgumentException(msg);
        }

        return log4jConversion.get(level);
    }
}
