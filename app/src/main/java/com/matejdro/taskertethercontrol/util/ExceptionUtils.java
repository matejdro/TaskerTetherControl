package com.matejdro.taskertethercontrol.util;

public class ExceptionUtils {
    public static String getNestedExceptionMessages(Throwable throwable) {
        StringBuilder message = new StringBuilder();
        while (true) {
            message.append(throwable.getClass().getName());
            message.append(": ");
            message.append(throwable.getMessage());

            if (throwable.getCause() != null) {
                message.append('\n');
                throwable = throwable.getCause();
            } else {
                break;
            }
        }

        return message.toString();
    }

    public static boolean isSecurityException(Throwable throwable) {
        while (true) {
            if (throwable instanceof SecurityException) {
                return true;
            }

            if (throwable.getCause() != null) {
                throwable = throwable.getCause();
            } else {
                return false;
            }
        }
    }
}
