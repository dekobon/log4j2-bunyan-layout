/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package log4j.layout.bunyan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Utilities class that provides Java unescape methods.
 * This class will attempt to find an unescape method by:
 * 1. Looking for a method available within the JRE (String.translateEscapes)
 * 2. Looking for the Apache Commons Text class org.apache.commons.text.StringEscapeUtils
 * 3. If none of the above are available, it will use a naive function to escape the string
 */
public class StringEscapeUtils {
    protected static final Class<?> JAVA_UNESCAPE_CLASS = findStringUnescapeClass();

    @SuppressWarnings("JavaReflectionMemberAccess")
    protected static Class<?> findStringUnescapeClass() {
        try {
            String.class.getMethod("translateEscapes"); // try to find Java 15+ String method
            return String.class;
        } catch (NoSuchMethodException e) {
            // Do nothing and continue
        }
        try {
            return Class.forName("org.apache.commons.text.StringEscapeUtils");
        } catch (ClassNotFoundException e) {
            return StringEscapeUtils.class;
        }
    }

    protected static String unescape(final String string) {
        if (JAVA_UNESCAPE_CLASS.equals(String.class)) {
            try {
                final Method method = JAVA_UNESCAPE_CLASS.getMethod("translateEscapes");
                return Objects.toString(method.invoke(string));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot find valid translateEscapes method", e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Unable to invoke translateEscapes method", e);
            }
        }

        try {
            final Method method = JAVA_UNESCAPE_CLASS.getMethod("unescapeJava", String.class);
            return Objects.toString(method.invoke(null, string));
        }  catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot find valid unescapeJava method", e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke unescapeJava method", e);
        }
    }

    /**
     * Naive implementation of Java string unescape that supports only a subset of unescapes.
     */
    @SuppressWarnings("unused")
    public static String unescapeJava(final String string) {
        return string
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\f", "\f")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\0", "\0");
    }
}
