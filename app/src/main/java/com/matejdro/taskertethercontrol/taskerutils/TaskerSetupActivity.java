package com.matejdro.taskertethercontrol.taskerutils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public abstract class TaskerSetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!loadIntent()) {
            onFreshTaskerSetup();
        }

        super.onCreate(savedInstanceState);
    }

    private boolean loadIntent() {
        Intent intent = getIntent();

        if (intent == null) {
            return false;
        }

        Bundle bundle = intent.getBundleExtra(LocaleConstants.EXTRA_BUNDLE);
        if (bundle == null) {
            return false;
        }

        return onPreviousTaskerOptionsLoaded(bundle);
    }

    protected abstract boolean onPreviousTaskerOptionsLoaded(Bundle taskerOptions);

    protected void onFreshTaskerSetup() {

    }
}
