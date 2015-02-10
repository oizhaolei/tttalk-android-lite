package com.ruptech.tttalk_android.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.XXBroadcastReceiver;
import com.ruptech.tttalk_android.adapter.SlidingMenuAdapter;
import com.ruptech.tttalk_android.bus.ConnectionStatusChangedEvent;
import com.ruptech.tttalk_android.bus.LogoutEvent;
import com.ruptech.tttalk_android.bus.NetChangeEvent;
import com.ruptech.tttalk_android.fragment.MainFragment;
import com.ruptech.tttalk_android.fragment.SettingsFragment;
import com.ruptech.tttalk_android.service.TTTalkService;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.XMPPUtils;
import com.ruptech.tttalk_android.view.AddRosterItemDialog;
import com.squareup.otto.Subscribe;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends ActionBarActivity implements ListView.OnItemClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static HashMap<String, Integer> mStatusMap;
    static {
        mStatusMap = new HashMap<>();
        mStatusMap.put(PrefUtils.OFFLINE, -1);
        mStatusMap.put(PrefUtils.DND, R.drawable.status_shield);
        mStatusMap.put(PrefUtils.XA, R.drawable.status_invisible);
        mStatusMap.put(PrefUtils.AWAY, R.drawable.status_leave);
        mStatusMap.put(PrefUtils.AVAILABLE, R.drawable.status_online);
        mStatusMap.put(PrefUtils.CHAT, R.drawable.status_qme);
    }
    long back_pressed;
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @InjectView(R.id.left_drawer)
    ListView mDrawerList;
    private TTTalkService mService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TTTalkService.XXBinder) service).getService();
            // 开始连接xmpp服务器
            if (!mService.isAuthenticated()) {
                String account = App.readUser().getAccount();
                String password = App.readUser().getPassword();

                mService.login(account, password);
                getSupportActionBar().setTitle(R.string.login_prompt_msg);
                // setStatusImage(false);
                // mTitleProgressBar.setVisibility(View.VISIBLE);
            } else {
                getSupportActionBar().setTitle(XMPPUtils
                        .splitJidAndServer(App.readUser().getAccount()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

    };
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mSlidingMenuTitles;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
    }

    @Subscribe
    public void answerNetChange(NetChangeEvent event) {
        getSupportActionBar().setTitle(R.string.net_error_tip);
    }

    @Subscribe
    public void answerConnectionStatusChanged(ConnectionStatusChangedEvent event) {
        int connectedState=event.connectedState;
        String reason=event.reason;
        switch (connectedState) {
            case TTTalkService.CONNECTED:
                getSupportActionBar().setTitle(XMPPUtils.splitJidAndServer(App.readUser().getAccount()));
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
    protected void onDestroy() {
        App.mBus.unregister(this);

        super.onDestroy();
    }

    @Subscribe
    public void answerLogout(LogoutEvent event) {
        finish();
        App.saveUser(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindXMPPService();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);
        ButterKnife.inject(this);
        App.mBus.register(this);

        mSlidingMenuTitles = getResources().getStringArray(R.array.slidingmenu_array);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // improve performance by indicating the list if fixed size.
//        mDrawerList.setHasFixedSize(true);
//        mDrawerList.setLayoutManager(new LinearLayoutManager(this));

        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new SlidingMenuAdapter(this, mSlidingMenuTitles));
        mDrawerList.setOnItemClickListener(this);
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
//                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
//                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment  ;

        switch (position) {
            case 5:
                fragment = new SettingsFragment();
                break;

            default:
                fragment = new MainFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item title, then close the drawer
        getSupportActionBar().setTitle(mSlidingMenuTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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
