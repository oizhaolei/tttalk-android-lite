package com.ruptech.tttalk_android.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.utils.TimeUtil;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class RecentChatAdapter extends SimpleCursorAdapter {
    private static final String SELECT = ChatConstants.DATE
            + " in (select max(" + ChatConstants.DATE + ") from "
            + ChatProvider.TABLE_NAME + " group by " + ChatConstants.JID
            + " having count(*)>0)";// 查询合并重复jid字段的所有聊天对象
    private static final String[] FROM = new String[]{
            ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
            ChatProvider.ChatConstants.DIRECTION,
            ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
            ChatProvider.ChatConstants.DELIVERY_STATUS};// 查询字段
    private static final String SORT_ORDER = ChatConstants.DATE + " DESC";
    private ContentResolver mContentResolver;
    private LayoutInflater mLayoutInflater;
    private Activity mContext;

    public RecentChatAdapter(Activity context) {
        super(context, 0, null, FROM, null);
        mContext = context;
        mContentResolver = context.getContentResolver();
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void requery() {
        Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI, FROM,
                SELECT, null, SORT_ORDER);
        Cursor oldCursor = getCursor();
        changeCursor(cursor);
        mContext.stopManagingCursor(oldCursor);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = this.getCursor();
        cursor.moveToPosition(position);
        long dateMilliseconds = cursor.getLong(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DATE));
        String date = TimeUtil.getChatTime(dateMilliseconds);
        String message = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
        String jid = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.JID));

        String selection = ChatConstants.JID + " = '" + jid + "' AND "
                + ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING
                + " AND " + ChatConstants.DELIVERY_STATUS + " = "
                + ChatConstants.DS_NEW;// 新消息数量字段
        Cursor msgcursor = mContentResolver.query(ChatProvider.CONTENT_URI,
                new String[]{"count(" + ChatConstants.PACKET_ID + ")",
                        ChatConstants.DATE, ChatConstants.MESSAGE}, selection,
                null, SORT_ORDER);
        msgcursor.moveToFirst();
        int count = msgcursor.getInt(0);
        ViewHolder viewHolder;
        if (convertView == null
                || convertView.getTag(R.mipmap.ic_launcher
                + (int) dateMilliseconds) == null) {
            convertView = mLayoutInflater.inflate(
                    R.layout.item_recent_chat, parent, false);
            viewHolder = new ViewHolder(convertView);

            convertView.setTag(R.mipmap.ic_launcher + (int) dateMilliseconds,
                    viewHolder);
            convertView.setTag(R.string.app_name, R.mipmap.ic_launcher
                    + (int) dateMilliseconds);
        } else {
            viewHolder = (ViewHolder) convertView.getTag(R.mipmap.ic_launcher
                    + (int) dateMilliseconds);
        }
        viewHolder.jidView.setText(XMPPUtils.splitJidAndServer(jid));
        viewHolder.msgView.setText(XMPPUtils
                .convertNormalStringToSpannableString(mContext, message, true));
        viewHolder.dataView.setText(date);

        if (msgcursor.getInt(0) > 0) {
            viewHolder.msgView.setText(msgcursor.getString(msgcursor
                    .getColumnIndex(ChatConstants.MESSAGE)));
            viewHolder.dataView.setText(TimeUtil.getChatTime(msgcursor
                    .getLong(msgcursor.getColumnIndex(ChatConstants.DATE))));
            viewHolder.unReadView.setText(msgcursor.getString(0));
        }
        viewHolder.unReadView.setVisibility(count > 0 ? View.VISIBLE
                : View.GONE);
        viewHolder.unReadView.bringToFront();
        msgcursor.close();

        return convertView;
    }


    static class ViewHolder {
        @InjectView(R.id.recent_list_item_name)
        TextView jidView;
        @InjectView(R.id.recent_list_item_time)
        TextView dataView;
        @InjectView(R.id.recent_list_item_msg)
        TextView msgView;
        @InjectView(R.id.unreadmsg)
        TextView unReadView;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
