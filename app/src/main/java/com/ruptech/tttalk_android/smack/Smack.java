package com.ruptech.tttalk_android.smack;

import com.ruptech.tttalk_android.exception.XMPPException;

public interface Smack {
    public boolean login(String account, String password) throws XMPPException;

    public boolean logout();

    public boolean isAuthenticated();

    public void addRosterItem(String user, String alias, String group)
            throws XMPPException;

    public void removeRosterItem(String user) throws XMPPException;

    public void renameRosterItem(String user, String newName)
            throws XMPPException;

    public void moveRosterItemToGroup(String user, String group)
            throws XMPPException;

    public void renameRosterGroup(String group, String newGroup);

    public void requestAuthorizationForRosterItem(String user);

    public void addRosterGroup(String group);

    public void setStatusFromConfig();

    public void sendMessage(String user, String message);

    public void sendServerPing();

    public String getNameForJID(String jid);
}
