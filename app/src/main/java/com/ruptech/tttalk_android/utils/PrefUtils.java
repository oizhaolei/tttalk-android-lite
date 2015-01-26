package com.ruptech.tttalk_android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.model.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PrefUtils {
    public static final String GMAIL_SERVER = "talk.google.com";
    public final static String ISNEEDLOG = "isneedlog";
    public final static String REPORT_CRASH = "reportcrash";
    public final static String ACCOUNT = "account";
    public final static String PASSWORD = "password";
    public final static String Server = "server";
    public final static String AUTO_START = "auto_start";
    public final static String SHOW_MY_HEAD = "show_my_head";

    public final static String APP_VERSION = "app_version";


    public final static String OFFLINE = "offline";
    public final static String DND = "dnd";
    public final static String XA = "xa";
    public final static String AWAY = "away";
    public final static String AVAILABLE = "available";
    public final static String CHAT = "chat";

    public final static String JID = "account_jabberID";
    public final static String PORT = "account_port";
    public final static String RESSOURCE = "account_resource";
    public final static String PRIORITY = "account_prio";
    public final static String DEFAULT_PORT = "5222";
    public final static int DEFAULT_PORT_INT = 5222;
    public final static String CONN_STARTUP = "connstartup";
    public final static String AUTO_RECONNECT = "reconnect";
    public final static String MESSAGE_CARBONS = "carbons";
    public final static String SHOW_OFFLINE = "showOffline";
    public final static String LEDNOTIFY = "led";
    public final static String VIBRATIONNOTIFY = "vibration_list";
    public final static String SCLIENTNOTIFY = "ringtone";
    public final static String TICKER = "ticker";
    public final static String FOREGROUND = "foregroundService";
    public final static String SMACKDEBUG = "smackdebug";

    public final static String REQUIRE_TLS = "require_tls";
    public final static String STATUS_MODE = "status_mode";
    public final static String STATUS_MESSAGE = "status_message";
    public final static String THEME = "theme";
    final public static String PREF_USER = "USER_INFO";
    private static final String TAG = PrefUtils.class.getSimpleName();
    private static SharedPreferences mPref;

    public static String getPrefString(String key,
                                       final String defaultValue) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        return settings.getString(key, defaultValue);
    }

    public static void setPrefString(final String key,
                                     final String value) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        settings.edit().putString(key, value).commit();
    }

    public static boolean getPrefBoolean(final String key,
                                         final boolean defaultValue) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        return settings.getBoolean(key, defaultValue);
    }

    public static boolean hasKey(final String key) {
        return PreferenceManager.getDefaultSharedPreferences(App.mContext).contains(
                key);
    }

    public static void setPrefBoolean(final String key,
                                      final boolean value) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        settings.edit().putBoolean(key, value).commit();
    }

    public static void setPrefInt(final String key,
                                  final int value) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        settings.edit().putInt(key, value).commit();
    }

    public static int getPrefInt(final String key,
                                 final int defaultValue) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        return settings.getInt(key, defaultValue);
    }

    public static void setPrefFloat(final String key,
                                    final float value) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        settings.edit().putFloat(key, value).commit();
    }

    public static float getPrefFloat(final String key,
                                     final float defaultValue) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        return settings.getFloat(key, defaultValue);
    }

    public static void setSettingLong(final String key,
                                      final long value) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        settings.edit().putLong(key, value).commit();
    }

    public static long getPrefLong(final String key,
                                   final long defaultValue) {
        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(App.mContext);
        return settings.getLong(key, defaultValue);
    }

    public static void clearPreference(Context context,
                                       final SharedPreferences p) {
        final SharedPreferences.Editor editor = p.edit();
        editor.clear();
        editor.commit();
    }

    private static SharedPreferences getPref() {
        if (mPref == null) {
            mPref = PreferenceManager.getDefaultSharedPreferences(App.mContext);
        }
        return mPref;
    }


    public static void removePref(String prefKey) {
        getPref().edit().remove(prefKey).commit();
    }

    public static User readUser() {
        return (User) readObject(PREF_USER);
    }


    public static void writeUser(User user) {
        writeObject(PREF_USER, user);
    }


    private static Object readObject(String prefKey) {
        String str = getPref().getString(prefKey, "");
        byte[] bytes = str.getBytes();
        if (bytes.length == 0) {
            return null;
        }
        try {
            ByteArrayInputStream byteArray = new ByteArrayInputStream(bytes);
            Base64InputStream base64InputStream = new Base64InputStream(
                    byteArray, Base64.DEFAULT);
            ObjectInputStream in = new ObjectInputStream(base64InputStream);
            Object obj = in.readObject();
            in.close();
            return obj;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    private static void writeObject(String prefKey, Object obj) {
        if (obj == null) {
            removePref(prefKey);
        } else {
            try {
                ByteArrayOutputStream out;
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

                ObjectOutputStream objectOutput;
                objectOutput = new ObjectOutputStream(arrayOutputStream);
                objectOutput.writeObject(obj);
                byte[] data = arrayOutputStream.toByteArray();
                objectOutput.close();
                arrayOutputStream.close();

                out = new ByteArrayOutputStream();
                Base64OutputStream b64 = new Base64OutputStream(out,
                        Base64.DEFAULT);
                b64.write(data);
                b64.close();
                out.close();
                String str = new String(out.toByteArray());
                getPref().edit().putString(prefKey, str).commit();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);

            }
        }
    }

}
