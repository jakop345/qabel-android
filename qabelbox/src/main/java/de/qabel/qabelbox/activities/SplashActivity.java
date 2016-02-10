package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.Sanity;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Created by danny on 11.01.2016.
 */
public class SplashActivity extends Activity {

    private final long SPLASH_TIME = 1500;
    private SplashActivity mActivity;
    private AppPreference prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_splashscreen);
        setupAppPreferences();
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
        int lastAppStartVersion = prefs.getLastAppStartVersion();

        int currentAppVersionCode = 0;
        try {
            currentAppVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (lastAppStartVersion == 0) {
            prefs.setLastAppStartVersion(currentAppVersionCode);
            //@todo show welcome
        } else {
            if (lastAppStartVersion != currentAppVersionCode) {
                prefs.setLastAppStartVersion(currentAppVersionCode);
                //@todo show whatsnew screen
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startDelayedHandler();
        }
    }

    private void startDelayedHandler() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                startMainActivity();
            }

            private void startMainActivity() {

                if (!Sanity.startWizardActivities(mActivity)) {

                    Intent intent = new Intent(mActivity, MainActivity.class);
                    intent.setAction("");
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            }
        }
                , SPLASH_TIME);
    }


}
