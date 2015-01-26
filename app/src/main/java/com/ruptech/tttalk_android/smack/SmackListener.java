package com.ruptech.tttalk_android.smack;

/**
 * Created by zhaolei on 15/1/26.
 */
public interface SmackListener {
    void onConnectionFailed(String message);

    void onRosterChanged();

    void onNewMessage(String fromJID, String chatMessage);

    void unRegisterTimeoutAlarm();

    void onLogin();

    void registerTimeoutAlarm();

    void onLogout();
}
