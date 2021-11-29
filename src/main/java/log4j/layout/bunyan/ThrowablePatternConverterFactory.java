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

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;

import java.util.LinkedList;
import java.util.List;

public class ThrowablePatternConverterFactory {
    public static ThrowablePatternConverter instance(final BunyanThrowableFormat throwableFormat,
                                                     final String eol,
                                                     final Configuration configuration) {
        final List<String> optionsList = new LinkedList<>();
        if (throwableFormat != null) {
            optionsList.addAll(throwableFormat.getOptions());
        }
        optionsList.add(String.format("separator(%s)", eol));
        final boolean isExtended = optionsList.remove(BunyanThrowableFormat.EXTENDED_FORMAT);
        if (isExtended) {
            optionsList.add(BunyanThrowableFormat.DEFAULT_FORMAT);
        }

        final String[] options = new String[optionsList.size()];
        optionsList.toArray(options);

        if (isExtended) {
            return ExtendedThrowablePatternConverter.newInstance(configuration, options);
        } else {
            return ThrowablePatternConverter.newInstance(configuration, options);
        }
    }
}
