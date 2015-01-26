package com.ruptech.tttalk_android.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.ChatActivity;
import com.ruptech.tttalk_android.adapter.RosterAdapter;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.model.Roster;

public class FriendListFragment extends ListFragment {
    private static final String TAG = FriendListFragment.class.getName();

    private Handler mainHandler = new Handler();

    private ContentObserver mRosterObserver = new RosterObserver();
    private ContentResolver mContentResolver;
    private RosterAdapter rosterAdapter;

    public FriendListFragment() {
        super();
    }

    public static Fragment newInstance() {
        return new FriendListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContentResolver = getActivity().getContentResolver();
        rosterAdapter = new RosterAdapter(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_roster, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
        rosterAdapter.requery();
        mContentResolver.registerContentObserver(ChatProvider.CONTENT_URI,
                true, mRosterObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentResolver.unregisterContentObserver(mRosterObserver);
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

        Roster roster = rosterAdapter.getItem(position);
        String userJid = roster.getJid();
        String userName = roster.getAlias();

        startChatActivity(userJid, userName);
    }

    private void initView() {
        setListAdapter(rosterAdapter);

    }

    public void updateRoster() {
        rosterAdapter.requery();
    }

    private class RosterObserver extends ContentObserver {
        public RosterObserver() {
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
