package io.cloudbeat.common.wrapper.console;

import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.LogLevel;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public final class SystemConsoleWrapper {
    private PrintStream orgSystemOut;
    private PrintStream orgSystemErr;
    private boolean capturing;

    public boolean start(CbTestReporter reporter) {
        if (capturing) {
            return false;
        }

        capturing = true;
        orgSystemOut = System.out;
        orgSystemErr = System.err;
        //OutputStream outputStreamCombiner =
          //      new OutputStreamCombiner(Arrays.asList(orgSystemOut, baos));
        try {
            PrintStream customSystemOut = new ConsolePrintStreamWrapper(System.out, reporter, LogLevel.INFO);
            System.setOut(customSystemOut);
            PrintStream customSystemErr = new ConsolePrintStreamWrapper(System.err, reporter, LogLevel.ERROR);
            System.setErr(customSystemErr);
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    public void stop() {
        if (!capturing)
            return;

        System.setOut(orgSystemOut);
        System.setErr(orgSystemErr);
        orgSystemOut = null;
        orgSystemErr = null;
        capturing = false;
    }


    private static class OutputStreamCombiner extends OutputStream {
        private List<OutputStream> outputStreams;

        public OutputStreamCombiner(List<OutputStream> outputStreams) {
            this.outputStreams = outputStreams;
        }

        public void write(int b) throws IOException {
            for (OutputStream os : outputStreams) {
                os.write(b);
            }
        }

        public void flush() throws IOException {
            for (OutputStream os : outputStreams) {
                os.flush();
            }
        }

        public void close() throws IOException {
            for (OutputStream os : outputStreams) {
                os.close();
            }
        }
    }
}
