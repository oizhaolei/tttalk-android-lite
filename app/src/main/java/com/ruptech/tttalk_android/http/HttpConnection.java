package com.ruptech.tttalk_android.http;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class HttpConnection {
    private final String TAG = HttpConnection.class.getSimpleName();

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0 || s.equals("null");
    }

    private static String encodeParameters(Map<String, String> params)
            throws RuntimeException {
        StringBuffer buf = new StringBuffer();
        String[] keyArray = params.keySet().toArray(new String[params.size()]);
        Arrays.sort(keyArray);
        int j = 0;
        for (String key : keyArray) {
            String value = params.get(key);
            if (j++ != 0) {
                buf.append("&");
            }
            if (!isEmpty(value)) {
                try {
                    buf.append(URLEncoder.encode(key, "UTF-8")).append("=")
                            .append(URLEncoder.encode(value, "UTF-8"));
                } catch (java.io.UnsupportedEncodingException neverHappen) {
                    // throw new RuntimeException(neverHappen.getMessage(),
                    // neverHappen);
                }
            }
        }

        return buf.toString();
    }

    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<>();

        if (json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static Map toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public Response get(String url) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, url);
        }
        String body = HttpRequest.get(url).body();

        return new Response(body);
    }

    public Response post(String url, Map<String, String> postParams) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, url + ", " + postParams);
        }

        String body = HttpRequest.post(url).form(postParams).body();

        return new Response(body);
    }

    /**
     * Returns the base URL
     *
     * @param ifPage 业务接口名
     * @return the base URL
     */

    public String genRequestURL(String ifPage, Map<String, String> params) {
        //
        if (params == null) {
            params = new HashMap<>();
        }
        if (App.readUser() != null) {
            String APP_SOURCE = "an-";
            String source = APP_SOURCE + App.getAppVersionCode();
            params.put("source", source);
        }
        String url = ifPage;
        if (!url.startsWith("http")) {
            url = App.properties.getProperty("server.url") + url;
        }
        url += "?" + encodeParameters(params);
        return url;
    }

    /**
     * Issues an HTTP GET request.
     *
     * @return the response
     */
    protected Response _get(String ifPage, Map<String, String> params) throws InterruptedException {
        String url = genRequestURL(ifPage, params);
        Response response = get(url);
        return response;
    }

    protected Response _post(String ifPage, Map<String, String> params) {
        String url = genRequestURL(ifPage, null);
        Response response = post(url, params);
        return response;
    }

    class Response {
        private final String TAG = Response.class.getSimpleName();

        private String body;

        public Response(String body) {
            this.body = body;
        }

        public JSONArray asJSONArray() throws RuntimeException {
            try {
                return new JSONArray(body);
            } catch (Exception jsone) {
                throw new RuntimeException(jsone.getMessage(), jsone);
            }
        }

        public JSONObject asJSONObject() throws Exception {
            try {
                return new JSONObject(body);
            } catch (JSONException jsone) {
                throw new Exception(jsone.getMessage() + ":" + body, jsone);
            }
        }
    }

}
