package io.cloudbeat.selenium.har;

import io.cloudbeat.common.har.model.HarEntry;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public final class TimingsHelper {
    static void calculateHarTimings(HarEntry harEntry, JSONObject timing) throws JSONException {
        double dnsStart = timing.getDouble("dnsStart");
        double dnsEnd = timing.getDouble("dnsEnd");
        double sslStart = timing.getDouble("sslStart");
        double sslEnd = timing.getDouble("sslEnd");
        double connectStart = timing.getDouble("connectStart");
        double connectEnd = timing.getDouble("connectEnd");
        double sendStart = timing.getDouble("sendStart");
        double sendEnd = timing.getDouble("sendEnd");
        double receiveHeadersEnd = timing.getDouble("receiveHeadersEnd");
        double receiveHeadersStart = timing.getDouble("receiveHeadersStart");
        double requestTime = timing.getDouble("requestTime");
        double pushStart = timing.getDouble("pushStart");
        double queueing = (requestTime - (double)harEntry.getAdditional().get("__timestamp")) * 1000;
        double blocked =
                firstNonNegativeOrNonZero(queueing, dnsStart, connectStart, sendStart);
        double dns = calculateOptionalTimeSpan(dnsStart, dnsEnd);
        double connect = calculateOptionalTimeSpan(connectStart, connectEnd);
        double send = calculateOptionalTimeSpan(sendStart, sendEnd);
        double wait = calculateOptionalTimeSpan(sendEnd, receiveHeadersStart);
        double ssl = calculateOptionalTimeSpan(sslStart, sslEnd);
        double receive = 0;
        harEntry.getTimings().setReceive(receive);
        harEntry.getTimings().setSsl(ssl);
        harEntry.getTimings().setWait(wait);
        harEntry.getTimings().setSend(send);
        harEntry.getTimings().setConnect(connect);
        harEntry.getTimings().setDns(dns);
        harEntry.getTimings().setBlocked(blocked);
        harEntry.setAdditionalField("_requestTime", requestTime);
        harEntry.setAdditionalField("__receiveHeadersEnd", receiveHeadersEnd);
        if (pushStart > 0)
            harEntry.setAdditionalField("_was_pushed", 1);
        harEntry.setTime(
                Math.max(0, blocked) +
                        Math.max(0, dns) +
                        Math.max(0, connect) +
                        send +
                        wait +
                        receive
        );
    }
    private static double calculateOptionalTimeSpan(double start, double end) {
        if (start >= 0) {
            return end - start;
        }
        return -1;
    }
    private static double firstNonNegativeOrNonZero(double... values) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] > 0) return values[i];
        }
        return -1;
    }
}
