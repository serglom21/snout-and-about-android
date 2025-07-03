package com.example.snoutandabout;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;

public class MyApplication extends Application {
    private ITransaction appSessionTransaction;
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        String currentPackage = BuildConfig.APPLICATION_ID;
        List<String> allowedPackages = Arrays.asList(BuildConfig.ALLOWED_PACKAGE_NAMES.split(","));

        Log.d(TAG, "Application onCreate called");
        Log.d(TAG, "Current package: " + currentPackage);

        if (allowedPackages.contains(currentPackage)) {
            Log.d(TAG, "Application onCreate - Sentry already initialized by ContentProvider");
            
            // Sentry is already initialized by SentryInitProvider
            // Get the app session transaction for potential cleanup
            appSessionTransaction = SentryInitProvider.getAppSessionTransaction();
            Log.d(TAG, "Retrieved app session transaction: " + (appSessionTransaction != null ? "exists" : "null"));
        } else {
            Log.d(TAG, "Application onCreate - Sentry initialization was skipped (package not in allowed list)");
        }
    }

    // This is tricky and might not be called reliably
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Application onTerminate called");
        // Finish the app session transaction if it exists and isn't finished
        SentryInitProvider.finishAppSessionTransaction(SpanStatus.OK);
    }

    /**
     * Public method to finish the app session transaction
     * This can be called from activities when appropriate
     */
    public static void finishAppSession() {
        Log.d(TAG, "finishAppSession called");
        SentryInitProvider.finishAppSessionTransaction(SpanStatus.OK);
    }
}