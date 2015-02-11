package com.ruptech.tttalk_android.task.impl;

import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.model.Message;
import com.ruptech.tttalk_android.task.GenericTask;
import com.ruptech.tttalk_android.task.TaskResult;
import com.ruptech.tttalk_android.utils.DateCommonUtils;

import java.util.Date;

public class RequestTranslateTask extends GenericTask {
	private Message message;
	private boolean existTranslatedMessage;

	public RequestTranslateTask(Message message) {
		this.message = message;
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

	@Override
	public Object[] getMsgs() {
		return new Object[] {message};
	}
}
