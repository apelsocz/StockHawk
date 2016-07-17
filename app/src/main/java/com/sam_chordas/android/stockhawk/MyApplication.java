package com.sam_chordas.android.stockhawk;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private ConnectivityManager mConnectivityManager;

    public static MyApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (!isConnected()) {
//            NetworkConnectivityReceiver.setEnabled(true);
        }
        scheduleGCM();
    }

    public boolean isConnected() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public boolean scheduleGCM() {
        if (isConnected()) {
            final long PERIOD = 3600L;
            final long FLEX = 10L;
            final String PERIODIC_TAG = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(PERIOD)
                    .setFlex(FLEX)
                    .setTag(PERIODIC_TAG)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();

            // Schedule task with tag "periodic" - this ensure that only the
            // stocks present in the DB are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);

            return true;
        }
        return false;
    }
}