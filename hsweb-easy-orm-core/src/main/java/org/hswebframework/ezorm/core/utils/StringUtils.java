package org.hswebframework.ezorm.core.utils;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.Collection;
import java.util.Map;

public class StringUtils {

    static FastThreadLocal<StringBuilder> SHARE = new FastThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() throws Exception {
            return new StringBuilder();
        }
    };

    public static boolean isNullOrEmpty(Object arg) {
        if (arg == null) {
            return true;
        }
        if (arg instanceof String) {
            return (String.valueOf(arg)).length() == 0;
        }
        if (arg instanceof Collection) {
            return ((Collection<?>) arg).isEmpty();
        }
        if (arg instanceof Map) {
            return ((Map<?, ?>) arg).isEmpty();
        }
        return false;
    }

    public static String join(CharSequence delimiter, Iterable<?> args) {
        StringBuilder builder = SHARE.get();
        try {
            int idx = 0;
            for (Object arg : args) {
                if (idx++ > 0) {
                    builder.append(delimiter);
                }
                builder.append(arg);
            }
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }

    public static String concat(Object... args) {
        StringBuilder builder = SHARE.get();
        try {
            for (Object arg : args) {
                builder.append(arg);
            }
            return builder.toString();
        } finally {
            builder.setLength(0);
        }
    }

    public static String toLowerCaseFirstOne(String str) {
        if (Character.isLowerCase(str.charAt(0)))
            return str;
        else {
            char[] chars = str.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
    }
}
