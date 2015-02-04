package com.ruptech.tttalk_android.activity;

import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.baidu.baidutranslate.openapi.TranslateClient;
import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.adapter.ChatAdapter;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.db.RosterProvider;
import com.ruptech.tttalk_android.service.IConnectionStatusCallback;
import com.ruptech.tttalk_android.service.TTTalkService;
import com.ruptech.tttalk_android.utils.StatusMode;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ChatActivity extends ActionBarActivity implements OnTouchListener,
        OnClickListener, IConnectionStatusCallback {
    public static final String INTENT_EXTRA_USERNAME = ChatActivity.class
            .getName() + ".username";// 昵称对应的key
    private static final String TAG = ChatActivity.class.getName();
    private static final String[] PROJECTION_FROM = new String[]{
            ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
            ChatProvider.ChatConstants.DIRECTION,
            ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
            ChatProvider.ChatConstants.DELIVERY_STATUS,
            ChatConstants.PACKET_ID};// 查询字段
    // 查询联系人数据库字段
    private static final String[] STATUS_QUERY = new String[]{
            RosterProvider.RosterConstants.STATUS_MODE,
            RosterProvider.RosterConstants.STATUS_MESSAGE,};
    @InjectView(R.id.msg_listView)
    ListView mMsgListView;// 对话ListView
    @InjectView(R.id.send)
    Button mSendMsgBtn;// 发送消息button
    @InjectView(R.id.input)
    EditText mChatEditText;// 消息输入框
    private InputMethodManager mInputMethodManager;
    private String mWithJabberID = null;// 当前聊天用户的ID
    private ContentObserver mContactObserver = new ContactObserver();// 联系人数据监听，主要是监听对方在线状态
    private TTTalkService mService;// Main服务
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TTTalkService.XXBinder) service).getService();
            mService.registerConnectionStatusCallback(ChatActivity.this);
            // 如果没有连接上，则重新连接xmpp服务器
            if (!mService.isAuthenticated()) {
                String usr = App.readUser().getAccount();
                String password = App.readUser().getPassword();
                mService.login(usr, password);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.unRegisterConnectionStatusCallback();
            mService = null;
        }

    };
    private TranslateClient client;

    // 【重要】 onCreate时候初始化翻译相关功能
    private void initTransClient() {
        client = new TranslateClient(this, App.properties.getProperty("baidu_api_key"));

        // 这里可以设置为在线优先、离线优先、 只在线、只离线 4种模式，默认为在线优先。
        client.setPriority(TranslateClient.Priority.OFFLINE_FIRST);
    }

    /**
     * 解绑服务
     */
    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            Log.i(TAG, "unbindXMPPService");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Service wasn't bound!");
        }
    }

    /**
     * 绑定服务
     */
    private void bindXMPPService() {
        Log.i(TAG, "bindXMPPService");
        Intent serviceIntent = new Intent(this, TTTalkService.class);
        Uri chatURI = Uri.parse(mWithJabberID);
        serviceIntent.setData(chatURI);
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.inject(this);

        initTransClient();// 初始化翻译相关功能

        initData();// 初始化数据
        initView();// 初始化view
        setChatWindowAdapter();// 初始化对话数据
        getContentResolver().registerContentObserver(
                RosterProvider.CONTENT_URI, true, mContactObserver);// 开始监听联系人数据库

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateContactStatus();// 更新联系人状态
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updateContactStatus() {
        Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI,
                STATUS_QUERY, RosterProvider.RosterConstants.JID + " = ?",
                new String[]{mWithJabberID}, null);
        int MODE_IDX = cursor
                .getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
        int MSG_IDX = cursor
                .getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            int status_mode = cursor.getInt(MODE_IDX);
            String status_message = cursor.getString(MSG_IDX);
            Log.d(TAG, "contact status changed: " + status_mode + " " + status_message);
            getSupportActionBar().setTitle(XMPPUtils.splitJidAndServer(getIntent()
                    .getStringExtra(INTENT_EXTRA_USERNAME)));
            int statusId = StatusMode.values()[status_mode].getDrawableId();
            if (statusId != -1) {// 如果对应离线状态
                // Drawable icon = getResources().getDrawable(statusId);
                // mTitleNameView.setCompoundDrawablesWithIntrinsicBounds(icon,
                // null,
                // null, null);
            } else {
            }
        }
        cursor.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hasWindowFocus())
            unbindXMPPService();// 解绑服务
        getContentResolver().unregisterContentObserver(mContactObserver);

        if (client != null) {
            client.onDestroy();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 窗口获取到焦点时绑定服务，失去焦点将解绑
        if (hasFocus)
            bindXMPPService();
        else
            unbindXMPPService();
    }

    private void initData() {
        mWithJabberID = getIntent().getDataString().toLowerCase();// 获取聊天对象的id
        // 将表情map的key保存在数组中
    }

    /**
     * 设置聊天的Adapter
     */
    private void setChatWindowAdapter() {
        String selection = ChatConstants.JID + "='" + mWithJabberID + "'";
        // 异步查询数据库
        new AsyncQueryHandler(getContentResolver()) {

            @Override
            protected void onQueryComplete(int token, Object cookie,
                                           Cursor cursor) {
                // ListAdapter adapter = new ChatWindowAdapter(cursor,
                // PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
                ListAdapter adapter = new ChatAdapter(ChatActivity.this,
                        cursor, PROJECTION_FROM, client);
                mMsgListView.setAdapter(adapter);
                mMsgListView.setSelection(adapter.getCount() - 1);
            }

        }.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM,
                selection, null, null);
        // 同步查询数据库，建议停止使用,如果数据庞大时，导致界面失去响应
        // Cursor cursor = managedQuery(ChatProvider.CONTENT_URI,
        // PROJECTION_FROM,
        // selection, null, null);
        // ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
        // PROJECTION_TO, mWithJabberID);
        // mMsgListView.setAdapter(adapter);
        // mMsgListView.setSelection(adapter.getCount() - 1);
    }

    private void initView() {
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // 触摸ListView隐藏表情和输入法
        mMsgListView.setOnTouchListener(this);
        mChatEditText.setOnTouchListener(this);
        mChatEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                if (s.length() > 0) {
                    mSendMsgBtn.setEnabled(true);
                } else {
                    mSendMsgBtn.setEnabled(false);
                }
            }
        });
        mSendMsgBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:// 发送消息
                sendMessageIfNotNull();
                break;
            default:
                break;
        }
    }

    private void sendMessageIfNotNull() {
        if (mChatEditText.getText().length() >= 1) {
            if (mService != null) {
                mService.sendMessage(mWithJabberID, mChatEditText.getText()
                        .toString(), null);
                if (!mService.isAuthenticated())
                    Toast.makeText(this, "消息已经保存随后发送", Toast.LENGTH_SHORT).show();
            }
            mChatEditText.setText(null);
            mSendMsgBtn.setEnabled(false);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.msg_listView:
                mInputMethodManager.hideSoftInputFromWindow(
                        mChatEditText.getWindowToken(), 0);
                break;
            case R.id.input:
                mInputMethodManager.showSoftInput(mChatEditText, 0);
                break;

            default:
                break;
        }
        return false;
    }

    // 防止乱pageview乱滚动
    private OnTouchListener forbidenScroll() {
        return new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
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

    public TTTalkService getService() {
        return mService;
    }

    /**
     * 联系人数据库变化监听
     */
    private class ContactObserver extends ContentObserver {
        public ContactObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            Log.d(TAG, "ContactObserver.onChange: " + selfChange);
            updateContactStatus();// 联系人状态变化时，刷新界面
        }
    }

}
