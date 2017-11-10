package com.matejdro.taskertethercontrol;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.matejdro.taskertethercontrol.util.ExceptionUtils;
import com.matejdro.taskertethercontrol.util.RootUtil;
import com.matejdro.taskertethercontrol.util.StreamUtils;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;

class SystemInstallTask extends AsyncTask<Void, Void, Boolean> {
    private String errorMessage;
    @SuppressWarnings("deprecation")
    private ProgressDialog progressDialog;

    private WeakReference<Context> contextRef;

    public SystemInstallTask(Context context) {
        errorMessage = context.getString(R.string.unknown_error);
        contextRef = new WeakReference<>(context);

        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Installing app to system...");
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null) {
            return false;
        }

        if (!RootUtil.isRootAccessible()) {
            errorMessage = context.getString(R.string.no_root);
            return false;
        }

        Process suProcess = null;
        try {
            suProcess = Runtime.getRuntime().exec("su");

            OutputStreamWriter streamWriter = new OutputStreamWriter(suProcess.getOutputStream());

            final InputStream errorStream = suProcess.getErrorStream();

            //Mount system as R/W
            streamWriter.write("mount -o rw,remount /system\n");
            streamWriter.flush();

            //Copy over APK
            String apkPath = context.getPackageResourcePath();
            streamWriter.write("cat " + apkPath + " > /system/priv-app/com.matejdro.taskertethercontrol.apk\n");
            streamWriter.flush();

            //Update file permissions
            streamWriter.write("chmod 644 /system/priv-app/com.matejdro.taskertethercontrol.apk\n");
            streamWriter.flush();

            //Mount system back to RO
            streamWriter.write("mount -o ro,remount /system\n");
            streamWriter.flush();

            //Exit SU
            streamWriter.write("exit\n");
            streamWriter.close();

            errorMessage = StreamUtils.readAndCloseStreamWithTimeout(errorStream, 5000);

            return errorMessage.trim().isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = ExceptionUtils.getNestedExceptionMessages(e);
            return false;
        } finally {
            if (suProcess != null) {
                suProcess.destroy();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        progressDialog.dismiss();

        Context context = contextRef.get();
        if (context == null) {
            return;
        }


        if (success) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.install_success)
                    .setMessage(R.string.reboot_request)
                    .setPositiveButton(android.R.string.ok, null).show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.install_failed)
                    .setMessage(errorMessage)
                    .setPositiveButton(android.R.string.ok, null).show();
        }
    }
}
