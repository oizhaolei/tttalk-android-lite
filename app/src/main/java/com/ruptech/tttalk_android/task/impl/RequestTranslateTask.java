package com.ruptech.tttalk_android.task.impl;

import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.model.Chat;
import com.ruptech.tttalk_android.model.Message;
import com.ruptech.tttalk_android.task.GenericTask;
import com.ruptech.tttalk_android.task.TaskResult;
import com.ruptech.tttalk_android.utils.AppPreferences;
import com.ruptech.tttalk_android.utils.DateCommonUtils;

import java.util.Date;

public class RequestTranslateTask extends GenericTask {
	private Message message;
    private Chat chat;

	private boolean existTranslatedMessage;

    public RequestTranslateTask(Chat chat, String from_lang, String to_lang) {
        Message message = new Message();
        long localId = System.currentTimeMillis();
        message.setId(localId);
        message.setMessageid(localId);
        message.setUserid(App.readUser().getTTTalkId());
        message.setTo_userid(0);
        message.setFrom_lang(from_lang);
        message.setTo_lang(to_lang);
        message.setFrom_content(chat.getMessage());
        message.setFrom_content_length(chat.getMessage().length());
        message.setMessage_status(AppPreferences.MESSAGE_STATUS_BEFORE_SEND);
        message.setStatus_text("sending");
        message.setFile_path(null);
        String filetype = AppPreferences.MESSAGE_TYPE_NAME_TEXT;
        message.setFile_type(filetype);
        String createDateStr = DateCommonUtils.getUtcDate(new Date(),
                DateCommonUtils.DF_yyyyMMddHHmmssSSS);
        message.create_date = createDateStr;
        message.update_date = createDateStr;
        this.message = message;
        this.chat = chat;

    }

	@Override
	protected TaskResult _doInBackground() throws Exception {
		if (BuildConfig.DEBUG)
			Log.v(TAG, "RequestTranslateTask");

		Long localId = message.getId();
		Long toUserId = message.to_userid;
		String text = message.getFrom_content();
		String fromLang = message.from_lang;
		String toLang = message.to_lang;
		int contentLength = message.getFrom_content_length();
		String filetype = message.file_type;
		String filePath = message.getFile_path();
        String lastUpdatedate = DateCommonUtils.getUtcDate(new Date(),
                DateCommonUtils.DF_yyyyMMddHHmmss);

        message = App.getHttpServer().requestTranslate(localId,
				toUserId, fromLang, toLang, text, contentLength,
				filetype, lastUpdatedate, filePath);

		return TaskResult.OK;
	}


	public boolean getIsNeedRetrieveUser() {
		return existTranslatedMessage;
	}

	public Message getMessage() {
		return message;
	}
    public Chat getChat() {
        return chat;
    }
	@Override
	public Object[] getMsgs() {
		return new Object[] {message};
	}
}
