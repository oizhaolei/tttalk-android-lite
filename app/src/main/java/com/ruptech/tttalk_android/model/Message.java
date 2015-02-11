package com.ruptech.tttalk_android.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3213066030077178620L;
	public long messageid;
	public long userid;
	public long to_userid;
	protected long id;
	public String create_date;

	public String from_lang;

	public String to_lang;

	public long from_voice_id;

	public String from_content;

	public int from_content_length;

	public String to_content;

	public String status_text;

	public double fee;

	public double to_user_fee;

	public String acquire_date;
	public String translated_date;
	public int verify_status;
	public int message_status;
	public String create_id;
	public String update_id;
	public String update_date;
	public String file_path;
	public String file_type;
	public int auto_translate = 0;

	public Message() {
	}

	public Message(JSONObject json) throws JSONException {

		id = json.getLong("local_id");
		messageid = json.getLong("id");
		userid = json.getLong("userid");
		to_userid = json.optLong("to_userid", 0);
		from_lang = json.getString("from_lang");
		to_lang = json.getString("to_lang");
		from_voice_id = json.optLong("from_voice_id");
		from_content = json.getString("from_content");
		from_content_length = json.getInt("from_content_length");
		to_content = json.getString("to_content");
		status_text = json.getString("status_text");
		fee = json.getDouble("fee");
		to_user_fee = json.getDouble("to_user_fee");
		acquire_date = json.getString("acquire_date");
		translated_date = json.getString("translated_date");
		verify_status = json.getInt("verify_status");
		message_status = json.getInt("message_status");
		auto_translate = json.getInt("auto_translate");
		create_id = json.getString("create_id");
		create_date = json.getString("create_date");
		update_id = json.getString("update_id");
		update_date = json.getString("update_date");
		file_path = json.getString("file_path");
		file_type = json.getString("file_type");
	}

	public String getAcquire_date() {
		return acquire_date;
	}

	public int getAuto_translate() {
		return auto_translate;
	}

	public String getCreate_date() {
		return create_date;
	}

	public String getCreate_id() {
		return create_id;
	}

	public double getFee() {
		return fee;
	}

	public String getFile_path() {
		return file_path;
	}

	public String getFile_type() {
		return file_type;
	}

	public String getFrom_content() {
		return from_content;
	}

	public int getFrom_content_length() {
		return from_content_length;
	}

	public String getFrom_lang() {
		return from_lang;
	}

	public long getFrom_voice_id() {
		return from_voice_id;
	}

	public long getId() {
		return id;
	}

	public int getMessage_status() {
		return message_status;
	}

	public long getMessageid() {
		return messageid;
	}

	public String getStatus_text() {
		return status_text;
	}

	public String getTo_content() {
		return to_content;
	}

	public String getTo_lang() {
		return to_lang;
	}

	public double getTo_user_fee() {
		return to_user_fee;
	}

	public long getTo_userid() {
		return to_userid;
	}

	public String getTranslated_date() {
		return translated_date;
	}

	public String getUpdate_date() {
		return update_date;
	}

	public String getUpdate_id() {
		return update_id;
	}

	public long getUserid() {
		return userid;
	}

	public int getVerify_status() {
		return verify_status;
	}

	public void setAcquire_date(String acquire_date) {
		this.acquire_date = acquire_date;
	}

	public void setAuto_translate(int auto_translate) {
		this.auto_translate = auto_translate;
	}

	public void setCreate_date(String create_date) {
		this.create_date = create_date;
	}

	public void setCreate_id(String create_id) {
		this.create_id = create_id;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public void setFile_path(String file_path) {
		this.file_path = file_path;
	}

	public void setFile_type(String file_type) {
		this.file_type = file_type;
	}

	public void setFrom_content(String from_content) {
		this.from_content = from_content;
	}

	public void setFrom_content_length(int from_content_length) {
		this.from_content_length = from_content_length;
	}

	public void setFrom_lang(String from_lang) {
		this.from_lang = from_lang;
	}

	public void setFrom_voice_id(long from_voice_id) {
		this.from_voice_id = from_voice_id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setMessage_status(int message_status) {
		this.message_status = message_status;
	}

	public void setMessageid(long messageid) {
		this.messageid = messageid;
	}

	public void setStatus_text(String status_text) {
		this.status_text = status_text;
	}

	public void setTo_content(String to_content) {
		this.to_content = to_content;
	}

	public void setTo_lang(String to_lang) {
		this.to_lang = to_lang;
	}

	public void setTo_user_fee(double to_user_fee) {
		this.to_user_fee = to_user_fee;
	}

	public void setTo_userid(long to_userid) {
		this.to_userid = to_userid;
	}

	public void setTranslated_date(String translated_date) {
		this.translated_date = translated_date;
	}

	public void setUpdate_date(String update_date) {
		this.update_date = update_date;
	}

	public void setUpdate_id(String update_id) {
		this.update_id = update_id;
	}

	public void setUserid(long userid) {
		this.userid = userid;
	}

	public void setVerify_status(int verify_status) {
		this.verify_status = verify_status;
	}

	@Override
	public String toString() {
		return "Message [id=" + id + ", messageid=" + messageid + ", userid="
				+ userid + ", to_userid=" + to_userid + ", from_lang="
				+ from_lang + ", to_lang=" + to_lang + ", from_voice_id="
				+ from_voice_id + ", from_content=" + from_content
				+ ", from_content_length=" + from_content_length
				+ ", to_content=" + to_content + ", status_text=" + status_text
				+ ", fee=" + fee + ", to_user_fee=" + to_user_fee
				+ ", acquire_date=" + acquire_date + ", translated_date="
				+ translated_date + ", verify_status=" + verify_status
				+ ", message_status=" + message_status + ", create_id="
				+ create_id + ", create_date=" + create_date + ", update_id="
				+ update_id + ", update_date=" + update_date + ", file_path="
				+ file_path + ", file_type=" + file_type + ", auto_translate="
				+ auto_translate + "]";
	}

}
