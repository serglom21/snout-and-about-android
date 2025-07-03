package com.example.snoutandabout;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.android.core.SentryAndroid;
import io.sentry.android.core.SentryAndroidOptions;

public class SentryInitProvider extends ContentProvider {
    private static ITransaction appSessionTransaction;
    private static final String TAG = "SentryInitProvider";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context is null in onCreate");
            return false;
        }

        String currentPackage = BuildConfig.APPLICATION_ID;
        List<String> allowedPackages = Arrays.asList(BuildConfig.ALLOWED_PACKAGE_NAMES.split(","));

        Log.d(TAG, "ContentProvider onCreate called");
        Log.d(TAG, "Current package: " + currentPackage);
        Log.d(TAG, "Allowed packages: " + BuildConfig.ALLOWED_PACKAGE_NAMES);

        if (allowedPackages.contains(currentPackage)) {
            Log.d(TAG, "Initializing Sentry in ContentProvider - package is in allowed list");
            
            try {
                // Initialize Sentry with the same configuration as before
                SentryAndroid.init(context, options -> {
                    options.setDsn("https://514d5e38a4003c2cf958c53c77679925@o4508236363464704.ingest.us.sentry.io/4508847444918272");
                    options.setEnableAutoActivityLifecycleTracing(true);
                    options.setEnableUserInteractionTracing(true);
                    options.setTracesSampleRate(1.0);
                    options.setRelease("5.0");
                    options.setDebug(true); // Enable debug mode to see more logs
                    
                    Log.d(TAG, "Sentry options configured");
                });

                Sentry.setTag("package_name", currentPackage);
                appSessionTransaction = Sentry.startTransaction("App Session", "app.session");
                
                // Add some data to the transaction to make it more meaningful
                appSessionTransaction.setData("app_start_time", System.currentTimeMillis());
                appSessionTransaction.setData("package_name", currentPackage);
                
                // Set a timeout to finish the app session transaction after 5 minutes
                // This ensures it gets sent to Sentry even if the app doesn't properly terminate
                new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (appSessionTransaction != null && !appSessionTransaction.isFinished()) {
                        Log.d(TAG, "App session transaction timeout - finishing automatically");
                        appSessionTransaction.finish(SpanStatus.OK);
                    }
                }, 5 * 60 * 1000); // 5 minutes
                
                // Test transaction to verify Sentry is working
                ITransaction testTransaction = Sentry.startTransaction("Test Transaction", "test");
                testTransaction.finish(SpanStatus.OK);
                Log.d(TAG, "Test transaction created and finished");
                
                // Another test transaction with more data
                ITransaction testTransaction2 = Sentry.startTransaction("Test Transaction 2", "test");
                testTransaction2.setData("test_key", "test_value");
                testTransaction2.setTag("test_tag", "test_value");
                testTransaction2.finish(SpanStatus.OK);
                Log.d(TAG, "Test transaction 2 created and finished");
                
                Log.d(TAG, "Sentry initialized successfully in ContentProvider");
                Log.d(TAG, "App session transaction started: " + (appSessionTransaction != null ? "yes" : "no"));
                Log.d(TAG, "App session transaction ID: " + (appSessionTransaction != null ? appSessionTransaction.getSpanContext().getTraceId() : "null"));
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Sentry in ContentProvider", e);
            }
        } else {
            Log.d(TAG, "Sentry initialization skipped - package not in allowed list");
        }

        return true;
    }

    /**
     * Get the app session transaction for cleanup purposes
     */
    public static ITransaction getAppSessionTransaction() {
        Log.d(TAG, "Getting app session transaction: " + (appSessionTransaction != null ? "exists" : "null"));
        return appSessionTransaction;
    }

    /**
     * Finish the app session transaction
     */
    public static void finishAppSessionTransaction(SpanStatus status) {
        Log.d(TAG, "finishAppSessionTransaction called with status: " + status);
        if (appSessionTransaction != null && !appSessionTransaction.isFinished()) {
            appSessionTransaction.finish(status);
            Log.d(TAG, "App session transaction finished with status: " + status);
            Log.d(TAG, "App session transaction finished successfully");
        } else {
            Log.d(TAG, "App session transaction not finished - transaction is " + 
                  (appSessionTransaction == null ? "null" : "already finished"));
        }
    }

    /**
     * Manually finish the app session transaction for testing
     */
    public static void finishAppSessionTransaction() {
        finishAppSessionTransaction(SpanStatus.OK);
    }

    // Required ContentProvider methods - these are not used for Sentry initialization
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
} 