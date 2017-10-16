package com.matejdro.taskertethercontrol;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.matejdro.taskertethercontrol.taskerutils.LocaleConstants;

import net.dinglisch.android.tasker.TaskerPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.content.Context.CONNECTIVITY_SERVICE;

@SuppressWarnings("unchecked")
@SuppressLint("PrivateApi")
public class TaskerReceiver extends BroadcastReceiver {
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
                Class internalConnectivityManagerClass = Class.forName("android.net.IConnectivityManager");

                Field internalConnectivityManagerField = ConnectivityManager.class.getDeclaredField("mService");
                internalConnectivityManagerField.setAccessible(true);

                Object internalConnectivityManager = internalConnectivityManagerField.get(connectivityManager);

                Method startTetheringMethod = internalConnectivityManagerClass.getDeclaredMethod("startTethering",
                        int.class,
                        ResultReceiver.class,
                        boolean.class);

                ResultReceiver dummyResultReceiver = new ResultReceiver(null);

                startTetheringMethod.invoke(internalConnectivityManager,
                        0,
                        dummyResultReceiver,
                        false
                );
            } else {
                Method stopTetheringMethod = connectivityClass.getDeclaredMethod("stopTethering", int.class);
                stopTetheringMethod.invoke(connectivityManager, 0);
            }

        } catch (Exception e) {
            Bundle vars = new Bundle();
            String errorMessage = ExceptionUtils.getNestedExceptionMessages(e);
            vars.putString(TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE, errorMessage);
            TaskerPlugin.addVariableBundle(getResultExtras(true), vars);
            setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);

            return;
        }

        setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
    }
}
