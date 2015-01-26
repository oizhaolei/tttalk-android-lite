package com.ruptech.tttalk_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import com.ruptech.tttalk_android.service.XXService;
import com.ruptech.tttalk_android.utils.PrefUtils;

import java.util.ArrayList;

public class XXBroadcastReceiver extends BroadcastReceiver {
    public static final String BOOT_COMPLETED_ACTION = "com.ruptech.tttalk_android.action.BOOT_COMPLETED";
    private static final String TAG = XXBroadcastReceiver.class.getName();
    public static ArrayList<EventHandler> mListeners = new ArrayList<EventHandler>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action = " + action);
        if (TextUtils.equals(action, ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (mListeners.size() > 0)// 通知接口完成加载
                for (EventHandler handler : mListeners) {
                    handler.onNetChange();
                }
        } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            Log.d(TAG, "System shutdown, stopping service.");
            Intent xmppServiceIntent = new Intent(context, XXService.class);
            context.stopService(xmppServiceIntent);
        } else {
            if (!TextUtils.isEmpty(PrefUtils.getPrefString(context,
                    PrefUtils.PASSWORD, ""))
                    && PrefUtils.getPrefBoolean(context,
                    PrefUtils.AUTO_START, true)) {
                Intent i = new Intent(context, XXService.class);
                i.setAction(BOOT_COMPLETED_ACTION);
                context.startService(i);
            }
        }
    }

    public static abstract interface EventHandler {

        public abstract void onNetChange();
    }
}
