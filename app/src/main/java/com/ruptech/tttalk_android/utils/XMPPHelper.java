package com.ruptech.tttalk_android.utils;

import android.content.Context;
import android.text.Editable;
import android.util.TypedValue;

import com.ruptech.tttalk_android.exception.XXAdressMalformedException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMPPHelper {

    public static void verifyJabberID(String jid)
            throws XXAdressMalformedException {
        if (jid != null) {
            Pattern p = Pattern
                    .compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
            Matcher m = p.matcher(jid);

            if (!m.matches()) {
                throw new XXAdressMalformedException(
                        "Configured Jabber-ID is incorrect!");
            }
        } else {
            throw new XXAdressMalformedException("Jabber-ID wasn't set!");
        }
    }

    public static void verifyJabberID(Editable jid)
            throws XXAdressMalformedException {
        verifyJabberID(jid.toString());
    }

    public static int getEditTextColor(Context ctx) {
        TypedValue tv = new TypedValue();
        boolean found = ctx.getTheme().resolveAttribute(
                android.R.attr.editTextColor, tv, true);
        if (found) {
            // SDK 11+
            return ctx.getResources().getColor(tv.resourceId);
        } else {
            // SDK < 11
            return ctx.getResources().getColor(
                    android.R.color.primary_text_light);
        }
    }

    public static String splitJidAndServer(String account) {
        if (!account.contains("@"))
            return account;
        String[] res = account.split("@");
        String userName = res[0];
        return userName;
    }

    /**
     * 处理字符串中的表情
     *
     * @param context
     * @param message 传入的需要处理的String
     * @param small   是否需要小图片
     * @return
     */
    public static CharSequence convertNormalStringToSpannableString(
            Context context, String message, boolean small) {
        return message;
    }

}
