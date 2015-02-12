package com.ruptech.tttalk_android.adapter;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.baidutranslate.openapi.TranslateClient;
import com.baidu.baidutranslate.openapi.callback.ITransResultCallback;
import com.baidu.baidutranslate.openapi.entity.TransResult;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.ChatActivity;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.model.Chat;
import com.ruptech.tttalk_android.model.Message;
import com.ruptech.tttalk_android.task.GenericTask;
import com.ruptech.tttalk_android.task.TaskAdapter;
import com.ruptech.tttalk_android.task.TaskListener;
import com.ruptech.tttalk_android.task.TaskResult;
import com.ruptech.tttalk_android.task.impl.RequestTranslateTask;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.TimeUtil;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.Collection;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChatAdapter extends SimpleCursorAdapter {
    private static final String TAG = ChatAdapter.class.getName();

    private static final int DELAY_NEWMSG = 2000;
    private final TranslateClient mClient;
    private ChatActivity mContext;
    private LayoutInflater mInflater;
    private ContentResolver mContentResolver;

    private final TaskListener mRequestTranslateListener = new TaskAdapter() {

        @Override
        public void onPostExecute(GenericTask task, TaskResult result) {
            RequestTranslateTask fsTask = (RequestTranslateTask) task;
            if (result == TaskResult.OK) {
                Message message = fsTask.getMessage();
                setMessageID(fsTask.getChat().getPid(), message.getMessageid(), message.getTo_content());
                Log.d(TAG, "Request translate Success");
            } else {
                String msg = fsTask.getMsg();
                Log.d(TAG, "Request translate fail:" + msg);
            }
        }

        @Override
        public void onPreExecute(GenericTask task) {

        }

    };

    public void setMessageID(String packetID, long messageID, String to_content) {
        ContentValues cv = new ContentValues();
        cv.put(ChatConstants.MESSAGE_ID, messageID);
        if (to_content == null || to_content.length() == 0)
            cv.put(ChatConstants.TO_MESSAGE, "Translating...");
        else
            cv.put(ChatConstants.TO_MESSAGE, to_content);
        Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                + ChatProvider.TABLE_NAME);
        mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID
                + " = ? AND " + ChatConstants.DIRECTION + " = "
                + ChatConstants.INCOMING, new String[]{packetID});
    }

    public ChatAdapter(ChatActivity context, Cursor cursor, String[] from, TranslateClient client) {
        // super(context, android.R.layout.simple_list_item_1, cursor, from,
        // to);
        super(context, 0, cursor, from, null);
        mContext = context;
        mClient = client;
        mInflater = LayoutInflater.from(context);
        mContentResolver = context.getContentResolver();
    }

    public void baiduTranslate(String content, String fromLang, String toLang) {

        if (TextUtils.isEmpty(content))
            return;

        mClient.translate(content, fromLang, toLang, new ITransResultCallback() {

            @Override
            public void onResult(TransResult result) {// 翻译结果回调
                if (result == null) {
                    Log.d(TAG, "Trans Result is null");

                } else {
                    Log.d(TAG, result.toJSONString());

                    String msg;
                    if (result.error_code == 0) {// 没错
                        msg = result.trans_result;
                    } else {
                        msg = result.error_msg;
                    }
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = this.getCursor();
        cursor.moveToPosition(position);

        Chat chat = ChatProvider.parseChat(cursor);

        boolean from_me = (chat.getFromMe() == ChatConstants.OUTGOING);
        int come = chat.getFromMe();

        ViewHolder viewHolder;
        if (convertView == null
                || convertView.getTag(R.mipmap.ic_launcher + come) == null) {
            if (come == ChatConstants.OUTGOING) {
                convertView = mInflater.inflate(R.layout.chat_item_right,
                        parent, false);
            } else {
                convertView = mInflater.inflate(R.layout.chat_item_left, null);
            }
            viewHolder = buildHolder(convertView);
            convertView.setTag(R.mipmap.ic_launcher + come, viewHolder);
            convertView
                    .setTag(R.string.app_name, R.mipmap.ic_launcher + come);
        } else {
            viewHolder = (ViewHolder) convertView.getTag(R.mipmap.ic_launcher
                    + come);
        }

        if (!from_me && chat.getRead() == ChatConstants.DS_NEW) {
            markAsReadDelayed(chat.getId(), DELAY_NEWMSG);
        }

        viewHolder.avatar.setBackgroundResource(R.drawable.default_portrait);
        if (from_me
                && !PrefUtils.getPrefBoolean(
                PrefUtils.SHOW_MY_HEAD, true)) {
            viewHolder.avatar.setVisibility(View.GONE);
        }
        CharSequence text = XMPPUtils.convertNormalStringToSpannableString(
                mContext, chat.getMessage(), false);
        viewHolder.content.setText(text);
        if (chat.getTo_content() != null) {
            CharSequence to_content = XMPPUtils.convertNormalStringToSpannableString(
                    mContext, chat.getTo_content(), false);
            viewHolder.toContent.setText(to_content);
            viewHolder.toContent.setVisibility(View.VISIBLE);
        }else{
            viewHolder.toContent.setVisibility(View.GONE);
        }


        viewHolder.contentLayout.setTag(chat);

        String date = TimeUtil.getChatTime(chat.getDate());
        viewHolder.time.setText(date);
        return convertView;
    }

    private void markAsReadDelayed(final int id, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                markAsRead(id);
            }
        }, delay);
    }

    /**
     * 标记为已读消息
     *
     * @param id
     */
    private void markAsRead(int id) {
        Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                + ChatProvider.TABLE_NAME + "/" + id);
        Log.d(TAG, "markAsRead: " + rowuri);
        ContentValues values = new ContentValues();
        values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
        mContext.getContentResolver().update(rowuri, values, null, null);
    }


    private ViewHolder buildHolder(View convertView) {
        final ViewHolder holder = new ViewHolder(convertView);
        holder.contentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Chat chat = (Chat) holder.contentLayout.getTag();
                        switch (which) {
                            case 0://R.string.auto_translate:
                                baiduTranslate(chat.getMessage(), "zh", "en");
                                break;
                            case 1://R.string.human_translate:
                                requestTTTalkTranslate(chat, "CN", "EN");
                                break;
                            default:
                                break;
                        }
                    }
                };
                builder.setItems(R.array.chat_action, positiveListener);
                builder.create().show();
            }
        });

        return holder;
    }

    private void requestTTTalkTranslate(Chat chat, String fromLang, String toLang) {
        Collection<PacketExtension> extensions = new ArrayList<>();

        fromLang = "CN";
        toLang = "KR";

        String callback_id = chat.getPid();
        requestTranslate(chat, fromLang, toLang);
    }

    public void requestTranslate(Chat chat, String from_lang, String to_lang) {
        RequestTranslateTask mRequestTranslateTask = new RequestTranslateTask(chat, from_lang, to_lang);
        mRequestTranslateTask.setListener(mRequestTranslateListener);
        mRequestTranslateTask.execute();
    }

    static class ViewHolder {
        @InjectView(R.id.content_view)
        View contentLayout;
        @InjectView(R.id.content_textView)
        TextView content;
        @InjectView(R.id.to_content_textView)
        TextView toContent;
        @InjectView(R.id.datetime)
        TextView time;
        @InjectView(R.id.icon)
        ImageView avatar;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

    }

}
