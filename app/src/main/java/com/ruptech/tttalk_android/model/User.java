package com.ruptech.tttalk_android.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 6659682256959703673L;
    private String jid;
    private String alias;
    private String statusMode;
    private String statusMessage;

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getStatusMode() {
        return statusMode;
    }

    public void setStatusMode(String statusMode) {
        this.statusMode = statusMode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatus_message(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
