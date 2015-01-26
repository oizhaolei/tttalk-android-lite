package com.ruptech.tttalk_android.service;

public interface IConnectionStatusCallback {
    public void connectionStatusChanged(int connectedState, String reason);
}
