package com.ruptech.tttalk_android.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class ServerAppInfo implements Serializable {
    private static final long serialVersionUID = -311197799928729601L;
    public String apkname = "";
    public String appname = "";
    public int verCode = 0;
    public int fileSize = 0;
    public String verName = "";


    public static ServerAppInfo parse(JSONObject verInfo) throws JSONException {
        ServerAppInfo info = new ServerAppInfo();

        info.appname = verInfo.getString("appname");
        info.apkname = verInfo.getString("apkname");
        info.verName = verInfo.getString("verName");
        info.verCode = verInfo.getInt("verCode");
        info.fileSize = verInfo.getInt("fileSize");

        // server
        JSONObject serverInfo = verInfo.getJSONObject("server");
        info.appServerUrl = serverInfo.getString("appServerUrl");

        return info;
    }


    public String appServerUrl;

    public String getAppServerUrl() {
        return appServerUrl;
    }

    public String getApkUrl() {
        return String.format("%s%s", appServerUrl, apkname);
    }
}
