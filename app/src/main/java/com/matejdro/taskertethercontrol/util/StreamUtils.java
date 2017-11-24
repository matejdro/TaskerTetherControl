package com.matejdro.taskertethercontrol.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class StreamUtils {
    public static String readAndCloseStreamWithTimeout(final InputStream stream, final int timeoutMs) throws
            IOException, InterruptedException {

        StringBuilder stringBuilder = new StringBuilder();

        Thread readTimeoutThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException ignored) {
                }

                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        };

        readTimeoutThread.start();

        try {
            int readByte;
            while ((readByte = stream.read()) != -1) {
                stringBuilder.append((char) readByte);
            }
        } catch (InterruptedIOException ignored) {
            stringBuilder.append("\nTIMEOUT");
        }

        readTimeoutThread.interrupt();
        readTimeoutThread.join();

        return stringBuilder.toString();
    }

    public static void copyData(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while ((read = from.read(buffer)) > 0) {
            to.write(buffer, 0, read);
        }
    }
}
