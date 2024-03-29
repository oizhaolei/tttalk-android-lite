package com.ruptech.tttalk_android.task.impl;


import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.model.ServerAppInfo;
import com.ruptech.tttalk_android.task.GenericTask;
import com.ruptech.tttalk_android.task.TaskResult;

public class VersionCheckTask extends GenericTask {

    private ServerAppInfo serverAppInfo;

    public ServerAppInfo getServerAppInfo() {
        return serverAppInfo;
    }

    @Override
    protected TaskResult _doInBackground() throws Exception {
        // check version
        serverAppInfo = App.getHttpServer().ver();
        return TaskResult.OK;
    }

}
