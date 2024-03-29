package com.ruptech.tttalk_android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.util.Log;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.ChatActivity;
import com.ruptech.tttalk_android.utils.PrefUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseService extends Service {

    private static final String TAG = "BaseService";
    private static final int MAX_TICKER_MSG_LEN = 50;
    protected static int SERVICE_NOTIFICATION = 1;
    protected WakeLock mWakeLock;
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private Intent mNotificationIntent;
    private Vibrator mVibrator;
    private Map<String, Integer> mNotificationCount = new HashMap<>(
            2);
    private Map<String, Integer> mNotificationId = new HashMap<>(
            2);
    private int mLastNotificationId = 2;

    @Override
    public void onCreate() {
        Log.i(TAG, "called onCreate()");
        super.onCreate();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationIntent = new Intent(this, ChatActivity.class);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "called onDestroy()");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "called onStartCommand()");
        return START_STICKY;
    }


    protected void notifyClient(String fromJid, String fromUserName,
                                String message, boolean showNotification) {
        if (!showNotification) {
            return;
        }
        mWakeLock.acquire();
        setNotification(fromJid, fromUserName, message);
        setLEDNotification();

        int notifyId = 0;
        if (mNotificationId.containsKey(fromJid)) {
            notifyId = mNotificationId.get(fromJid);
        } else {
            mLastNotificationId++;
            notifyId = mLastNotificationId;
            mNotificationId.put(fromJid, Integer.valueOf(notifyId));
        }

        // If vibration is set to true, add the vibration flag to
        // the notification and let the system decide.
        boolean vibraNotify = PrefUtils.getPrefBoolean(
                PrefUtils.VIBRATIONNOTIFY, true);
        if (vibraNotify) {
            mVibrator.vibrate(400);
        }
        mNotificationManager.notify(notifyId, mNotification);

        mWakeLock.release();
    }

    private void setNotification(String fromJid, String fromUserId,
                                 String message) {

        int mNotificationCounter = 0;
        if (mNotificationCount.containsKey(fromJid)) {
            mNotificationCounter = mNotificationCount.get(fromJid);
        }
        mNotificationCounter++;
        mNotificationCount.put(fromJid, mNotificationCounter);
        String author;
        if (null == fromUserId || fromUserId.length() == 0) {
            author = fromJid;
        } else {
            author = fromUserId;
        }
        String title = author;
        String ticker;
        boolean isTicker = PrefUtils.getPrefBoolean(
                PrefUtils.TICKER, true);
        if (isTicker) {
            int newline = message.indexOf('\n');
            int limit = 0;
            String messageSummary = message;
            if (newline >= 0)
                limit = newline;
            if (limit > MAX_TICKER_MSG_LEN
                    || message.length() > MAX_TICKER_MSG_LEN)
                limit = MAX_TICKER_MSG_LEN;
            if (limit > 0)
                messageSummary = message.substring(0, limit) + " [...]";
            ticker = title + ":\n" + messageSummary;
        } else
            ticker = author;
        mNotification = new Notification(R.mipmap.ic_launcher, ticker,
                System.currentTimeMillis());
        Uri userNameUri = Uri.parse(fromJid);
        mNotificationIntent.setData(userNameUri);
        mNotificationIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME,
                fromUserId);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // need to set flag FLAG_UPDATE_CURRENT to get extras transferred
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification.setLatestEventInfo(this, title, message, pendingIntent);
        if (mNotificationCounter > 1)
            mNotification.number = mNotificationCounter;
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
    }

    private void setLEDNotification() {
        boolean isLEDNotify = PrefUtils.getPrefBoolean(
                PrefUtils.LEDNOTIFY, true);
        if (isLEDNotify) {
            mNotification.ledARGB = Color.MAGENTA;
            mNotification.ledOnMS = 300;
            mNotification.ledOffMS = 1000;
            mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
        }
    }

    public void resetNotificationCounter(String userJid) {
        mNotificationCount.remove(userJid);
    }

    public void clearNotification(String Jid) {
        int notifyId = 0;
        if (mNotificationId.containsKey(Jid)) {
            notifyId = mNotificationId.get(Jid);
            mNotificationManager.cancel(notifyId);
        }
    }


}
