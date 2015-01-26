package com.ruptech.tttalk_android.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.XXBroadcastReceiver;
import com.ruptech.tttalk_android.fragment.FriendListFragment;
import com.ruptech.tttalk_android.fragment.RecentChatFragment;
import com.ruptech.tttalk_android.service.IConnectionStatusCallback;
import com.ruptech.tttalk_android.service.TTTalkService;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.XMPPUtils;
import com.ruptech.tttalk_android.view.AddRosterItemDialog;
import com.ruptech.tttalk_android.view.PagerItem;
import com.ruptech.tttalk_android.view.ViewPagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;

public class MainActivity extends ActionBarActivity implements MaterialTabListener,
        IConnectionStatusCallback, XXBroadcastReceiver.EventHandler {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static HashMap<String, Integer> mStatusMap;

    static {
        mStatusMap = new HashMap<String, Integer>();
        mStatusMap.put(PrefUtils.OFFLINE, -1);
        mStatusMap.put(PrefUtils.DND, R.drawable.status_shield);
        mStatusMap.put(PrefUtils.XA, R.drawable.status_invisible);
        mStatusMap.put(PrefUtils.AWAY, R.drawable.status_leave);
        mStatusMap.put(PrefUtils.AVAILABLE, R.drawable.status_online);
        mStatusMap.put(PrefUtils.CHAT, R.drawable.status_qme);
    }

    public static MainActivity instance = null;
    @InjectView(R.id.tabHost)
    MaterialTabHost tabHost;
    @InjectView(R.id.pager)
    ViewPager pager;
    long back_pressed;
    private TTTalkService mService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TTTalkService.XXBinder) service).getService();
            mService.registerConnectionStatusCallback(MainActivity.this);
            // 开始连接xmpp服务器
            if (!mService.isAuthenticated()) {
                String usr = PrefUtils.getPrefString(
                        PrefUtils.ACCOUNT, "");
                String password = PrefUtils.getPrefString(PrefUtils.PASSWORD, "");
                mService.login(usr, password);
                getSupportActionBar().setTitle(R.string.login_prompt_msg);
                // setStatusImage(false);
                // mTitleProgressBar.setVisibility(View.VISIBLE);
            } else {
                getSupportActionBar().setTitle(XMPPUtils
                        .splitJidAndServer(PrefUtils.getPrefString(PrefUtils.ACCOUNT,
                                "")));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.unRegisterConnectionStatusCallback();
            mService = null;
        }

    };
    private Handler mainHandler = new Handler();

    public static void close() {
        if (instance != null) {
            instance.finish();
            instance = null;
        }
    }

    @Override
    public void onNetChange() {
        getSupportActionBar().setTitle(R.string.net_error_tip);
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        switch (connectedState) {
            case TTTalkService.CONNECTED:
                getSupportActionBar().setTitle(XMPPUtils.splitJidAndServer(PrefUtils
                        .getPrefString(
                                PrefUtils.ACCOUNT, "")));
                break;
            case TTTalkService.CONNECTING:
                getSupportActionBar().setTitle(R.string.login_prompt_msg);
                break;
            case TTTalkService.DISCONNECTED:
                getSupportActionBar().setTitle(R.string.login_prompt_no);
                break;

            default:
                getSupportActionBar().setTitle("?");
                break;
        }
    }


    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            Log.i(TAG, "unbindXMPPService");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
        XXBroadcastReceiver.mListeners.add(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindXMPPService();
        XXBroadcastReceiver.mListeners.remove(this);
    }

    private void bindXMPPService() {
        Log.i(TAG, "bindXMPPService");
        Intent serviceIntent = new Intent(MainActivity.this, TTTalkService.class);
        bindService(serviceIntent,
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    @Override
    public void onBackPressed() {

        if (back_pressed + 2000 > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.confirm_exit), Toast.LENGTH_SHORT)
                    .show();
            back_pressed = System.currentTimeMillis();
        }
    }

    protected List<PagerItem> setupTabs() {
        List<PagerItem> mTabs = new ArrayList<PagerItem>();

        /**
         * Populate our tab list with tabs. Each item contains a title, indicator color and divider
         * color, which are used by {@link com.ruptech.tttalk_android.view.SlidingTabLayout}.
         */
        mTabs.add(new PagerItem(
                getString(R.string.tab_title_chats)
        ) {
            public Fragment createFragment() {
                return RecentChatFragment.newInstance();
            }
        });

        mTabs.add(new PagerItem(
                getString(R.string.tab_title_friends) // Title
        ) {
            public Fragment createFragment() {
                return FriendListFragment.newInstance();
            }
        });
        return mTabs;
    }

    @Override
    public void onTabSelected(MaterialTab tab) {
        pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(MaterialTab tab) {
    }

    @Override
    public void onTabUnselected(MaterialTab tab) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        instance = this;


        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        // init view pager
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), setupTabs());
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // when user do a swipe the selected tab change
                tabHost.setSelectedNavigationItem(position);

            }
        });
        for (int i = 0; i < adapter.getCount(); i++) {
            tabHost.addTab(
                    tabHost.newTab()
                            .setText(adapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                gotoSettingActivity();
                return true;
            case R.id.action_add_roster:
                new AddRosterItemDialog(this,
                        mService).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void gotoSettingActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
