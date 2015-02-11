package com.ruptech.tttalk_android.task;

import android.os.AsyncTask;
import android.util.Log;

import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.http.NetworkException;
import com.ruptech.tttalk_android.http.ServerSideException;

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

    public void setListener(TaskListener taskListener) {
        mListener = taskListener;
    }

    public String getMsg() {
        return msg;
    }

    public Object[] getMsgs() {
        return new Object[0];
    }

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

    @Override
    public void update(Observable o, Object arg) {
        if (TaskManager.CANCEL_ALL == (Integer) arg && isCancelable) {
            if (getStatus() == Status.RUNNING) {
                cancel(true);
            }
        }
    }

    protected void handleException(Throwable e) {
        if (BuildConfig.DEBUG)
            Log.e(TAG, e.getMessage(), e);
        if (e instanceof ServerSideException) {
            msg = e.getMessage();
            publishProgress(msg);
        } else if (e instanceof NetworkException) {
            msg = e.getMessage();
            publishProgress(msg);
        }
        if (!(e instanceof NetworkException)) {
//            Utils.sendClientException(e, getMsgs());
        }
    }
}
