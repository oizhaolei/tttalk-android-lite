package com.ruptech.tttalk_android.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.http.HttpConnection;
import com.ruptech.tttalk_android.http.Response;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {

    private static final String APPNAME = "chinatalk";
    private final static String TAG = Utils.CATEGORY
            + ServerUtilities.class.getSimpleName();

    /**
     * Register this account/device pair within the server.
     *
     * @param userid
     * @param ifPage
     * @return
     *
     */
    private static boolean register(final String regId,
                                   long userid, String ifPage) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, "registering device (regId = " + regId + ")");
        Map<String, String> params = new HashMap<String, String>();
        params.put("task", "register");
        params.put("devicetoken", regId);
        params.put("clientid", String.valueOf(userid));
        params.put("appname", APPNAME);
        params.put("appversion", String.valueOf(App.getAppVersionCode()));
        params.put("devicename", android.os.Build.BRAND);
        params.put("devicemodel", android.os.Build.MODEL);
        params.put("deviceversion", android.os.Build.VERSION.RELEASE);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        params = HttpConnection.genParams(params);

        String serverUrl = App.getHttpServer().genRequestURL(ifPage, params);
        try {

            Response res = App.getHttpServer().get(serverUrl);
            JSONObject result = res.asJSONObject();
            if (result.getBoolean("success")) {
                return true;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, serverUrl);
        }

        return false;
    }



    /**
     * Unregister this account/device pair within the server.
     *
     * @param ifPage
     * @return
     */
    private static boolean unregister(final String regId,
                                     String ifPage) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, "unregistering device (regId = " + regId + ")");
        Map<String, String> params = new HashMap<String, String>();
        params.put("task", "unregister");
        params.put("devicetoken", regId);
        params = HttpConnection.genParams(params);

        String serverUrl = App.getHttpServer().genRequestURL(ifPage, params);
        try {

            Response res = App.getHttpServer().get(serverUrl);
            JSONObject result = res.asJSONObject();
            return result.getBoolean("success");
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, serverUrl);
            return false;
        }
    }

    public static void registerOpenfirePushOnServer(final String token) {
        if (App.readUser() != null
                && App.readUser().getTTTalkId() > 0
                && !Utils.isEmpty(token)) {
            final long userid = App.readUser().getTTTalkId();
            AsyncTask<Void, Void, Void> mRegisterTask;
            mRegisterTask = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    String ifPage = "openfire_devices.php";
                    register(token, userid, ifPage);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    Toast.makeText(App.mContext,
                            R.string.start_receiving_messages,
                            Toast.LENGTH_SHORT).show();
                }

            };
            mRegisterTask.execute();
        }
    }

    public static void unregisterOpenfirePushOnServer() {
        AsyncTask<Void, Void, Void> mRegisterTask;
        mRegisterTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                String ifPage = "openfire_devices.php";
                unregister(String.valueOf(App.readUser().getTTTalkId()), ifPage);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.makeText(App.mContext,
                        R.string.stop_receiving_messages,
                        Toast.LENGTH_SHORT).show();
            }

        };
        mRegisterTask.execute();
    }
}