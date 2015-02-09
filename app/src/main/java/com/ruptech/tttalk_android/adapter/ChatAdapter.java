package com.ruptech.tttalk_android.adapter;

import android.app.AlertDialog;
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
import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.ChatActivity;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.model.Chat;
import com.ruptech.tttalk_android.smack.FromLang;
import com.ruptech.tttalk_android.smack.OriginId;
import com.ruptech.tttalk_android.smack.TTTalkExtension;
import com.ruptech.tttalk_android.smack.ToLang;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.TimeUtil;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChatAdapter extends SimpleCursorAdapter {
    private static final String TAG = ChatAdapter.class.getName();

    private static final int DELAY_NEWMSG = 2000;
    private final TranslateClient mClient;
    private ChatActivity mContext;
    private LayoutInflater mInflater;

    public ChatAdapter(ChatActivity context, Cursor cursor, String[] from, TranslateClient client) {
        // super(context, android.R.layout.simple_list_item_1, cursor, from,
        // to);
        super(context, 0, cursor, from, null);
        mContext = context;
        mClient = client;
        mInflater = LayoutInflater.from(context);
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

        Chat chat = new Chat();
        chat.setDate(cursor.getLong(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DATE)));

        chat.setId(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants._ID)));
        chat.setMessage(cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.MESSAGE)));
        chat.setFromMe(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DIRECTION)));// 消息来自
        chat.setJid(cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.JID)));
        chat.setPid(cursor.getString(cursor
                .getColumnIndex(ChatConstants.PACKET_ID)));
        chat.setRead(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS)));

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

        bindViewData(viewHolder, from_me, chat);
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

    private void bindViewData(ViewHolder holder, boolean from_me, Chat chat) {
        holder.avatar.setBackgroundResource(R.drawable.default_portrait);
        if (from_me
                && !PrefUtils.getPrefBoolean(
                PrefUtils.SHOW_MY_HEAD, true)) {
            holder.avatar.setVisibility(View.GONE);
        }
        CharSequence text = XMPPUtils.convertNormalStringToSpannableString(
                mContext, chat.getMessage(), false);
        holder.content.setText(text);
        holder.contentLayout.setTag(chat);

        String date = TimeUtil.getChatTime(chat.getDate());
        holder.time.setText(date);
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
        Map<String, String> map = new HashMap <String, String>();
//        map.put("key", "1111111");
//        map.put("secret", "2222222");
//        map.put("test", "true");
//        map.put("ver", "97");
        map.put("fromlang", fromLang);
        map.put("tolang", toLang);
        map.put("original_id", callback_id);

        TTTalkExtension tttalk = new TTTalkExtension(map);
        Log.i("test", tttalk.toXML());
        extensions.add(tttalk);

        mContext.getService().sendMessage("tttalk.translator@tttalk.org", chat.getMessage(), extensions);
    }

    static class ViewHolder {
        @InjectView(R.id.content_view)
        View contentLayout;
        @InjectView(R.id.content_textView)
        TextView content;
        @InjectView(R.id.datetime)
        TextView time;
        @InjectView(R.id.icon)
        ImageView avatar;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }

    }

}
