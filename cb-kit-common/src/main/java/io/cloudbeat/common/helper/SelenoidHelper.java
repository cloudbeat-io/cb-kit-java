package io.cloudbeat.common.helper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class SelenoidHelper {
    private static final int CONNECTION_TIMEOUT = 2000;
    private static final int READ_TIMEOUT = 2000;
    public static boolean isSelenoid(String serverUrl) {
        if (serverUrl == null || serverUrl.length() == 0)
            return false;
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            if (status != 200)
                return false;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();
            return content.toString().indexOf("You are using Selenoid") > -1;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static byte[] getVideoFile(String hubUrl, String videoName) {
        try {
            URL url = new URL(hubUrl);
            String videoUrlStr = String.format("%s://%s/video/%s", url.getProtocol(), url.getAuthority(), videoName);
            URL videoUrl = new URL(videoUrlStr);
            HttpURLConnection conn = (HttpURLConnection) videoUrl.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            if (status != 200)
                return null;
            return readAllBytes(conn.getInputStream());
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);

                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }
}
