package com.ruptech.tttalk_android.utils;

import com.ruptech.tttalk_android.App;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

public class Utils {

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static final String CATEGORY = "chinatalk.";
    public final static String TAG = Utils.CATEGORY
            + Utils.class.getSimpleName();

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0 || s.equals("null");
    }

    public static String genSign(Map<String, String> params, String appkey) {
        // sign
        StringBuilder sb = new StringBuilder();
        sb.append(appkey);

        // 对参数名进行字典排序
        String[] keyArray = params.keySet().toArray(new String[params.size()]);
        Arrays.sort(keyArray);

        for (String key : keyArray) {
            String value = params.get(key);
            if (!Utils.isEmpty(value)) {
                sb.append(key).append(value);
            }
        }
        sb.append(App.properties.getProperty("APK_SECRET"));

        String sign = Utils.sha1(sb.toString());

        return sign;
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        // 把密文转换成十六进制的字符串形式
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    public static String sha1(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(str.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
