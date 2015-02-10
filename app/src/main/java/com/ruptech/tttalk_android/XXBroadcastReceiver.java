package com.ruptech.tttalk_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import com.ruptech.tttalk_android.bus.NetChangeEvent;
import com.ruptech.tttalk_android.service.TTTalkService;
import com.ruptech.tttalk_android.utils.PrefUtils;

public class XXBroadcastReceiver extends BroadcastReceiver {
    public static final String BOOT_COMPLETED_ACTION = "com.ruptech.tttalk_android.action.BOOT_COMPLETED";
    private static final String TAG = XXBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action = " + action);
        if (TextUtils.equals(action, ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            App.mBus.post(new NetChangeEvent(connectivity));
        } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            Log.d(TAG, "System shutdown, stopping service.");
            Intent xmppServiceIntent = new Intent(context, TTTalkService.class);
            context.stopService(xmppServiceIntent);
        } else {
            if (PrefUtils.getPrefBoolean(
                    PrefUtils.AUTO_START, true)) {
                Intent i = new Intent(context, TTTalkService.class);
                i.setAction(BOOT_COMPLETED_ACTION);
                context.startService(i);
            }
        }
    }
}
