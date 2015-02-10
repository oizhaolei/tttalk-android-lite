package com.ruptech.tttalk_android.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.ruptech.tttalk_android.activity.ChatActivity;
import com.ruptech.tttalk_android.adapter.RecentChatAdapter;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.utils.XMPPUtils;

public class RecentChatFragment extends ListFragment {
    private static final String TAG = RecentChatFragment.class.getName();

    private Handler mainHandler = new Handler();

    private ContentObserver mChatObserver = new ChatObserver();
    private ContentResolver mContentResolver;
    private RecentChatAdapter mRecentChatAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentResolver = getActivity().getContentResolver();
        mRecentChatAdapter = new RecentChatAdapter(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecentChatAdapter.requery();
        mContentResolver.registerContentObserver(ChatProvider.CONTENT_URI,
                true, mChatObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentResolver.unregisterContentObserver(mChatObserver);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void startChatActivity(String userJid, String userName) {
        Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
        Uri userNameUri = Uri.parse(userJid);
        chatIntent.setData(userNameUri);
        chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
        startActivity(chatIntent);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor clickCursor = mRecentChatAdapter.getCursor();
        clickCursor.moveToPosition(position);
        String jid = clickCursor.getString(clickCursor
                .getColumnIndex(ChatConstants.JID));

        startChatActivity(jid, XMPPUtils.splitJidAndServer(jid));
    }

    private void initView() {
        setListAdapter(mRecentChatAdapter);

    }

    public void updateRoster() {
        mRecentChatAdapter.requery();
    }

    private class ChatObserver extends ContentObserver {
        public ChatObserver() {
            super(mainHandler);
        }

        public void onChange(boolean selfChange) {
            mainHandler.postDelayed(new Runnable() {
                public void run() {
                    updateRoster();
                }
            }, 100);
        }
    }

}
