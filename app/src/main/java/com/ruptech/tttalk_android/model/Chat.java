package com.ruptech.tttalk_android.model;

import java.io.Serializable;

public class Chat implements Serializable {
    private static final long serialVersionUID = -850853231465927885L;
    protected int id;
    protected long date;
    protected int fromMe;
    protected int read;
    protected String jid;
    protected long tttalkid = 48547;
    protected String message;

    public String getTo_content() {
        return to_content;
    }

    public void setTo_content(String to_content) {
        this.to_content = to_content;
    }

    protected String to_content;
    protected String pid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getFromMe() {
        return fromMe;
    }

    public void setFromMe(int fromMe) {
        this.fromMe = fromMe;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPid() {
        return pid;
    }
    public long getTTTalkId() {
        return tttalkid;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }
}