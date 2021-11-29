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

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.util.Strings;

import java.util.*;

@Plugin(name = "ThrowableFormat", category = Node.CATEGORY, printObject = true)
public class BunyanThrowableFormat {
    public static final String DEFAULT_FORMAT = "full";
    public static final String EXTENDED_FORMAT = "extended";

    private final String format;
    private final Integer depth;
    private final String ignorePackages;

    @PluginBuilderFactory
    public static BunyanThrowableFormat.Builder newBuilder() {
        return new BunyanThrowableFormat.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BunyanThrowableFormat> {
        private static final LinkedHashSet<String> FORMATS;
        static {
            FORMATS = new LinkedHashSet<>();
            FORMATS.add("none");
            FORMATS.add("full");
            FORMATS.add("extended");
            FORMATS.add("short");
            FORMATS.add(ThrowableFormatOptions.CLASS_NAME);
            FORMATS.add(ThrowableFormatOptions.METHOD_NAME);
            FORMATS.add(ThrowableFormatOptions.LINE_NUMBER);
            FORMATS.add(ThrowableFormatOptions.FILE_NAME);
            FORMATS.add(ThrowableFormatOptions.MESSAGE);
            FORMATS.add(ThrowableFormatOptions.LOCALIZED_MESSAGE);
        }

        @PluginBuilderAttribute
        private String format;
        @PluginBuilderAttribute
        private Integer depth;
        @PluginBuilderAttribute
        private String ignorePackages;

        @Override
        public BunyanThrowableFormat build() {
            validate();

            if (format == null && depth == null) {
                setFormat(DEFAULT_FORMAT);
            }

            return new BunyanThrowableFormat(format, depth, ignorePackages);
        }

        public void setFormat(final String format) {
            this.format = format;
        }

        public void setDepth(final Integer depth) {
            this.depth = depth;
        }

        public void setIgnorePackages(final String ignorePackages) {
            this.ignorePackages = ignorePackages;
        }

        protected void validate() {
            if (Strings.isNotBlank(format)) {
                if (depth != null) {
                    throw new IllegalArgumentException("Format and depth were both set - set either one, but not both");
                }
                if (!FORMATS.contains(format)) {
                    throw new IllegalArgumentException("Unknown format specified: " + format);
                }
            }
            if (depth != null) {
                if (depth < 0) {
                    throw new IllegalArgumentException("Depth must be greater than zero");
                }
            }
        }
    }

    protected BunyanThrowableFormat(final String format, final Integer depth, final String ignorePackages) {
        this.format = format;
        this.depth = depth;
        this.ignorePackages = ignorePackages;
    }

    public String getFormat() {
        return format;
    }

    public Integer getDepth() {
        return depth;
    }

    public String getIgnorePackages() {
        return ignorePackages;
    }

    public List<String> getOptions() {
        final List<String> options = new LinkedList<>();

        if (depth != null) {
            options.add(Integer.toString(depth));
        } else if (Strings.isNotBlank(format)) {
            options.add(format);
        } else {
            options.add(DEFAULT_FORMAT);
        }
        if (Strings.isNotBlank(ignorePackages)) {
            options.add(String.format("filters(%s)", ignorePackages));
        }

        return Collections.unmodifiableList(options);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BunyanThrowableFormat that = (BunyanThrowableFormat) o;
        return Objects.equals(format, that.format) && Objects.equals(depth, that.depth) && Objects.equals(ignorePackages, that.ignorePackages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, depth, ignorePackages);
    }
}
