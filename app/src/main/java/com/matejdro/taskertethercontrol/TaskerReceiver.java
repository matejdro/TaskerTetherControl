package com.matejdro.taskertethercontrol;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.Settings;

import com.matejdro.taskertethercontrol.taskerutils.LocaleConstants;
import com.matejdro.taskertethercontrol.util.ExceptionUtils;

import net.dinglisch.android.tasker.TaskerPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.content.Context.CONNECTIVITY_SERVICE;

@SuppressWarnings("unchecked")
@SuppressLint("PrivateApi")
public class TaskerReceiver extends BroadcastReceiver {
    private static boolean hasSystemSettingsPermsision(Context context) {
        return Settings.System.canWrite(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Bundle bundle = intent.getBundleExtra(LocaleConstants.EXTRA_BUNDLE);
        if (bundle == null) {
            return;
        }

        boolean enableTethering = bundle.getBoolean(TaskerConstants.KEY_ENABLE_TETHERING, true);
        try {
            Class<ConnectivityManager> connectivityClass = ConnectivityManager.class;
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService
                    (CONNECTIVITY_SERVICE);

            if (enableTethering) {
                Field internalConnectivityManagerField = ConnectivityManager.class.getDeclaredField("mService");
                internalConnectivityManagerField.setAccessible(true);

                callStartTethering(internalConnectivityManagerField.get(connectivityManager));
            } else {
                Method stopTetheringMethod = connectivityClass.getDeclaredMethod("stopTethering", int.class);
                stopTetheringMethod.invoke(connectivityManager, 0);
            }

        } catch (Exception e) {
            Bundle vars = new Bundle();
            String errorMessage = ExceptionUtils.getNestedExceptionMessages(e);

            if (ExceptionUtils.isSecurityException(e)) {
                if (hasSystemSettingsPermsision(context)) {
                    errorMessage = context.getString(R.string.error_root_needed) + errorMessage;
                } else {
                    errorMessage = context.getString(R.string.error_no_settings_permission) + errorMessage;
                }
            }

            vars.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, errorMessage);
            TaskerPlugin.addVariableBundle(getResultExtras(true), vars);
            setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);

            return;
        }

        setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }

    private void callStartTethering(Object internalConnectivityManager) throws ReflectiveOperationException {
        Class internalConnectivityManagerClass = Class.forName("android.net.IConnectivityManager");

        ResultReceiver dummyResultReceiver = new ResultReceiver(null);

        try {
            Method startTetheringMethod = internalConnectivityManagerClass.getDeclaredMethod("startTethering",
                    int.class,
                    ResultReceiver.class,
                    boolean.class);

            startTetheringMethod.invoke(internalConnectivityManager,
                    0,
                    dummyResultReceiver,
                    false);
        } catch (NoSuchMethodException e) {
            // Newer devices have "callingPkg" String argument at the end of this method.
            Method startTetheringMethod = internalConnectivityManagerClass.getDeclaredMethod("startTethering",
                    int.class,
                    ResultReceiver.class,
                    boolean.class,
                    String.class);

            startTetheringMethod.invoke(internalConnectivityManager,
                    0,
                    dummyResultReceiver,
                    false,
                    "com.matejdro.taskertethercontrol");
        }
    }
}
