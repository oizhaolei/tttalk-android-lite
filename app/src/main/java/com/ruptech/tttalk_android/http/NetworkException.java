package com.ruptech.tttalk_android.http;

public class NetworkException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2163997882396308558L;

	/**
	 * 接收服务器端返回的错误消息
	 */
	public NetworkException(String msg) {
		super(msg);
	}

}
