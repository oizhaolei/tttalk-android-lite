package com.ruptech.tttalk_android.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.model.User;
import com.ruptech.tttalk_android.service.IConnectionStatusCallback;
import com.ruptech.tttalk_android.service.TTTalkService;
import com.ruptech.tttalk_android.utils.PrefUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends ActionBarActivity implements
        IConnectionStatusCallback {
    public static final String LOGIN_ACTION = "com.ruptech.tttalk_android.action.LOGIN";
    private static final String TAG = LoginActivity.class.getName();
    // UI references.
    @InjectView(R.id.server_edittext)
    EditText mServerView;
    @InjectView(R.id.username_edittext)
    EditText mUsernameView;
    @InjectView(R.id.password_edittext)
    EditText mPasswordView;
    @InjectView(R.id.lang_edittext)
    EditText mLangView;
    private TTTalkService mService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TTTalkService.XXBinder) service).getService();
            mService.registerConnectionStatusCallback(LoginActivity.this);
            // 开始连接xmpp服务器
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.unRegisterConnectionStatusCallback();
            mService = null;
        }

    };
    private ProgressDialog progressDialog;

    private void gotoMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindXMPPService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
        startService(new Intent(LoginActivity.this, TTTalkService.class));
        bindXMPPService();

        Log.v(TAG, App.properties.getProperty("server.url"));

        if (App.readUser() != null) {
            gotoMainActivity();
            finish();
            return;
        }

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        //TODO: encrypt password
        mServerView.setText(App.properties.getProperty("test.server"));
        if (App.readUser() == null) {
            mUsernameView.setText(App.properties.getProperty("test.username"));
            mPasswordView.setText(App.properties.getProperty("test.password"));
        } else {
            mUsernameView.setText(App.readUser().getAccount());
            mPasswordView.setText(App.readUser().getPassword());
            mLangView.setText(App.readUser().getLang());
        }
    }

    @OnClick(R.id.username_sign_in_button)
    public void doSignIn() {
        attemptLogin();
    }

    @OnClick(R.id.username_sign_up_button)
    public void doSignUp() {
        //TODO : create account, register
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mLangView.setError(null);

        // Store values at the time of the login attempt.
        String server = mServerView.getText().toString();
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String lang = mLangView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isLangValid(lang)) {
            mLangView.setError(getString(R.string.error_invalid));
            focusView = mLangView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            User me = new User();
            me.setAccount(username);
            me.setPassword(password);
            me.setLang(lang);
            App.saveUser(me);
            PrefUtils.setPrefString(PrefUtils.Server, server);


            progressDialog = ProgressDialog.show(LoginActivity.this,
                    LoginActivity.this.getString(R.string.progress_title),
                    LoginActivity.this.getString(R.string.progress_message), true, true);

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mService.login(username, password);

//            mAuthTask = new UserLoginTask(username, password);
//            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
        return username.length() > 1;
    }

    private boolean isLangValid(String lang) {
        return lang.length() > 1;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 1;
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        if (connectedState == TTTalkService.CONNECTED && mService.isAuthenticated()) {
            save2Preferences();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void save2Preferences() {

        PrefUtils.setPrefString(
                PrefUtils.STATUS_MODE,
                PrefUtils.AVAILABLE);
    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            Log.i(TAG, "unbindXMPPServiced");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Service wasn't bound!");
        }
    }

    private void bindXMPPService() {
        Log.e(TAG, "bindXMPPService");
        Intent mServiceIntent = new Intent(this, TTTalkService.class);
        mServiceIntent.setAction(LOGIN_ACTION);
        bindService(mServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

}



