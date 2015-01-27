package com.ruptech.tttalk_android.task;

import android.os.AsyncTask;

import java.util.Observable;
import java.util.Observer;

public abstract class GenericTask extends AsyncTask<TaskParams, Object, TaskResult> implements Observer {
    protected final String TAG = "TaskManager";
    private String msg;

    private boolean isCancelable = true;

    private TaskListener mListener = null;

    protected abstract TaskResult _doInBackground() throws Exception;


    @Override
    protected TaskResult doInBackground(TaskParams... params) {
        TaskResult result;
        try {
            result = _doInBackground();
        } catch (Exception e) {
            handleException(e);
            return TaskResult.FAILED;
        }
        return result;
    }

    public void doPublishProgress(Object... values) {
        super.publishProgress(values);
    }

    public TaskListener getListener() {
        return mListener;
    }

    public String getMsg() {
        return msg;
    }

    public Object[] getMsgs() {
        return new Object[0];
    }

    public abstract void handleException(Throwable e);

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mListener != null) {
            mListener.onCancelled(this);
        }
    }

    @Override
    protected void onPostExecute(TaskResult result) {
        super.onPostExecute(result);
        if (mListener != null) {
            mListener.onPostExecute(this, result);
        }

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mListener != null) {
            mListener.onPreExecute(this);
        }

    }

    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);

        if (mListener != null) {
            if (values != null && values.length > 0) {
                mListener.onProgressUpdate(this, values[0]);
            }
        }

    }

    public void setCancelable(boolean flag) {
        isCancelable = flag;
    }

    public void setListener(TaskListener taskListener) {
        mListener = taskListener;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (TaskManager.CANCEL_ALL == (Integer) arg && isCancelable) {
            if (getStatus() == Status.RUNNING) {
                cancel(true);
            }
        }
    }
}
