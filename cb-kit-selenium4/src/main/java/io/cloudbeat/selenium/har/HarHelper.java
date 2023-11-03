package io.cloudbeat.selenium.har;

import io.cloudbeat.common.har.model.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.net.HttpCookie;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.cloudbeat.common.har.HarHelper.decode;

public final class HarHelper {
    static HarCookie getHarCookieFromAssociatedCookie(final JSONObject cookie) throws JSONException {
        // "cookie":{"domain":".google.com","expires":1.733001258405135E9,"httpOnly":false,"name":"CONSENT","path":"\/","priority":"Medium","sameParty":false,"secure":true,"session":false,"size":18,"sourcePort":443,"sourceScheme":"Secure","value":"PENDING+182"}
        String domain = cookie.getString("domain");
        String name = cookie.getString("name");
        String value = cookie.getString("value");
        String path = cookie.getString("path");
        double expires = cookie.getDouble("expires");
        long expiresInMs = (long)(expires * 1000);
        boolean httpOnly = cookie.getBoolean("httpOnly");
        boolean secure = cookie.getBoolean("secure");

        HarCookie harCookie = new HarCookie();
        harCookie.setName(name);
        harCookie.setValue(value);
        harCookie.setSecure(secure);
        harCookie.setPath(path);
        harCookie.setDomain(domain);
        harCookie.setHttpOnly(httpOnly);
        harCookie.setExpires(new Date(expiresInMs));
        return harCookie;
    }
    static List<HarCookie> getHarCookieListFromCookieHeader(final String cookieHeader) {
        if (cookieHeader == null || cookieHeader.length() == 0)
            return Collections.emptyList();
        List<HttpCookie> parsedCookieList = HttpCookie.parse(cookieHeader);
        List<HarCookie> harCookieList = parsedCookieList.stream().map(httpCookie -> {
            HarCookie harCookie = new HarCookie();
            harCookie.setName(httpCookie.getName());
            harCookie.setValue(httpCookie.getValue());
            harCookie.setDomain(httpCookie.getDomain());
            harCookie.setPath(httpCookie.getPath());
            harCookie.setExpires(new Date(httpCookie.getMaxAge()));
            harCookie.setHttpOnly(httpCookie.isHttpOnly());
            harCookie.setSecure(httpCookie.getSecure());
            return harCookie;
        }).collect(Collectors.toList());
        return harCookieList;
    }
    static List<HarHeader> getHarHeaderListFromHeaders(JSONObject headers) {
        List<HarHeader> headerList = new ArrayList<>();
        headers.keys().forEachRemaining(key -> {
            try {
                headerList.add(new HarHeader(key.toString(), headers.getString(key.toString())));
            } catch (JSONException e) {}
        });
        return headerList;
    }

    static boolean isSupportedProtocol(String url) {
        if (url == null) return  false;
        return url.startsWith("https://") || url.startsWith("http://");
    }
    static String getHeaderValue(JSONObject headers, String headerName) throws JSONException {
        if (headers == null)
            return "";
        // build a list of header names in the lower case
        List<String> headerNames = new ArrayList<>();
        headers.keys().forEachRemaining(key -> headerNames.add(key.toString()));
        // http header names are case insensitive
        String lowerCaseHeaderName = headerName.toLowerCase();
        Optional<String> matchingHeaderName = headerNames.stream()
                .filter(h -> h.toLowerCase().equals(lowerCaseHeaderName))
                .findFirst();
        if (matchingHeaderName.isPresent())
            return headers.getString(matchingHeaderName.get());
        return "";
    }

    static HarPostData parsePostData(String contentType, String postData) {
        if (contentType == null || contentType.length() == 0) {
            return null;
        }
        HarPostData harPostData = new HarPostData();
        harPostData.setMimeType(contentType);
        if (contentType.contains("application/x-www-form-urlencoded"))
            parseUrlEncodedPostData(postData, harPostData);
        else
            harPostData.setText(postData);
        return harPostData;
    }

    static void parseUrlEncodedPostData(String postData, HarPostData harPostData) {
        if (postData == null || postData.length() == 0)
            harPostData.setParams(null);
        List<HarPostDataParam> paramList = Pattern.compile("&")
                .splitAsStream(postData)
                .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                .map(o -> new HarPostDataParam(decode(o[0]), decode(o[1])))
                .collect(Collectors.toList());
        harPostData.setParams(paramList);
    }

    static boolean isHttp1x(final String protocol) {
        return protocol.toLowerCase().startsWith("http/1.");
    }

    static long calculateResponseHeaderSize(
            final JSONObject headers,
            final String protocol,
            final int status,
            final String statusText) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("%s %d %s\r\n",
                protocol,
                status,
                statusText));
        headers.keys().forEachRemaining(key -> {
            try {
                String val = headers.getString(key.toString());
                buffer.append(String.format("%s: %s\r\n", key, val));
            } catch (JSONException e) {}
        });
        buffer.append("\r\n");

        return (long)buffer.length();
    }

    static long calculateRequestHeaderSize(
            final HarRequest harRequest) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("%s %s %s\\r\\n",
                harRequest.getMethod(),
                harRequest.getUrl(),
                harRequest.getHttpVersion()));
        harRequest.getHeaders().stream().forEach(harHeader -> {
            buffer.append(String.format("%s: %s\r\n",
                    harHeader.getName(), harHeader.getValue()));
        });
        buffer.append("\r\n");

        return (long)buffer.length();
    }
}
