package io.cloudbeat.selenium.har;

import io.cloudbeat.common.har.model.*;
import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public final class PopulateDataHelper {
    static void populateFromRequestWillBeSent(HarEntry harEntry,
                                                    JSONObject request,
                                                    JSONObject params,
                                                    HarPage currentPage) throws JSONException {
        double timestamp = params.getDouble("timestamp");
        double wallTime = params.getDouble("wallTime");
        String requestId = params.getString("requestId");
        String frameId = params.has("frameId")
                ? params.getString("frameId") : null;
        String resourceType = params.has("type")
                ? params.getString("type").toLowerCase() : null;
        // populate HarEntry general property
        harEntry.setAdditionalField("__timestamp", timestamp);
        harEntry.setAdditionalField("__wallTime", wallTime);
        harEntry.setAdditionalField("__frameId", frameId);
        harEntry.setAdditionalField("_requestId", requestId);
        harEntry.setAdditionalField("_resourceType", resourceType);
        harEntry.setTime(0.0);
        // wallTime is not necessarily monotonic, timestamp is. So calculate startedDateTime from timestamp diffs.
        // (see https://cs.chromium.org/chromium/src/third_party/WebKit/Source/platform/network/ResourceLoadTiming.h?q=requestTime+package:%5Echromium$&dr=CSs&l=84)
        long startTimeInMs = (long) (wallTime * 1000);
        harEntry.setStartedDateTime(new Date(startTimeInMs));

        if (currentPage != null)
            harEntry.setPageref(currentPage.getId());

        JSONObject headers = request.getJSONObject("headers");
        String reqUrl = request.getString("url");
        String method = request.getString("method");
        String protocol = request.has("protocol") ?
                request.getString("protocol") : null;
        String reqUrlFragment = request.has("urlFragment") ?
                request.getString("urlFragment") : "";
        String reqPostData = request.has("postData") ?
                request.getString("postData") : null;
        String cookieHeader = HarHelper.getHeaderValue(headers, "Cookie");
        String fullUrl = reqUrl + reqUrlFragment;
        boolean isLinkPreload = request.has("isLinkPreload") ?
                request.getBoolean("isLinkPreload") : false;
        String contentType = HarHelper.getHeaderValue(headers, "Content-Type");
        HarPostData postData = reqPostData != null ?
                HarHelper.parsePostData(contentType, reqPostData) : null;
        HarRequest harRequest = harEntry.getRequest();
        harRequest.setUrl(fullUrl);
        harRequest.setMethod(HttpMethod.valueOf(method));
        harRequest.setHttpVersion(protocol);
        harRequest.setPostData(postData);
        harRequest.setHeaders(HarHelper.getHarHeaderListFromHeaders(headers));
        harRequest.setCookies(HarHelper.getHarCookieListFromCookieHeader(cookieHeader));
        if (isLinkPreload)
            harEntry.getRequest().setAdditionalField("_isLinkPreload", true);
    }

    static void populateFromRedirectResponse(HarLog harLog,
                                                     HarPage harPage,
                                                     JSONObject params) throws JSONException {
        if (!params.has("redirectResponse"))
            return;
        String requestId = params.getString("requestId");
        if (requestId == null) return;
        Optional<HarEntry> previousEntry = harLog.getEntries().stream()
                .filter(entry ->
                        entry.getAdditional().getOrDefault("_requestId", "").equals(requestId))
                .findFirst();
        if (harPage != null) {
            double timestamp = params.getDouble("timestamp");
            harPage.setAdditionalField("__redirectResponse_timestamp", timestamp);
        }
        if (previousEntry.isPresent()) {
            previousEntry.get().getAdditional().remove("_requestId");
            previousEntry.get().setAdditionalField("_requestId", requestId + "r");
            JSONObject redirectResponse = params.getJSONObject("redirectResponse");
            populateFromResponseReceived(
                    previousEntry.get(),
                    redirectResponse,
                    harPage);
        }
    }

    static void populateFromRequestWillBeSentExtraInfo(HarEntry harEntry, JSONObject params) throws JSONException {
        JSONObject headers = params.has("headers") ?
                params.getJSONObject("headers") : null;
        JSONArray associatedCookies = params.has("associatedCookies") ?
                params.getJSONArray("associatedCookies") : null;
        if (headers != null) {
            // override the existing headers with received from extra info
            List<HarHeader> harHeaders = HarHelper.getHarHeaderListFromHeaders(headers);
            if (harHeaders != null && harHeaders.size() > 0) {
                harEntry.getRequest().getHeaders().clear();
                harEntry.getRequest().getHeaders().addAll(harHeaders);
            }
        }
        if (associatedCookies != null && associatedCookies.length() > 0) {
            for (int i=0; i < associatedCookies.length(); i++) {
                JSONObject associatedCookie = associatedCookies.getJSONObject(i);
                JSONArray blockedReasons = associatedCookie.has("blockedReasons") ?
                        associatedCookie.getJSONArray("blockedReasons") : null;
                if (blockedReasons == null || blockedReasons.length() != 0)
                    continue;
                JSONObject cookie = associatedCookie.has("cookie") ?
                        associatedCookie.getJSONObject("cookie") : null;
                if (cookie == null) continue;
                HarCookie harCookie = HarHelper.getHarCookieFromAssociatedCookie(cookie);
                if (harEntry.getRequest().getCookies().equals(Collections.EMPTY_LIST))
                    harEntry.getRequest().setCookies(new ArrayList<>());
                harEntry.getRequest().getCookies().add(harCookie);
            }
        }
    }

    static void populateFromResponseReceived(HarEntry harEntry,
                                                     JSONObject response,
                                                     HarPage parentPage) throws JSONException {
        JSONObject headers = response.getJSONObject("headers");
        JSONObject timing = response.getJSONObject("timing");
        JSONObject requestHeaders = response.has("requestHeaders") ?
                response.getJSONObject("requestHeaders") : null;
        JSONObject requestHeadersText = response.has("requestHeadersText") ?
                response.getJSONObject("requestHeadersText") : null;
        String cookieHeaderValue = HarHelper.getHeaderValue(headers, "Set-Cookie");
        String locationHeaderValue = HarHelper.getHeaderValue(headers, "Location");
        String headersText = response.has("headersText") ?
                response.getString("headersText") : null;
        HarResponse harResponse = harEntry.getResponse();
        harResponse.setHeaders(HarHelper.getHarHeaderListFromHeaders(headers));
        int status = response.getInt("status");
        String statusText = response.getString("statusText");
        String encoding = response.has("encoding") ?
                response.getString("encoding") : null;
        String mimeType = response.getString("mimeType");
        String protocol = response.has("protocol") ?
                response.getString("protocol") : null;
        String connectionId = response.has("connectionId") ?
                response.getString("connectionId") : null;
        String remoteIPAddress = response.getString("remoteIPAddress");
        long encodedDataLength = response.getLong("encodedDataLength");
        boolean fromDiskCache = response.has("fromDiskCache") ?
                response.getBoolean("fromDiskCache") : false;

        // populate HarResponse properties
        harResponse.setHttpVersion(protocol);
        if (locationHeaderValue != null && locationHeaderValue.length() > 0)
            harResponse.setRedirectURL(locationHeaderValue);
        if (statusText == null || statusText.length() == 0)
            statusText = org.apache.commons.httpclient.HttpStatus.getStatusText(status);
        harResponse.setStatusText(statusText);
        harResponse.setStatus(status);
        harResponse.getContent().setMimeType(mimeType);
        harResponse.getContent().setSize(0L);
        harResponse.getContent().setEncoding(encoding);
        if (cookieHeaderValue != null && cookieHeaderValue.length() > 0)
            harResponse.setCookies(HarHelper.getHarCookieListFromCookieHeader(cookieHeaderValue));
        else if (harEntry.getAdditional().containsKey("_extraResponseInfo_headers")) {
            // TODO: We need to merge headers received from extraResponseInfo event
            // with headers received on responseReceived event
            List<HarHeader> extraResponseHeaders = (List<HarHeader>)harEntry.getAdditional()
                    .get("_extraResponseInfo_headers");
            Optional<HarHeader> setCookieHeader = extraResponseHeaders.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("set-cookie"))
                    .findFirst();
            if (setCookieHeader.isPresent()) {
                List<HarCookie> cookiesFromSetCookieHeader =
                        HarHelper.getHarCookieListFromCookieHeader(setCookieHeader.get().getValue());
                harResponse.setCookies(cookiesFromSetCookieHeader);
            }
        }
        if (connectionId != null && connectionId.length() > 0)
            harEntry.setConnection(connectionId);
        harEntry.setServerIPAddress(remoteIPAddress);
        if (harEntry.getRequest().getHttpVersion() == null && protocol != null)
            harEntry.getRequest().setHttpVersion(protocol);
        if (fromDiskCache) {
            if (HarHelper.isHttp1x(protocol)) {
                // In http2 headers are compressed, so calculating size from headers text wouldn't be correct.
                harResponse.setHeadersSize(
                        HarHelper.calculateResponseHeaderSize(headers, protocol, status, statusText));
            }
        }
        else {
            if (requestHeaders != null && harEntry.getRequest().getHeaders().size() == 0) {
                harEntry.getRequest().setHeaders(HarHelper.getHarHeaderListFromHeaders(requestHeaders));
                if (harEntry.getRequest().getCookies().size() == 0) {
                    String cookieHeader = HarHelper.getHeaderValue(requestHeaders, "Cookie");
                    harEntry.getRequest().setCookies(HarHelper.getHarCookieListFromCookieHeader(cookieHeader));
                }
            }
            if (HarHelper.isHttp1x(protocol)) {
                if (headersText != null)
                    harResponse.setHeadersSize((long)headersText.length());
                else {
                    harResponse.setHeadersSize(HarHelper.calculateResponseHeaderSize(
                            headers,
                            protocol,
                            status,
                            statusText
                    ));
                }

                harResponse.setBodySize(
                        encodedDataLength - harResponse.getHeadersSize());

                if (requestHeadersText != null)
                    harEntry.getRequest().setHeadersSize((long)requestHeadersText.length());
                else
                    // Since entry.request.httpVersion is now set, we can calculate header size.
                    harEntry.getRequest().setHeadersSize(HarHelper.calculateRequestHeaderSize(
                            harEntry.getRequest()));
            }
        }
        harResponse.setAdditionalField("_transferSize", encodedDataLength);
        // calculate "timing" metrics
        TimingsHelper.calculateHarTimings(harEntry, timing);
    }
    static void populateFromResponseReceivedExtraInfo(
            HarLog harLog,
            List<HarEntry> entriesWithoutPage,
            JSONObject params) throws JSONException {
        String requestId = params.has("requestId") ?
                params.getString("requestId") : null;
        JSONObject headers = params.has("headers") ?
                params.getJSONObject("headers") : null;
        JSONArray blockedCookies = params.has("blockedCookies") ?
                params.getJSONArray("blockedCookies") : null;
        String altSvcHeader = headers.has("alt-svc")
                ? headers.getString("alt-svc") : null;

        Optional<HarEntry> harEntryByRequestId = harLog.getEntries()
                .stream()
                .filter(e ->
                        e.getAdditional().getOrDefault("_requestId", "").equals(requestId))
                .findFirst();

        if (!harEntryByRequestId.isPresent())
            harEntryByRequestId = entriesWithoutPage
                    .stream()
                    .filter(e ->
                            e.getAdditional().getOrDefault("_requestId", "").equals(requestId))
                    .findFirst();
        if (!harEntryByRequestId.isPresent())
            return;
        if (altSvcHeader != null && altSvcHeader.startsWith("h3")
                && harEntryByRequestId.get().getRequest().getHttpVersion() == null)
            harEntryByRequestId.get().getRequest().setHttpVersion("h3");
        List<HarHeader> harHeaders = headers != null ? HarHelper.getHarHeaderListFromHeaders(headers) : null;
        if (harEntryByRequestId.get().getResponse().getStatus() != null &&  harHeaders != null) {
            harEntryByRequestId.get().getResponse().getHeaders().clear();
            harEntryByRequestId.get().getResponse().getHeaders().addAll(harHeaders);
            // Extra info received before response
            /*harEntryByRequestId.get().setAdditionalField("_extraResponseInfo",
                    headers: parseHeaders(params.headers),
                    blockedCookies: params.blockedCookies*/
            return;
        }
        else if (harEntryByRequestId.get().getResponse().getStatus() == null) {
            harEntryByRequestId.get().setAdditionalField(
                    "_extraResponseInfo_headers",
                    harHeaders);
        }
    }
    static void populatePageFromFirstRequest(HarPage harPage, JSONObject params) throws JSONException {
        if (!harPage.getAdditional().containsKey("__timestamp")) {
            harPage.setAdditionalField("__wallTime", params.getDouble("wallTime"));
            harPage.setAdditionalField("__timestamp", params.getDouble("timestamp"));
            long startTimeInMs = (long) (params.getDouble("wallTime") * 1000);
            harPage.setStartedDateTime(new Date(startTimeInMs));
            // URL is better than blank, and it's what devtools uses.
            JSONObject request = params.getJSONObject("request");
            String pageTitle = harPage.getTitle() == null || harPage.getTitle().equals("")
                    ? request.getString("url") : harPage.getTitle();
            harPage.setTitle(pageTitle);
        }
    }

    static void populateFromDataReceived(HarEntry harEntry, JSONObject params) throws JSONException {
        long dataLength = params.has("dataLength") ?
                params.getLong("dataLength") : 0;
        long contentSize = harEntry.getResponse().getContent().getSize() != null ?
                harEntry.getResponse().getContent().getSize() : 0;
        if (harEntry.getResponse().getStatus() != null)
            harEntry.getResponse().getContent().setSize(
                    contentSize + dataLength
            );
        // calculate "receive" time
        double timestamp = params.getDouble("timestamp");
        double _requestTime = (double)harEntry.getAdditional().get("_requestTime");
        double __receiveHeadersEnd = (double)harEntry.getAdditional().get("__receiveHeadersEnd");
        HarTiming harTiming = harEntry.getTimings();
        harTiming.setReceive(
                (timestamp - _requestTime) * 1000 - __receiveHeadersEnd);
        // recalculate "time"
        harEntry.setTime(
                Math.max(0, harTiming.getBlocked()) +
                        Math.max(0, harTiming.getDns()) +
                        Math.max(0, harTiming.getConnect()) +
                        Math.max(0, harTiming.getSend()) +
                        Math.max(0, harTiming.getWait()) +
                        Math.max(0, harTiming.getReceive())
        );
    }

    static void populateOnLoadingFinished(HarEntry harEntry, JSONObject params) throws JSONException {
        if (harEntry.getAdditional().containsKey("_requestTime") &&
                harEntry.getAdditional().containsKey("__receiveHeadersEnd")) {
            double timestamp = params.getDouble("timestamp");
            double _requestTime = (double)harEntry.getAdditional().get("_requestTime");
            double __receiveHeadersEnd = (double)harEntry.getAdditional().get("__receiveHeadersEnd");
            HarTiming harTiming = harEntry.getTimings();
            harTiming.setReceive(
                    (timestamp - _requestTime) * 1000 - __receiveHeadersEnd);
            harEntry.setTime(
                    Math.max(0, harTiming.getBlocked()) +
                    Math.max(0, harTiming.getDns()) +
                    Math.max(0, harTiming.getConnect()) +
                    Math.max(0, harTiming.getSend()) +
                    Math.max(0, harTiming.getWait()) +
                    Math.max(0, harTiming.getReceive())
            );
        }
        // For cached entries, Network.loadingFinished can have an earlier
        // timestamp than Network.dataReceived

        // encodedDataLength will be -1 sometimes
        long encodedDataLength = params.getLong("encodedDataLength");
        if (encodedDataLength >= 0) {
            if (harEntry.getResponse().getStatus() != null) {
                HarResponse harResponse = harEntry.getResponse();
                harResponse.setAdditionalField("_transferSize", encodedDataLength);
                if (HarHelper.isHttp1x(harResponse.getHttpVersion()) &&
                    harResponse.getHeadersSize() > -1)
                    // Reduce headers size from encodedDataLength
                    harResponse.setBodySize(encodedDataLength - harResponse.getHeadersSize());
                else
                    harResponse.setBodySize(encodedDataLength);
                long contentSize = harEntry.getResponse().getContent().getSize() != null ?
                        harEntry.getResponse().getContent().getSize() : 0;
                long compression = Math.max(
                        0,
                        contentSize - harResponse.getBodySize()
                );
                if (compression > 0)
                    harResponse.getContent().setCompression(compression);
            }
        }
    }

    public static void populateFromLoadEventFired(HarPage page, JSONObject params) throws JSONException {
        if (!params.has("timestamp") || !page.getAdditional().containsKey("__timestamp"))
            return;
        double timestamp = params.getDouble("timestamp");
        double pageTimestamp = page.getAdditional().containsKey("__redirectResponse_timestamp") ?
                (double)page.getAdditional().get("__redirectResponse_timestamp") :
                (double)page.getAdditional().get("__timestamp");
        page.getPageTimings().setOnLoad((timestamp - pageTimestamp) * 1000);
    }

    public static void populateFromDomContentEventFired(HarPage page, JSONObject params) throws JSONException {
        if (!params.has("timestamp") || !page.getAdditional().containsKey("__timestamp"))
            return;
        double timestamp = params.getDouble("timestamp");
        double pageTimestamp = (double)page.getAdditional().get("__timestamp");
        page.getPageTimings().setOnContentLoad((timestamp - pageTimestamp) * 1000);
    }

    public static void populateFromLoadingFailed(
            HarEntry harEntry, HarLog harLog, JSONObject params) throws JSONException {
        String errorText = params.has("errorText") ?
                params.getString("errorText") : null;
        if (errorText != null && errorText.equalsIgnoreCase("net::ERR_ABORTED")) {
            populateOnLoadingFinished(harEntry, params);
            return;
        }
        String requestId = params.getString("requestId");
        List<HarEntry> entriesWithoutCurrentRequest = harLog.getEntries().stream()
                .filter(e -> !e.getAdditional().get("_requestId").toString().equalsIgnoreCase(requestId))
                .collect(Collectors.toList());
        harLog.setEntries(entriesWithoutCurrentRequest);
    }
}
