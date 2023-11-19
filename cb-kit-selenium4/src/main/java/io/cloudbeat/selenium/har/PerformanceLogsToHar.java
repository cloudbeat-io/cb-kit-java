package io.cloudbeat.selenium.har;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudbeat.common.har.model.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

import java.util.*;
import java.util.stream.Collectors;

public final class PerformanceLogsToHar {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static HarLog parse(LogEntries perfLogs) {
        Map<String, String> rootFrameMappings = new HashMap<>();
        List<HarEntry> entriesWithoutPage = new ArrayList<>();
        List<JSONObject> paramsWithoutPage = new ArrayList<>();
        List<JSONObject> responsesWithoutPage = new ArrayList<>();
        List<String> ignoredRequests = new ArrayList<>();
        HarLog harLog = new HarLog();
        List<HarPage> harPages = harLog.getPages();

        // reverse the log entries list before processing
        List<LogEntry> logList = new ArrayList<>();
        Iterator<LogEntry> iterator = perfLogs.iterator();
        while (iterator.hasNext())
            logList.add(iterator.next());

        //for (Iterator<LogEntry> it = perfLogs.iterator(); it.hasNext();) {
        for (LogEntry entry : logList) {
            //LogEntry entry = it.next();
            try {
                JSONObject json = new JSONObject(entry.getMessage());
                JSONObject message = json.getJSONObject("message");
                String method = message.getString("method");
                if (method == null || (!method.startsWith("Page.") && !method.startsWith("Network.")))
                    continue;
                JSONObject params = message.getJSONObject("params");
                boolean containsCookie = params.toString().toLowerCase().contains("cookie");
                if (containsCookie)
                    System.out.println("We got cookie");
                String requestId = params.has("requestId") ?
                        params.getString("requestId") : null;
                String frameId = params.has("frameId") ?
                        params.getString("frameId") : null;
                Optional<HarEntry> harEntryByRequestId;

                switch (method) {
                    case "Page.frameStartedLoading":
                    case "Page.frameRequestedNavigation":
                    case "Page.navigatedWithinDocument":
                    case "Page.navigationRequested":
                    case "Page.frameScheduledNavigation":
                        String rootFrameId = rootFrameMappings.getOrDefault(frameId, frameId);
                        if (harPages.stream().anyMatch(page -> page.getAdditional().getOrDefault("__frameId", "").equals(rootFrameId)))
                            continue;
                        String currentPageId = UUID.randomUUID().toString();
                        String title =
                                method.equals("Page.navigatedWithinDocument") ? params.getString("url") : "";
                        HarPage harPage = new HarPage();
                        harPage.setId(currentPageId);
                        harPage.setTitle(title);
                        harPage.setAdditionalField("__frameId", rootFrameId);
                        harLog.getPages().add(harPage);
                        // do we have any unmapped requests, add them
                        if (entriesWithoutPage.size() > 0) {
                            // update pageref
                            entriesWithoutPage.forEach(harEntry -> harEntry.setPageref(currentPageId));
                            harLog.getEntries().addAll(entriesWithoutPage);

                            if (paramsWithoutPage.size() > 0)
                                PopulateDataHelper.populatePageFromFirstRequest(
                                        harPage, paramsWithoutPage.get(0));

                            // Add unmapped redirects
                            paramsWithoutPage.forEach(param -> {
                                try {
                                    if (param.has("redirectResponse"))
                                        PopulateDataHelper.populateFromRedirectResponse(harLog, harPage, params);
                                } catch (JSONException e) {}
                            });
                        }
                        break;
                    case "Network.requestWillBeSent":
                        JSONObject request = params.getJSONObject("request");
                        String reqUrl = request.getString("url");
                        if (!HarHelper.isSupportedProtocol(reqUrl)) {
                            ignoredRequests.add(requestId);
                            continue;
                        }
                        HarPage lastPage = harPages.size() > 0 ? harPages.get(harPages.size() - 1) : null;
                        HarEntry harEntry = new HarEntry();
                        PopulateDataHelper.populateFromRequestWillBeSent(harEntry, request, params, lastPage);
                        PopulateDataHelper.populateFromRedirectResponse(harLog, lastPage, params);
                        if (lastPage == null) {
                            entriesWithoutPage.add(harEntry);
                            paramsWithoutPage.add(params);
                            continue;
                        }
                        harLog.getEntries().add(harEntry);
                        // this is the first request for this page, so set timestamp of page.
                        PopulateDataHelper.populatePageFromFirstRequest(lastPage, params);
                        /*long startedTime =
                            lastPage.getAdditional().get("__wallTime") + (timestamp - page.__timestamp);
                        entry.startedDateTime = dayjs.unix(entrySecs).toISOString();
                         */
                        break;
                    case "Network.responseReceived":
                        if (harLog.getPages().size() < 1) {
                            // we haven't loaded any pages yet.
                            responsesWithoutPage.add(params);
                            continue;
                        }
                        if (ignoredRequests.contains(requestId))
                            continue;
                        // retrieve requestId related har entry
                        harEntryByRequestId =
                                getHarEntryByRequestId(requestId, harLog, entriesWithoutPage);
                        if (!harEntryByRequestId.isPresent())
                            continue;
                        // retrieve current frame related parent page
                        Optional<HarPage> parentPage =
                                getParentPageByFrameId(frameId, harLog, rootFrameMappings);
                        if (!parentPage.isPresent())
                            continue;
                        JSONObject response = params.getJSONObject("response");
                        PopulateDataHelper.populateFromResponseReceived(
                                harEntryByRequestId.get(), response, parentPage.get());
                        break;
                    case "Network.requestWillBeSentExtraInfo":
                        if (ignoredRequests.contains(requestId)) {
                            continue;
                        }
                        harEntryByRequestId =
                                getHarEntryByRequestId(requestId, harLog, null);
                        if (!harEntryByRequestId.isPresent())
                            continue;
                        PopulateDataHelper.populateFromRequestWillBeSentExtraInfo(
                                harEntryByRequestId.get(), params);
                        break;
                    case "Network.responseReceivedExtraInfo":
                        if (harLog.getPages().size() == 0)
                            // we haven't loaded any pages yet.
                            continue;
                        if (ignoredRequests.contains(requestId))
                            continue;
                        PopulateDataHelper.populateFromResponseReceivedExtraInfo(
                                harLog, entriesWithoutPage, params);
                        break;
                    case "Network.dataReceived":
                        if (harLog.getPages().size() == 0)
                            // we haven't loaded any pages yet.
                            continue;
                        if (ignoredRequests.contains(requestId))
                            continue;
                        harEntryByRequestId =
                                getHarEntryByRequestId(requestId, harLog, entriesWithoutPage);
                        if (!harEntryByRequestId.isPresent())
                            continue;
                        PopulateDataHelper.populateFromDataReceived(
                                harEntryByRequestId.get(), params);
                        break;
                    case "Network.loadingFinished":
                        if (harLog.getPages().size() == 0)
                            // we haven't loaded any pages yet.
                            continue;
                        if (ignoredRequests.contains(requestId)) {
                            ignoredRequests.remove(requestId);
                            continue;
                        }
                        harEntryByRequestId =
                                getHarEntryByRequestId(requestId, harLog, entriesWithoutPage);
                        if (!harEntryByRequestId.isPresent())
                            continue;
                        PopulateDataHelper.populateOnLoadingFinished(
                                harEntryByRequestId.get(), params);
                        break;
                    case "Page.loadEventFired":
                        if (harLog.getPages().size() == 0)
                            // we haven't loaded any pages yet.
                            continue;
                        lastPage = harPages.size() > 0 ? harPages.get(harPages.size() - 1) : null;
                        PopulateDataHelper.populateFromLoadEventFired(lastPage, params);
                        break;
                    case "Page.domContentEventFired":
                        if (harLog.getPages().size() == 0)
                            // we haven't loaded any pages yet.
                            continue;
                        lastPage = harPages.size() > 0 ? harPages.get(harPages.size() - 1) : null;
                        PopulateDataHelper.populateFromDomContentEventFired(lastPage, params);
                        break;
                    case "Network.loadingFailed":
                        if (ignoredRequests.contains(requestId)) {
                            ignoredRequests.remove(requestId);
                            continue;
                        }
                        harEntryByRequestId =
                                getHarEntryByRequestId(requestId, harLog, null);
                        PopulateDataHelper.populateFromLoadingFailed(
                                harEntryByRequestId.get(), harLog, params);
                        break;
                }
            }
            catch (JSONException e) {
                System.out.println(e.getMessage());
            }
        }
        // filter out all entries that are missing the required data
        harLog.setEntries(
                harLog.getEntries().stream()
                .filter(e -> e.getRequest().getHttpVersion() != null)
                .collect(Collectors.toList())
        );
        return harLog;
    }
    private static Optional<HarPage> getParentPageByFrameId(
            String frameId,
            HarLog harLog,
            Map<String, String> rootFrameMappings
    ) {
        String parentFrameId =
                rootFrameMappings.getOrDefault(frameId, frameId);
        Optional<HarPage> parentPage =
                harLog.getPages()
                        .stream()
                        .filter(p ->
                                p.getAdditional().getOrDefault("__frameId", "").equals(parentFrameId))
                        .findFirst();
        if (!parentPage.isPresent())
            return harLog.getPages().size() > 0 ?
                    Optional.of(harLog.getPages().get(0)) : Optional.empty();
        return parentPage;
    }

    private static Optional<HarEntry> getHarEntryByRequestId(
            String requestId,
            HarLog harLog,
            List<HarEntry> entriesWithoutPage
    ) {
        Optional<HarEntry> harEntryByRequestId = harLog.getEntries()
                .stream()
                .filter(e ->
                        e.getAdditional().getOrDefault("_requestId", "").equals(requestId))
                .findFirst();

        if (!harEntryByRequestId.isPresent() && entriesWithoutPage != null)
            return entriesWithoutPage
                    .stream()
                    .filter(e ->
                            e.getAdditional().getOrDefault("_requestId", "").equals(requestId))
                    .findFirst();
        return harEntryByRequestId;
    }
}
