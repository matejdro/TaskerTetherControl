package com.matejdro.taskertethercontrol;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.matejdro.taskertethercontrol.taskerutils.LocaleConstants;
import com.matejdro.taskertethercontrol.taskerutils.TaskerSetupActivity;

import net.dinglisch.android.tasker.TaskerPlugin;

public class TetherSetupActivity extends TaskerSetupActivity {
    private RadioButton enableButton;
    private RadioButton disableButton;

    private TextView warningView;
    private Button fixPermissionButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tasker_control);

        enableButton = findViewById(R.id.enableButton);
        disableButton = findViewById(R.id.disableButton);

        super.onCreate(savedInstanceState);

        warningView = findViewById(R.id.warning_box);
        fixPermissionButton = findViewById(R.id.action_button);
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Settings.System.canWrite(this)) {
            warningView.setVisibility(View.VISIBLE);
            fixPermissionButton.setVisibility(View.VISIBLE);

            warningView.setText(R.string.no_settings_permission_warning);
            warningView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_warning, 0, 0, 0
            );

            fixPermissionButton.setText(R.string.open_settings);
        } else if (checkPermission("android.permission.TETHER_PRIVILEGED",
                android.os.Process.myPid(),
                android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            warningView.setVisibility(View.VISIBLE);
            fixPermissionButton.setVisibility(View.VISIBLE);

            warningView.setText(R.string.no_system_warning);
            warningView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_info, 0, 0, 0
            );

            fixPermissionButton.setText(R.string.install_app_to_the_system);
        } else {
            warningView.setVisibility(View.GONE);
            fixPermissionButton.setVisibility(View.GONE);
        }

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


    public void fixPermissions(View view) {
        if (!Settings.System.canWrite(this)) {
            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            settingsIntent.setData(Uri.parse("package:" + getPackageName()));

            try {
                startActivity(settingsIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new SystemInstallTask(this).execute((Void[]) null);
    }

}
