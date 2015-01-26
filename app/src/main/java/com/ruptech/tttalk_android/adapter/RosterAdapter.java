package com.ruptech.tttalk_android.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.db.RosterProvider;
import com.ruptech.tttalk_android.db.RosterProvider.RosterConstants;
import com.ruptech.tttalk_android.model.Roster;
import com.ruptech.tttalk_android.utils.StatusMode;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class RosterAdapter extends ArrayAdapter<Roster> {
    private static final String TAG = RosterAdapter.class.getName();
    // 不在线状态
    private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE
            + " != " + StatusMode.offline.ordinal();
    // 在线人数
    private static final String COUNT_AVAILABLE_MEMBERS = "SELECT COUNT() FROM "
            + RosterProvider.TABLE_ROSTER
            + " inner_query"
            + " WHERE inner_query."
            + RosterConstants.GROUP
            + " = "
            + RosterProvider.QUERY_ALIAS
            + "."
            + RosterConstants.GROUP
            + " AND inner_query." + OFFLINE_EXCLUSION;
    // 总人数
    private static final String COUNT_MEMBERS = "SELECT COUNT() FROM "
            + RosterProvider.TABLE_ROSTER + " inner_query"
            + " WHERE inner_query." + RosterConstants.GROUP + " = "
            + RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP;
    private static final String[] GROUPS_QUERY_COUNTED = new String[]{
            RosterConstants._ID,
            RosterConstants.GROUP,
            "(" + COUNT_AVAILABLE_MEMBERS + ") || '/' || (" + COUNT_MEMBERS
                    + ") AS members"};
    // 联系人查询序列
    private static final String[] ROSTER_QUERY = new String[]{
            RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS,
            RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE,};
    private static final int resource = R.layout.contact_list_item_for_buddy;
    private Context mContext;
    private ContentResolver mContentResolver;
    private LayoutInflater mInflater;

    public RosterAdapter(Context context) {
        super(context, resource);
        // TODO Auto-generated constructor stub
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mContentResolver = context.getContentResolver();

        requery();
    }

    public void requery() {
        clear();
        Cursor childCursor = mContentResolver.query(RosterProvider.CONTENT_URI,
                ROSTER_QUERY, null, null, null);
        childCursor.moveToFirst();
        while (!childCursor.isAfterLast()) {
            Roster roster = new Roster();
            roster.setJid(childCursor.getString(childCursor
                    .getColumnIndexOrThrow(RosterConstants.JID)));
            roster.setAlias(childCursor.getString(childCursor
                    .getColumnIndexOrThrow(RosterConstants.ALIAS)));
            roster.setStatus_message(childCursor.getString(childCursor
                    .getColumnIndexOrThrow(RosterConstants.STATUS_MESSAGE)));
            roster.setStatusMode(childCursor.getString(childCursor
                    .getColumnIndexOrThrow(RosterConstants.STATUS_MODE)));
            add(roster);
            childCursor.moveToNext();
        }
        childCursor.close();

        notifyDataSetChanged();

    }


    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        Roster roster = getItem(position);
        int presenceMode = Integer.parseInt(roster.getStatusMode());
        ViewHolder holder;
        if (view == null
                || view.getTag(R.mipmap.ic_launcher + presenceMode) == null) {
            Log.i(TAG, "new  child ");
            view = mInflater.inflate(
                    R.layout.contact_list_item_for_buddy, parent, false);
            holder = new ViewHolder(view);

            view.setTag(R.mipmap.ic_launcher + presenceMode, holder);
            view.setTag(R.string.app_name, R.mipmap.ic_launcher
                    + presenceMode);
        } else {
            Log.i(TAG, "get child form case");
            holder = (ViewHolder) view.getTag(R.mipmap.ic_launcher
                    + presenceMode);
        }
        holder.nickView.setText(roster.getAlias());
        holder.statusMsgView.setText(TextUtils.isEmpty(roster
                .getStatusMessage()) ? mContext.getString(R.string.status_offline) : roster.getStatusMessage());
        setViewImage(holder.onlineModeView, holder.headView, holder.statusView,
                roster.getStatusMode());

        return view;
    }

    protected void setViewImage(ImageView online, ImageView head, ImageView v,
                                String value) {
        int presenceMode = Integer.parseInt(value);
        int statusDrawable = getIconForPresenceMode(presenceMode);
        if (statusDrawable == -1) {
            v.setVisibility(View.INVISIBLE);
            head.setImageResource(R.drawable.login_default_avatar_offline);
            online.setImageDrawable(null);
            return;
        }
        head.setImageResource(R.drawable.login_default_avatar);
        online.setImageResource(R.drawable.terminal_icon_ios_online);
        v.setImageResource(statusDrawable);

    }

    private int getIconForPresenceMode(int presenceMode) {
        return StatusMode.values()[presenceMode].getDrawableId();
    }

    static class ViewHolder {
        @InjectView(R.id.icon)
        ImageView headView;
        @InjectView(R.id.contact_list_item_name)
        TextView nickView;
        @InjectView(R.id.stateicon)
        ImageView statusView;
        @InjectView(R.id.online_mode)
        ImageView onlineModeView;
        @InjectView(R.id.contact_list_item_state)
        TextView statusMsgView;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

}
