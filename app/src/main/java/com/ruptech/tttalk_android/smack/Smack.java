package com.ruptech.tttalk_android.smack;

import com.ruptech.tttalk_android.exception.XMPPException;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.Collection;

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

    public void sendMessage(String user, String message, Collection<PacketExtension> extensions);

    public void sendServerPing();

    public String getNameForJID(String jid);

    boolean createAccount(String username, String password);

    byte[] getAvatar(String jid) throws XMPPException;

    String getUser();
}
