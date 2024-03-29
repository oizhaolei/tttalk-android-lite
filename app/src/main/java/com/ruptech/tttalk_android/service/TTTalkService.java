package com.ruptech.tttalk_android.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.XXBroadcastReceiver;
import com.ruptech.tttalk_android.activity.LoginActivity;
import com.ruptech.tttalk_android.activity.MainActivity;
import com.ruptech.tttalk_android.bus.ConnectionStatusChangedEvent;
import com.ruptech.tttalk_android.bus.NetChangeEvent;
import com.ruptech.tttalk_android.db.RosterProvider;
import com.ruptech.tttalk_android.exception.XMPPException;
import com.ruptech.tttalk_android.smack.Smack;
import com.ruptech.tttalk_android.smack.SmackImpl;
import com.ruptech.tttalk_android.smack.SmackListener;
import com.ruptech.tttalk_android.utils.NetUtil;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.squareup.otto.Subscribe;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class TTTalkService extends BaseService implements SmackListener {
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = -1;
    public static final int CONNECTING = 1;
    // private boolean mIsNeedReConnection = false; // 是否需要重连
    private int mConnectedState = DISCONNECTED; // 是否已经连接
    public static final String PONG_TIMEOUT = "pong timeout";// 连接超时
    public static final String NETWORK_ERROR = "network error";// 网络错误
    public static final String LOGOUT = "logout";// 手动退出
    public static final String LOGIN_FAILED = "login failed";// 登录失败
    public static final String DISCONNECTED_WITHOUT_WARNING = "disconnected without warning";// 没有警告的断开连接
    private static final String TAG = TTTalkService.class.getName();
    // 自动重连 start
    private static final int RECONNECT_AFTER = 5;
    private int mReconnectTimeout = RECONNECT_AFTER;
    private static final int RECONNECT_MAXIMUM = 10 * 60;// 最大重连时间间隔
    private static final String RECONNECT_ALARM = "com.ruptech.tttalk_android.RECONNECT_ALARM";
    private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
    private static final String PING_ALARM = "com.ruptech.tttalk_android.PING_ALARM";
    private Intent mPingAlarmIntent = new Intent(PING_ALARM);
    private static final String PONG_TIMEOUT_ALARM = "com.ruptech.tttalk_android.PONG_TIMEOUT_ALARM";
    private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
    private static final String[] GROUPS_QUERY = new String[]{
            RosterProvider.RosterConstants._ID, RosterProvider.RosterConstants.GROUP,};
    private IBinder mBinder = new XXBinder();
    private Thread mConnectingThread;
    private Handler mMainHandler = new Handler();
    private boolean mIsFirstLoginAction;
    private PendingIntent mPAlarmIntent;
    private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
    // 自动重连 end
    private ActivityManager mActivityManager;
    private String mPackageName;
    private HashSet<String> mIsBoundTo = new HashSet<String>();
    private PendingIntent mPingAlarmPendIntent;
    private PendingIntent mPongTimeoutAlarmPendIntent;
    private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
    private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "[SERVICE] onBind");
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.add(chatPartner);
        }
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)
                && TextUtils.equals(action, LoginActivity.LOGIN_ACTION)) {
            mIsFirstLoginAction = true;
        } else {
            mIsFirstLoginAction = false;
        }
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.add(chatPartner);
        }
        String action = intent.getAction();
        mIsFirstLoginAction = TextUtils.equals(action, LoginActivity.LOGIN_ACTION);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.remove(chatPartner);
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App.mBus.register(this);
        //BaseActivity.mListeners.add(this);
        mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        mPackageName = getPackageName();
        mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && intent.getAction() != null
                && TextUtils.equals(intent.getAction(),
                XXBroadcastReceiver.BOOT_COMPLETED_ACTION)) {
            String account = App.readUser().getAccount();
            String password = App.readUser().getPassword();
            if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(password))
                login(account, password);
        }
        mMainHandler.removeCallbacks(monitorStatus);
        mMainHandler.postDelayed(monitorStatus, 1000L);// 检查应用是否在后台运行线程
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.mBus.unregister(this);
        //BaseActivity.mListeners.remove(this);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .cancel(mPAlarmIntent);// 取消重连闹钟
        unregisterReceiver(mAlarmReceiver);// 注销广播监听
        logout();
    }

    // 登录
    public void login(final String account, final String password) {
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
            connectionFailed(NETWORK_ERROR);
            return;
        }
        if (mConnectingThread != null) {
            Log.i(TAG, "a connection is still goign on!");
            return;
        }
        mConnectingThread = new Thread() {
            @Override
            public void run() {
                try {
                    postConnecting();
                    App.mSmack = createSmack();
                    if (App.mSmack.login(account, password)) {
                        // 登陆成功
                        postConnectionScuessed();
                    } else {
                        // 登陆失败
                        onConnectionFailed(LOGIN_FAILED);
                    }
                } catch (XMPPException e) {
                    String message = e.getLocalizedMessage();
                    // 登陆失败
                    if (e.getCause() != null)
                        message += "\n" + e.getCause().getLocalizedMessage();
                    onConnectionFailed(message);
                    Log.i(TAG, "YaximXMPPException in doConnect():");
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    if (mConnectingThread != null)
                        synchronized (mConnectingThread) {
                            mConnectingThread = null;
                        }
                }
            }

        };
        mConnectingThread.start();
    }

    // 退出
    public boolean logout() {
        // mIsNeedReConnection = false;// 手动退出就不需要重连闹钟了
        boolean isLogout = false;
        if (mConnectingThread != null) {
            synchronized (mConnectingThread) {
                try {
                    mConnectingThread.interrupt();
                    mConnectingThread.join(50);
                } catch (InterruptedException e) {
                    Log.e(TAG, "doDisconnect: failed catching connecting thread");
                } finally {
                    mConnectingThread = null;
                }
            }
        }
        if (App.mSmack != null) {
            isLogout = App.mSmack.logout();
            App.mSmack = null;
        }
        connectionFailed(LOGOUT);// 手动退出
        return isLogout;
    }

    // 发送消息
    public void sendMessage(String user, String message, Collection<PacketExtension> extensions) {
        if (App.mSmack != null)
            App.mSmack.sendMessage(user, message, extensions);
        else
            SmackImpl.sendOfflineMessage(getContentResolver(), user, message);
    }

    // 是否连接上服务器
    public boolean isAuthenticated() {
        if (App.mSmack != null) {
            return App.mSmack.isAuthenticated();
        }

        return false;
    }

    // 清除通知栏
    public void clearNotifications(String Jid) {
        clearNotification(Jid);
    }

    /**
     * 非UI线程连接失败反馈
     *
     * @param reason
     */
    public void onConnectionFailed(final String reason) {
        mMainHandler.post(new Runnable() {
            public void run() {
                connectionFailed(reason);
            }
        });
    }

    // 设置连接状态
    public void setStatusFromConfig() {
        App.mSmack.setStatusFromConfig();
    }

    // 新增联系人
    public void addRosterItem(String user, String alias, String group) {
        try {
            App.mSmack.addRosterItem(user, alias, group);
        } catch (XMPPException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "exception in addRosterItem(): " + e.getMessage());
        }
    }

    // 新增分组
    public void addRosterGroup(String group) {
        App.mSmack.addRosterGroup(group);
    }

    // 删除联系人
    public void removeRosterItem(String user) {
        try {
            App.mSmack.removeRosterItem(user);
        } catch (XMPPException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "exception in removeRosterItem(): " + e.getMessage());
        }
    }

    // 将联系人移动到其他组
    public void moveRosterItemToGroup(String user, String group) {
        try {
            App.mSmack.moveRosterItemToGroup(user, group);
        } catch (XMPPException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "exception in moveRosterItemToGroup(): " + e.getMessage());
        }
    }

    // 重命名联系人
    public void renameRosterItem(String user, String newName) {
        try {
            App.mSmack.renameRosterItem(user, newName);
        } catch (XMPPException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "exception in renameRosterItem(): " + e.getMessage());
        }
    }

    // 重命名组
    public void renameRosterGroup(String group, String newGroup) {
        App.mSmack.renameRosterGroup(group, newGroup);
    }

    public byte[] getAvatar(String jid) throws XMPPException {
        return App.mSmack.getAvatar(jid);
    }

    /**
     * UI线程反馈连接失败
     *
     * @param reason
     */
    private void connectionFailed(String reason) {
        Log.i(TAG, "connectionFailed: " + reason);
        mConnectedState = DISCONNECTED;// 更新当前连接状态
        if (TextUtils.equals(reason, LOGOUT)) {// 如果是手动退出
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);
            return;
        }
        // 回调
        App.mBus.post(new ConnectionStatusChangedEvent(mConnectedState, reason));
        if (mIsFirstLoginAction)// 如果是第一次登录,就算登录失败也不需要继续
            return;

//        if (mConnectionStatusCallback != null) {
//            mConnectionStatusCallback.answerConnectionStatusChanged(mConnectedState,
//                    reason);
//        }

        // 无网络连接时,直接返回
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);
            return;
        }

        String account = App.readUser().getAccount();
        String password = App.readUser().getPassword();
        // 无保存的帐号密码时，也直接返回
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            Log.d(TAG, "account = null || password = null");
            return;
        }
        // 如果不是手动退出并且需要重新连接，则开启重连闹钟
        if (PrefUtils.getPrefBoolean(
                PrefUtils.AUTO_RECONNECT, true)) {
            Log.d(TAG, "connectionFailed(): registering reconnect in "
                    + mReconnectTimeout + "s");
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(
                    AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                            + mReconnectTimeout * 1000, mPAlarmIntent);
            mReconnectTimeout = mReconnectTimeout * 2;
            if (mReconnectTimeout > RECONNECT_MAXIMUM)
                mReconnectTimeout = RECONNECT_MAXIMUM;
        } else {
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPAlarmIntent);
        }

    }

    private void postConnectionScuessed() {
        mMainHandler.post(new Runnable() {
            public void run() {
                connectionScuessed();
            }

        });
    }

    private void connectionScuessed() {
        mConnectedState = CONNECTED;// 已经连接上
        mReconnectTimeout = RECONNECT_AFTER;// 重置重连的时间

        App.mBus.post(new ConnectionStatusChangedEvent(mConnectedState, ""));
//        if (mConnectionStatusCallback != null)
//            mConnectionStatusCallback.answerConnectionStatusChanged(mConnectedState,
//                    "");
    }    Runnable monitorStatus = new Runnable() {
        public void run() {
            try {
                Log.i(TAG, "monitorStatus is running... " + mPackageName);
                mMainHandler.removeCallbacks(monitorStatus);
                // 如果在后台运行并且连接上了
                if (!isAppOnForeground()) {
                    Log.i(TAG, "app run in background...");
                    // if (isAuthenticated())
                    updateServiceNotification(getString(R.string.run_bg_ticker));
                    return;
                } else {
                    stopForeground(true);
                }
                // mMainHandler.postDelayed(monitorStatus, 1000L);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    };

    // 连接中，通知界面线程做一些处理
    private void postConnecting() {
        // TODO Auto-generated method stub
        mMainHandler.post(new Runnable() {
            public void run() {
                connecting();
            }
        });
    }

    private void connecting() {
        // TODO Auto-generated method stub
        mConnectedState = CONNECTING;// 连接中
        String reason="";
        App.mBus.post(new ConnectionStatusChangedEvent(mConnectedState, reason));
//        if (mConnectionStatusCallback != null)
//            mConnectionStatusCallback.answerConnectionStatusChanged(mConnectedState,
//                    "");
    }

    // 收到新消息
    public void onNewMessage(final String from, final String message) {
        mMainHandler.post(new Runnable() {
            public void run() {
                if (!PrefUtils.getPrefBoolean(
                        PrefUtils.SCLIENTNOTIFY, false))
                    MediaPlayer.create(TTTalkService.this, R.raw.office).start();
                if (!isAppOnForeground())
                    notifyClient(from, App.mSmack.getNameForJID(from), message,
                            !mIsBoundTo.contains(from));
                // T.showLong(XXService.this, from + ": " + message);

            }

        });
    }

    // 联系人改变
    public void onRosterChanged() {
        // gracefully handle^W ignore events after a disconnect
        if (App.mSmack == null)
            return;
        if (App.mSmack != null && !App.mSmack.isAuthenticated()) {
            Log.i(TAG, "onRosterChanged(): disconnected without warning");
            connectionFailed(DISCONNECTED_WITHOUT_WARNING);
        }
    }

    /**
     * 更新通知栏
     *
     * @param message
     */
    public void updateServiceNotification(String message) {
        if (!PrefUtils.getPrefBoolean(
                PrefUtils.FOREGROUND, true))
            return;
        String title = App.readUser().getAccount();

        Notification n = new Notification(R.drawable.default_portrait,
                title, System.currentTimeMillis());
        n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        n.contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        n.setLatestEventInfo(this, title, message, n.contentIntent);
        startForeground(SERVICE_NOTIFICATION, n);
    }

    public boolean isAppOnForeground() {
        List<RunningTaskInfo> taskInfos = mActivityManager.getRunningTasks(1);
        if (taskInfos.size() > 0
                && TextUtils.equals(getPackageName(),
                taskInfos.get(0).topActivity.getPackageName())) {
            return true;
        }

        // List<RunningAppProcessInfo> appProcesses = mActivityManager
        // .getRunningAppProcesses();
        // if (appProcesses == null)
        // return false;
        // for (RunningAppProcessInfo appProcess : appProcesses) {
        // // Log.i(TAG, appProcess.processName);
        // // The name of the process that this object is associated with.
        // if (appProcess.processName.equals(mPackageName)
        // && appProcess.importance ==
        // RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
        // return true;
        // }
        // }
        return false;
    }

    @Subscribe
    public void answerNetChange(NetChangeEvent event) {
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {// 如果是网络断开，不作处理
            connectionFailed(NETWORK_ERROR);
            return;
        }
        if (isAuthenticated())// 如果已经连接上，直接返回
            return;

        String account = App.readUser().getAccount();
        String password = App.readUser().getPassword();

        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))// 如果没有帐号，也直接返回
            return;
        if (!PrefUtils.getPrefBoolean(
                PrefUtils.AUTO_RECONNECT, true))// 不需要重连
            return;
        login(account, password);// 重连
    }    // 判断程序是否在后台运行的任务

    public void onLogin() {
        mPingAlarmPendIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, mPingAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, mPongTimeoutAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(mPingAlarmReceiver, new IntentFilter(
                PING_ALARM));
        registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(
                PONG_TIMEOUT_ALARM));
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .setInexactRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis()
                                + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        mPingAlarmPendIntent);
    }

    public void onLogout() {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .cancel(mPingAlarmPendIntent);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .cancel(mPongTimeoutAlarmPendIntent);
        unregisterReceiver(mPingAlarmReceiver);
        unregisterReceiver(mPongTimeoutAlarmReceiver);
    }

    public void unRegisterTimeoutAlarm() {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .cancel(mPongTimeoutAlarmPendIntent);
    }

    public void registerTimeoutAlarm() {
        // register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                        + SmackImpl.PACKET_TIMEOUT + 3000, mPongTimeoutAlarmPendIntent);
    }

    public void createAccount(final String username, final String password) {
        // TODO
    }

    private Smack createSmack() {
        if (App.mSmack == null)
            App.mSmack = new SmackImpl(TTTalkService.this, TTTalkService.this, getContentResolver());
        return App.mSmack;
    }

    public List<String> getRosterGroups() {
        // we want all, online and offline
        List<String> list = new ArrayList<>();
        Cursor cursor = getContentResolver().query(RosterProvider.GROUPS_URI,
                GROUPS_QUERY, null, null, RosterProvider.RosterConstants.GROUP);
        int idx = cursor.getColumnIndex(RosterProvider.RosterConstants.GROUP);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(idx));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    /**
     * BroadcastReceiver to trigger reconnect on pong timeout.
     */
    private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
        public void onReceive(Context ctx, Intent i) {
            onConnectionFailed(TTTalkService.PONG_TIMEOUT);
            logout();// 超时就断开连接
        }
    }

    /**
     * BroadcastReceiver to trigger sending pings to the server
     */
    private class PingAlarmReceiver extends BroadcastReceiver {
        public void onReceive(Context ctx, Intent i) {
            if (App.mSmack.isAuthenticated()) {
                App.mSmack.sendServerPing();
            } else
                Log.d(TAG, "Ping: alarm received, but not connected to server.");
        }
    }

    public class XXBinder extends Binder {
        public TTTalkService getService() {
            return TTTalkService.this;
        }
    }

    // 自动重连广播
    private class ReconnectAlarmReceiver extends BroadcastReceiver {
        public void onReceive(Context ctx, Intent i) {
            Log.d(TAG, "Alarm received.");
            if (!PrefUtils.getPrefBoolean(
                    PrefUtils.AUTO_RECONNECT, true)) {
                return;
            }
            if (mConnectedState != DISCONNECTED) {
                Log.d(TAG, "Reconnect attempt aborted: we are connected again!");
                return;
            }
            String account = App.readUser().getAccount();
            String password = App.readUser().getPassword();

            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
                Log.d(TAG, "account = null || password = null");
                return;
            }
            login(account, password);
        }
    }




}
