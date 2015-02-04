package com.ruptech.tttalk_android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruptech.tttalk_android.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainFragment extends Fragment {

    @InjectView(android.R.id.tabhost)
    FragmentTabHost mTabHost;

    public MainFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        ButterKnife.inject(this, rootView);

        mTabHost.setup(getActivity(), getChildFragmentManager(), android.R.id.tabcontent);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_title_chats)).setIndicator(getString(R.string.tab_title_chats)),
                RecentChatFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_title_friends)).setIndicator(getString(R.string.tab_title_friends)),
                FriendListFragment.class, null);

        return rootView;
    }
}

