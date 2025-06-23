package com.example.snoutandabout;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.android.core.SentryAndroid;
import io.sentry.android.core.SentryAndroidOptions;

public class MyApplication extends Application {
    private ITransaction appSessionTransaction;

    @Override
    public void onCreate() {
        super.onCreate();
        String currentPackage = getPackageName();
        List<String> allowedPackages = Arrays.asList(BuildConfig.ALLOWED_PACKAGE_NAMES.split(","));

        if (allowedPackages.contains(currentPackage)) {
            Log.d("INITIALIZED", "true");
            SentryAndroid.init(this, options -> {
                options.setDsn();
                options.setEnableAutoActivityLifecycleTracing(true);
                options.setEnableUserInteractionTracing(true);
                options.setTracesSampleRate(1.0);
                options.setRelease("1.0");
            });
            Sentry.setTag("package_name", currentPackage);
            appSessionTransaction = Sentry.startTransaction("App Session", "app.session");
        } else {
            Log.d("SENTRY INITIALIZED", "false");
        }

    }

    // This is tricky and might not be called reliably
    @Override
    public void onTerminate() {
        super.onTerminate();
        if (appSessionTransaction != null && !appSessionTransaction.isFinished()) {
            appSessionTransaction.finish(SpanStatus.OK);
        }
    }
}