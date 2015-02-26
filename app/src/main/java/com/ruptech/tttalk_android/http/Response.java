package com.ruptech.tttalk_android.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Response {
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

	public String getBody() {
		return body;
	}
}
