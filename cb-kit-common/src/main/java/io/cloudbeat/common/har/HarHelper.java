package io.cloudbeat.common.har;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.har.model.*;
import io.cloudbeat.common.model.HttpNetworkEntry;
import okio.Path;
import org.apache.cxf.helpers.FileUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class HarHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static HarLog getHarLog(final List<HarEntry> harEntries) {
        HarLog harLog = new HarLog();
        harLog.setEntries(harEntries);
        return harLog;
    }

    public static List<HarHeader> getHarHeaderList(final Map<String, Object> headersMap) {
        if (headersMap == null || headersMap.isEmpty())
            return Collections.emptyList();
        return headersMap.keySet().stream()
                .map(headerName -> new HarHeader(headerName, objectToString(headersMap.get(headerName))))
                .collect(Collectors.toList());
    }
    public static List<HarQueryParam> getHarQueryParamList(final String url) {
        URL urlObj;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            return Collections.emptyList();
        }
        if (urlObj.getQuery() == null || urlObj.getQuery().length() == 0)
            return Collections.emptyList();
        List<HarQueryParam> list = Pattern.compile("&")
                .splitAsStream(urlObj.getQuery())
                .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                .map(o -> new HarQueryParam(decode(o[0]), decode(o[1])))
                .collect(Collectors.toList());
        return list;
    }

    private static String objectToString(Object obj) {
        if (obj == null) return null;
        return obj.toString();
    }

    public static String decode(final String encoded) {
        if (encoded == null) return null;
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static void writeHarFile(final HarLog harLog, final File file) throws IOException {
        String harJsonStr = OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER);
        String harJsonWithJsWrapStr = "onInputData(" + harJsonStr + ")";
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(harJsonWithJsWrapStr.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }
}
