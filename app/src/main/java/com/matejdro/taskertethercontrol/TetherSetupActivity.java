package com.matejdro.taskertethercontrol;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import com.matejdro.taskertethercontrol.taskerutils.LocaleConstants;
import com.matejdro.taskertethercontrol.taskerutils.TaskerSetupActivity;

import net.dinglisch.android.tasker.TaskerPlugin;

import java.io.InputStream;
import java.io.OutputStreamWriter;

public class TetherSetupActivity extends TaskerSetupActivity {
    private RadioButton enableButton;
    private RadioButton disableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tasker_control);

        enableButton = (RadioButton) findViewById(R.id.enableButton);
        disableButton = (RadioButton) findViewById(R.id.disableButton);

        super.onCreate(savedInstanceState);

        View warningView = findViewById(R.id.warning_box);
        View installButtonView = findViewById(R.id.install_button);

        if (checkPermission("android.permission.MANAGE_USERS",
                android.os.Process.myPid(),
                android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            warningView.setVisibility(View.GONE);
            installButtonView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    protected void onFreshTaskerSetup() {
        enableButton.setChecked(true);
    }

    @Override
    protected boolean onPreviousTaskerOptionsLoaded(Bundle taskerOptions) {
        boolean enableTethering = taskerOptions.getBoolean(TaskerConstants.KEY_ENABLE_TETHERING, true);

        if (enableTethering) {
            enableButton.setChecked(true);
        } else {
            disableButton.setChecked(true);
        }

        return true;
    }

    protected void save() {
        Intent intent = new Intent();

        boolean enableTethering = enableButton.isChecked();

        String description = getString(enableTethering ? R.string.enable_tethering : R.string.disable_tethering);

        Bundle dataStorage = new Bundle();
        dataStorage.putBoolean(TaskerConstants.KEY_ENABLE_TETHERING, enableTethering);

        intent.putExtra(LocaleConstants.EXTRA_STRING_BLURB, description);
        intent.putExtra(LocaleConstants.EXTRA_BUNDLE, dataStorage);

        TaskerPlugin.Setting.requestTimeoutMS(intent, 7000);

        setResult(RESULT_OK, intent);
    }


    public void installSystemApp(View view) {
        new SystemInstallTask().execute((Void[]) null);
    }

    private class SystemInstallTask extends AsyncTask<Void, Void, Boolean> {
        private String errorMessage = getString(R.string.unknown_error);
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(TetherSetupActivity.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Installing app to system...");
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!RootUtil.isRootAccessible()) {
                errorMessage = getString(R.string.no_root);
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
                String apkPath = getPackageResourcePath();
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

            if (success) {
                new AlertDialog.Builder(TetherSetupActivity.this)
                        .setTitle(R.string.install_success)
                        .setMessage(R.string.reboot_request)
                        .setPositiveButton(android.R.string.ok, null).show();
            } else {
                new AlertDialog.Builder(TetherSetupActivity.this)
                        .setTitle(R.string.install_failed)
                        .setMessage(errorMessage)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        }
    }
}
