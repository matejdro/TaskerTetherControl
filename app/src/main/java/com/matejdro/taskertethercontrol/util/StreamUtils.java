package com.matejdro.taskertethercontrol.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

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
}
