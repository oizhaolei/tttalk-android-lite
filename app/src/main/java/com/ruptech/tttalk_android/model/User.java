package com.ruptech.tttalk_android.model;

public class User extends Roster {
    private static final long serialVersionUID = 6503913403445783857L;

    String account;
    String password;
    String lang;
    long tttalkid;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLang() {
        return lang;
    }
    public long getTTTalkId() {
        return tttalkid;
    }
    public void setTTTalkId(long tttalk_id) {
        tttalkid = tttalk_id;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
