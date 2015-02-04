package com.ruptech.tttalk_android.task;

public abstract class TaskAdapter implements TaskListener {

    @Override
    public void onCancelled(GenericTask task) {
    }

    @Override
    public void onPostExecute(GenericTask task, TaskResult result) {
    }

    @Override
    public void onPreExecute(GenericTask task) {
    }

    @Override
    public void onProgressUpdate(GenericTask task, Object param) {
    }
}
