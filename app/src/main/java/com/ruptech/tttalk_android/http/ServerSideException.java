package com.ruptech.tttalk_android.http;

public class ServerSideException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4717967550586117656L;
	/**
	 * 接收服务器端返回的错误消息
	 */
	public ServerSideException(String msg) {
		super(msg);
	}

}
