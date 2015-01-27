package com.ruptech.tttalk_android.http;


import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.model.ServerAppInfo;

import org.json.JSONObject;

public class HttpServer extends HttpConnection {

    private final String TAG = HttpServer.class.getSimpleName();


    public ServerAppInfo ver() throws Exception {
        String url = _url("ver.php");
        try {
            Response res = get(url);
            JSONObject verInfo = res.asJSONObject();
            ServerAppInfo info = ServerAppInfo.parse(verInfo);
            return info;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            throw e;
        }
    }

    private String _url(String s) {
        return String.format("%s%s", App.properties.getProperty("server.url") , s);
    }

}
