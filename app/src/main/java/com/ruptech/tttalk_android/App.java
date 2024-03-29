package com.ruptech.tttalk_android;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.ruptech.tttalk_android.bus.LogoutEvent;
import com.ruptech.tttalk_android.http.HttpServer;
import com.ruptech.tttalk_android.model.User;
import com.ruptech.tttalk_android.smack.Smack;
import com.ruptech.tttalk_android.utils.AssetsPropertyReader;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.squareup.otto.Bus;

import java.util.Properties;


/**
 * A login screen that offers login via email/password.
 */
public class App extends Application implements
        Thread.UncaughtExceptionHandler {
    public final static String TAG = App.class.getName();
    static public Properties properties;
    public static Context mContext;
    public static NotificationManager notificationManager;
    public static Smack mSmack;
    public static Bus mBus;
    private static HttpServer httpServer;
    private static User user;

    public static int getAppVersionCode() {
        int verCode = Integer.MAX_VALUE;
        try {
            PackageInfo packageInfo = App.mContext.getPackageManager()
                    .getPackageInfo(App.mContext.getPackageName(), 0);
            verCode = packageInfo.versionCode;

        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, e.getMessage(), e);
        }

        return verCode;
    }

    public static HttpServer getHttpServer() {
        if (httpServer == null) {
            httpServer = new HttpServer();
        }
        return httpServer;
    }

    public static User readUser() {
        if (user == null)
            user = PrefUtils.readUser();
        return user;
    }

    public static void saveUser(User user) {
        PrefUtils.writeUser(user);
        App.user = user;
    }

    public static void logout() {

        Log.v(TAG, "logout.");
        mBus.post(new LogoutEvent());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (BuildConfig.DEBUG)
            Log.e(TAG, thread.getName(), throwable);

        exitApp();
    }

    public void exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG)
            Log.e(TAG, "App.onCreate");
        Thread.setDefaultUncaughtExceptionHandler(this);
        mBus = new Bus();

        mContext = this.getApplicationContext();
        notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        AssetsPropertyReader assetsPropertyReader = new AssetsPropertyReader(this);
        properties = assetsPropertyReader.getProperties("env.properties");

    }

}



