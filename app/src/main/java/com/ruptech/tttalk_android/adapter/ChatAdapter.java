package com.ruptech.tttalk_android.adapter;

import android.content.ContentValues;
import android.content.Context;
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
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.TimeUtil;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChatAdapter extends SimpleCursorAdapter {
    private static final String TAG = ChatAdapter.class.getName();

    private static final int DELAY_NEWMSG = 2000;
    private final TranslateClient mClient;
    private Context mContext;
    private LayoutInflater mInflater;

    public ChatAdapter(Context context, Cursor cursor, String[] from, TranslateClient client) {
        // super(context, android.R.layout.simple_list_item_1, cursor, from,
        // to);
        super(context, 0, cursor, from, null);
        mContext = context;
        mClient = client;
        mInflater = LayoutInflater.from(context);
    }

    public void translate(String content, String fromLang, String toLang) {

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

        long dateMilliseconds = cursor.getLong(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DATE));

        int _id = cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants._ID));
        String date = TimeUtil.getChatTime(dateMilliseconds);
        String message = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
        int come = cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
        boolean from_me = (come == ChatConstants.OUTGOING);
        String jid = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.JID));
        int delivery_status = cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));
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

        if (!from_me && delivery_status == ChatConstants.DS_NEW) {
            markAsReadDelayed(_id, DELAY_NEWMSG);
        }

        bindViewData(viewHolder, date, from_me, jid, message, delivery_status);
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

    private void bindViewData(ViewHolder holder, String date, boolean from_me,
                              String from, String message, int delivery_status) {
        holder.avatar.setBackgroundResource(R.drawable.default_portrait);
        if (from_me
                && !PrefUtils.getPrefBoolean(
                PrefUtils.SHOW_MY_HEAD, true)) {
            holder.avatar.setVisibility(View.GONE);
        }
        CharSequence text = XMPPUtils.convertNormalStringToSpannableString(
                mContext, message, false);
        holder.content.setText(text);
        holder.contentMenu.setTag(text);
        holder.time.setText(date);
    }

    private ViewHolder buildHolder(View convertView) {
        final ViewHolder holder = new ViewHolder(convertView);
        holder.contentMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = (String) holder.contentMenu.getTag();
                translate(content, "zh", "en");
            }
        });

        return holder;
    }

    static class ViewHolder {
        @InjectView(R.id.more)
        View contentMenu;
        @InjectView(R.id.textView2)
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
