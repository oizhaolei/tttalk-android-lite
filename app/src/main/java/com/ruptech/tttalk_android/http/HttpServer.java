package com.ruptech.tttalk_android.http;


import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.model.Message;
import com.ruptech.tttalk_android.model.ServerAppInfo;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    public Message requestTranslate(long localId, long toUserId,
                                          String fromLang, String toLang, String text, int contentLength,
                                          String filetype, String lastUpdatedate, String filePath)
            throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("local_id", String.valueOf(localId));
        params.put("to_userid", String.valueOf(toUserId));
        params.put("from_lang", fromLang);
        params.put("to_lang", toLang);
        if (text == null)
            text = "";
        params.put("text", text);
        params.put("content_length", String.valueOf(contentLength));
        params.put("filetype", filetype);
        params.put("update_date", lastUpdatedate);
        params.put("file_path", filePath);
        if (BuildConfig.DEBUG)
            Log.v(TAG, "params:" + params);

        Response res = _get("xmpp_translate.php", params);
        JSONObject result = res.asJSONObject();

        boolean success = result.getBoolean("success");
        if (success) {
            JSONObject jo = result.getJSONObject("data");
            Message message = new Message(jo);
            return message;
        }
        String msg = result.getString("msg");
        throw new ServerSideException(msg);

    }



    private String _url(String s) {
        return String.format("%s%s", App.properties.getProperty("xmpp.server.url"), s);
    }

}
