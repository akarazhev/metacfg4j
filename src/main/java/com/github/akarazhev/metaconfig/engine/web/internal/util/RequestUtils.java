package com.github.akarazhev.metaconfig.engine.web.internal.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class RequestUtils {

    private RequestUtils() {
        // Util class
    }

//    public static Map<String, List<String>> parse(String query) {
//        if (query == null || "".equals(query)) {
//            return Collections.emptyMap();
//        }
//
//        return Pattern.compile("&").splitAsStream(query)
//                .map(s -> Arrays.copyOf(s.split("="), 2))
//                .collect(groupingBy(s -> decode(s[0]), mapping(s -> decode(s[1]), toList())));
//    }

    private static String decode(final String encoded) {
        try {
            return encoded == null ? null : URLDecoder.decode(encoded, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 is a required encoding", e);
        }
    }
}
