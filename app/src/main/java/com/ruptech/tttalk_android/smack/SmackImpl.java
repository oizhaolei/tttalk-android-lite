package com.ruptech.tttalk_android.smack;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.db.ChatProvider;
import com.ruptech.tttalk_android.db.ChatProvider.ChatConstants;
import com.ruptech.tttalk_android.db.RosterProvider;
import com.ruptech.tttalk_android.db.RosterProvider.RosterConstants;
import com.ruptech.tttalk_android.exception.XMPPException;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.ServerUtilities;
import com.ruptech.tttalk_android.utils.StatusMode;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import java.util.Collection;
import java.util.Date;

public class SmackImpl implements Smack {
    public static final String XMPP_IDENTITY_NAME = "tttalk";
    public static final String XMPP_IDENTITY_TYPE = "phone";
    public static final int PACKET_TIMEOUT = 30000;
    private static final String TAG = SmackImpl.class.getName();
    final static private String[] SEND_OFFLINE_PROJECTION = new String[]{
            ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE,
            ChatConstants.DATE, ChatConstants.PACKET_ID};
    final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION
            + " = "
            + ChatConstants.OUTGOING
            + " AND "
            + ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

    static {
        registerSmackProviders();
    }

    private final ContentResolver mContentResolver;
    private final Context mContext;
    private ConnectionConfiguration mXMPPConfig;
    private XMPPConnection mXMPPConnection;
    private SmackListener mSmackListener;
    private Roster mRoster;
    private RosterListener mRosterListener;
    private PacketListener mPacketListener;
    private PacketListener mSendFailureListener;
    private PacketListener mPongListener;
    // ping-pong服务器
    private String mPingID;
    private long mPingTimestamp;

    public SmackImpl(Context context, SmackListener listener, ContentResolver contentResolver) {
        int port = PrefUtils.getPrefInt(
                PrefUtils.PORT, PrefUtils.DEFAULT_PORT_INT);
        String server = PrefUtils.getPrefString(
                PrefUtils.Server, PrefUtils.GMAIL_SERVER);
        boolean smackDebug = PrefUtils.getPrefBoolean(
                PrefUtils.SMACKDEBUG, false);
        boolean requireSsl = PrefUtils.getPrefBoolean(
                PrefUtils.REQUIRE_TLS, false);

        ProviderManager.getInstance().addExtensionProvider(FromLang.ELEMENT_NAME, FromLang.NAMESPACE, new FromLang.Provider());
        ProviderManager.getInstance().addExtensionProvider(ToLang.ELEMENT_NAME, ToLang.NAMESPACE, new ToLang.Provider());
        ProviderManager.getInstance().addExtensionProvider(Cost.ELEMENT_NAME, Cost.NAMESPACE, new Cost.Provider());
        ProviderManager.getInstance().addExtensionProvider(OriginId.ELEMENT_NAME, OriginId.NAMESPACE, new OriginId.Provider());
        ProviderManager.getInstance().addExtensionProvider(TTTalkExtension.ELEMENT_NAME, TTTalkExtension.NAMESPACE, new TTTalkExtension.Provider());

        this.mXMPPConfig = new ConnectionConfiguration(server, port);

        this.mXMPPConfig.setReconnectionAllowed(false);
        this.mXMPPConfig.setSendPresence(false);
        this.mXMPPConfig.setCompressionEnabled(false); // disable for now
        this.mXMPPConfig.setDebuggerEnabled(smackDebug);
        this.mXMPPConfig.setSASLAuthenticationEnabled(requireSsl);
        if (requireSsl) {
            this.mXMPPConfig
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        } else {
            this.mXMPPConfig
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        }
        this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
        SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
        SmackConfiguration.setKeepAliveInterval(-1);
        SmackConfiguration.setDefaultPingInterval(0);
        registerRosterListener();// 监听联系人动态变化

        this.mContext = context;
        this.mSmackListener = listener;
        mContentResolver = contentResolver;
    }

    // ping-pong服务器

    static void registerSmackProviders() {
        ProviderManager pm = ProviderManager.getInstance();
        // add IQ handling
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
                new DiscoverInfoProvider());
        // add delayed delivery notifications
        pm.addExtensionProvider("delay", "urn:xmpp:delay",
                new DelayInfoProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
        // add carbons and forwarding
        pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE,
                new Forwarded.Provider());
        pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
        pm.addExtensionProvider("received", Carbon.NAMESPACE,
                new Carbon.Provider());
        // add delivery receipts
        pm.addExtensionProvider(DeliveryReceipt.ELEMENT,
                DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
        pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
                DeliveryReceipt.NAMESPACE,
                new DeliveryReceiptRequest.Provider());
        // add XMPP Ping (XEP-0199)
        pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());

        ServiceDiscoveryManager.setIdentityName(XMPP_IDENTITY_NAME);
        ServiceDiscoveryManager.setIdentityType(XMPP_IDENTITY_TYPE);
    }

    public static void sendOfflineMessage(ContentResolver cr, String toJID,
                                          String message) {
        ContentValues values = new ContentValues();
        values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
        values.put(ChatConstants.JID, toJID);
        values.put(ChatConstants.MESSAGE, message);
        values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
        values.put(ChatConstants.DATE, System.currentTimeMillis());

        cr.insert(ChatProvider.CONTENT_URI, values);
    }

    @Override
    public boolean login(String account, String password) throws XMPPException {
        try {
            if (mXMPPConnection.isConnected()) {
                try {
                    mXMPPConnection.disconnect();
                } catch (Exception e) {
                    Log.d(TAG, "conn.disconnect() failed: " + e);
                }
            }
            mXMPPConnection.connect();
            if (!mXMPPConnection.isConnected()) {
                throw new XMPPException("SMACK connect failed without exception!");
            }
            mXMPPConnection.addConnectionListener(new ConnectionListener() {
                public void connectionClosedOnError(Exception e) {
                    mSmackListener.onConnectionFailed(e.getMessage());
                }

                public void connectionClosed() {
                }

                public void reconnectingIn(int seconds) {
                }

                public void reconnectionFailed(Exception e) {
                }

                public void reconnectionSuccessful() {
                }
            });
            initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功

            // SMACK auto-logins if we were authenticated before
            if (!mXMPPConnection.isAuthenticated()) {
                String ressource = PrefUtils.getPrefString(
                        PrefUtils.RESSOURCE, XMPP_IDENTITY_NAME);
                mXMPPConnection.login(account, password, ressource);
            }
            setStatusFromConfig();// 更新在线状态

        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e.getLocalizedMessage(),
                    e.getWrappedThrowable());
        } catch (Exception e) {
            // actually we just care for IllegalState or NullPointer or XMPPEx.
            Log.e(TAG, "login(): " + Log.getStackTraceString(e));
            throw new XMPPException(e.getLocalizedMessage(), e.getCause());
        }
        registerAllListener();// 注册监听其他的事件，比如新消息
        ServerUtilities.registerOpenfirePushOnServer(mXMPPConnection.getUser());
        return mXMPPConnection.isAuthenticated();
    }

    private void registerAllListener() {
        // actually, authenticated must be true now, or an exception must have
        // been thrown.
        if (isAuthenticated()) {
            registerMessageListener();
            registerMessageSendFailureListener();
            registerPongListener();
            sendOfflineMessages();
            if (mSmackListener == null) {
                mXMPPConnection.disconnect();
                return;
            }
            // we need to "ping" the service to let it know we are actually
            // connected, even when no roster entries will come in
            mSmackListener.onRosterChanged();
        }
    }

    /**
     * ********* start 新消息处理 *******************
     */
    private void registerMessageListener() {
        // do not register multiple packet listeners
        if (mPacketListener != null)
            mXMPPConnection.removePacketListener(mPacketListener);

        PacketTypeFilter filter = new PacketTypeFilter(Message.class);

        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                try {
                    if (packet instanceof Message) {
                        Message msg = (Message) packet;
                        String chatMessage = msg.getBody();

                        // try to extract a carbon
                        Carbon cc = CarbonManager.getCarbon(msg);
                        if (cc != null
                                && cc.getDirection() == Carbon.Direction.received) {
                            Log.d(TAG, "carbon: " + cc.toXML());
                            msg = (Message) cc.getForwarded()
                                    .getForwardedPacket();
                            chatMessage = msg.getBody();
                            // fall through
                        } else if (cc != null
                                && cc.getDirection() == Carbon.Direction.sent) {
                            Log.d(TAG, "carbon: " + cc.toXML());
                            msg = (Message) cc.getForwarded()
                                    .getForwardedPacket();
                            chatMessage = msg.getBody();
                            if (chatMessage == null)
                                return;
                            String fromJID = XMPPUtils.getJabberID(msg.getTo());

                            addChatMessageToDB(ChatConstants.OUTGOING, fromJID,
                                    chatMessage, ChatConstants.DS_SENT_OR_READ,
                                    System.currentTimeMillis(),
                                    msg.getPacketID());
                            // always return after adding
                            return;
                        }

                        if (chatMessage == null) {
                            return;
                        }

                        if (msg.getType() == Message.Type.error) {
                            chatMessage = "<Error> " + chatMessage;
                        }

                        long ts;
                        DelayInfo timestamp = (DelayInfo) msg.getExtension(
                                "delay", "urn:xmpp:delay");
                        if (timestamp == null)
                            timestamp = (DelayInfo) msg.getExtension("x",
                                    "jabber:x:delay");
                        if (timestamp != null)
                            ts = timestamp.getStamp().getTime();
                        else
                            ts = System.currentTimeMillis();

                        String fromJID = XMPPUtils.getJabberID(msg.getFrom());

                        if (fromJID.startsWith(App.properties.getProperty("translator_jid"))){
                            Collection<PacketExtension> extensions = msg.getExtensions();
                            for(PacketExtension ext : extensions){
                                if (ext instanceof TTTalkExtension){
                                    TTTalkExtension tttalkExtension =(TTTalkExtension)ext;
                                    String messageId = tttalkExtension.getValue("message_id");
                                    setToContent(messageId, chatMessage);
                                }
                            }

                        }else{
                            addChatMessageToDB(ChatConstants.INCOMING, fromJID,
                                    chatMessage, ChatConstants.DS_NEW, ts,
                                    msg.getPacketID());
                        }

                        mSmackListener.onNewMessage(fromJID, chatMessage);
                    }
                } catch (Exception e) {
                    // SMACK silently discards exceptions dropped from
                    // processPacket :(
                    Log.e(TAG, "failed to process packet:");
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        };

        mXMPPConnection.addPacketListener(mPacketListener, filter);
    }

    /**
     * ********* end 新消息处理 *******************
     */

    private void addChatMessageToDB(int direction, String JID, String message,
                                    int delivery_status, long ts, String packetID) {
        ContentValues values = new ContentValues();

        values.put(ChatConstants.DIRECTION, direction);
        values.put(ChatConstants.JID, JID);
        values.put(ChatConstants.MESSAGE, message);
        values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
        values.put(ChatConstants.DATE, ts);
        values.put(ChatConstants.PACKET_ID, packetID);

        mContentResolver.insert(ChatProvider.CONTENT_URI, values);
    }

    public void setToContent(String messageID, String message) {
        ContentValues cv = new ContentValues();
        cv.put(ChatConstants.TO_MESSAGE, message);
        Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                + ChatProvider.TABLE_NAME);
        mContentResolver.update(rowuri, cv, ChatConstants.MESSAGE_ID
                + " = ?  " , new String[]{messageID});
    }


    /**
     * ************** start 处理消息发送失败状态 **********************
     */
    private void registerMessageSendFailureListener() {
        // do not register multiple packet listeners
        if (mSendFailureListener != null)
            mXMPPConnection
                    .removePacketSendFailureListener(mSendFailureListener);

        PacketTypeFilter filter = new PacketTypeFilter(Message.class);

        mSendFailureListener = new PacketListener() {
            public void processPacket(Packet packet) {
                try {
                    if (packet instanceof Message) {
                        Message msg = (Message) packet;
                        String chatMessage = msg.getBody();

                        Log.d("SmackableImp",
                                "message "
                                        + chatMessage
                                        + " could not be sent (ID:"
                                        + (msg.getPacketID() == null ? "null"
                                        : msg.getPacketID()) + ")");
                        changeMessageDeliveryStatus(msg.getPacketID(),
                                ChatConstants.DS_NEW);
                    }
                } catch (Exception e) {
                    // SMACK silently discards exceptions dropped from
                    // processPacket :(
                    Log.e(TAG, "failed to process packet:");
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        };

        mXMPPConnection.addPacketSendFailureListener(mSendFailureListener,
                filter);
    }

    /**
     * ************** end 处理消息发送失败状态 **********************
     */

    public void changeMessageDeliveryStatus(String packetID, int new_status) {
        ContentValues cv = new ContentValues();
        cv.put(ChatConstants.DELIVERY_STATUS, new_status);
        Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                + ChatProvider.TABLE_NAME);
        mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID
                + " = ? AND " + ChatConstants.DIRECTION + " = "
                + ChatConstants.OUTGOING, new String[]{packetID});
    }

    /**
     * ************** start 处理ping服务器消息 **********************
     */
    private void registerPongListener() {
        // reset ping expectation on new connection
        mPingID = null;

        if (mPongListener != null)
            mXMPPConnection.removePacketListener(mPongListener);

        mPongListener = new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                if (packet == null)
                    return;

                if (packet.getPacketID().equals(mPingID)) {
                    Log.i(TAG, String.format(
                            "Ping: server latency %1.3fs",
                            (System.currentTimeMillis() - mPingTimestamp) / 1000.));
                    mPingID = null;
                    mSmackListener.unRegisterTimeoutAlarm();
                }
            }

        };

        mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(
                IQ.class));

        mSmackListener.onLogin();
    }

    /**
     * ************** start 发送离线消息 **********************
     */
    public void sendOfflineMessages() {
        Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
                SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);
        final int _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
        final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
        final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
        final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
        final int PACKETID_COL = cursor
                .getColumnIndexOrThrow(ChatConstants.PACKET_ID);
        ContentValues mark_sent = new ContentValues();
        mark_sent.put(ChatConstants.DELIVERY_STATUS,
                ChatConstants.DS_SENT_OR_READ);
        while (cursor.moveToNext()) {
            int _id = cursor.getInt(_ID_COL);
            String toJID = cursor.getString(JID_COL);
            String message = cursor.getString(MSG_COL);
            String packetID = cursor.getString(PACKETID_COL);
            long ts = cursor.getLong(TS_COL);
            Log.d(TAG, "sendOfflineMessages: " + toJID + " > " + message);
            final Message newMessage = new Message(toJID, Message.Type.chat);
            newMessage.setBody(message);
            DelayInformation delay = new DelayInformation(new Date(ts));
            newMessage.addExtension(delay);
            newMessage.addExtension(new DelayInfo(delay));
            newMessage.addExtension(new DeliveryReceiptRequest());
            if ((packetID != null) && (packetID.length() > 0)) {
                newMessage.setPacketID(packetID);
            } else {
                packetID = newMessage.getPacketID();
                mark_sent.put(ChatConstants.PACKET_ID, packetID);
            }
            Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                    + ChatProvider.TABLE_NAME + "/" + _id);
            mContentResolver.update(rowuri, mark_sent, null, null);
            mXMPPConnection.sendPacket(newMessage); // must be after marking
            // delivered, otherwise it
            // may override the
            // SendFailListener
        }
        cursor.close();
    }

    /***************** end 处理ping服务器消息 ***********************/

    /**
     * **************************** start 联系人数据库事件处理 *********************************
     */
    private void registerRosterListener() {
        mRoster = mXMPPConnection.getRoster();
        mRosterListener = new RosterListener() {
            private boolean isFristRoter;

            @Override
            public void presenceChanged(Presence presence) {
                Log.i(TAG, "presenceChanged(" + presence.getFrom() + "): " + presence);
                String jabberID = XMPPUtils.getJabberID(presence.getFrom());
                RosterEntry rosterEntry = mRoster.getEntry(jabberID);
                updateRosterEntryInDB(rosterEntry);
                mSmackListener.onRosterChanged();
            }

            @Override
            public void entriesUpdated(Collection<String> entries) {

                Log.i(TAG, "entriesUpdated(" + entries + ")");
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    updateRosterEntryInDB(rosterEntry);
                }
                mSmackListener.onRosterChanged();
            }

            @Override
            public void entriesDeleted(Collection<String> entries) {
                Log.i(TAG, "entriesDeleted(" + entries + ")");
                for (String entry : entries) {
                    deleteRosterEntryFromDB(entry);
                }
                mSmackListener.onRosterChanged();
            }

            @Override
            public void entriesAdded(Collection<String> entries) {
                Log.i(TAG, "entriesAdded(" + entries + ")");
                ContentValues[] cvs = new ContentValues[entries.size()];
                int i = 0;
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
                }
                mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
                if (isFristRoter) {
                    isFristRoter = false;
                    mSmackListener.onRosterChanged();
                }
            }
        };
        mRoster.addRosterListener(mRosterListener);
    }

    /**
     * ************** end 发送离线消息 **********************
     */

    private void updateRosterEntryInDB(final RosterEntry entry) {
        final ContentValues values = getContentValuesForRosterEntry(entry);

        if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
                RosterConstants.JID + " = ?", new String[]{entry.getUser()}) == 0)
            addRosterEntryToDB(entry);
    }

    private void addRosterEntryToDB(final RosterEntry entry) {
        ContentValues values = getContentValuesForRosterEntry(entry);
        Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
        Log.i(TAG, "addRosterEntryToDB: Inserted " + uri);
    }

    private void deleteRosterEntryFromDB(final String jabberID) {
        int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
                RosterConstants.JID + " = ?", new String[]{jabberID});
        Log.i(TAG, "deleteRosterEntryFromDB: Deleted " + count + " entries");
    }

    private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
        final ContentValues values = new ContentValues();

        values.put(RosterConstants.JID, entry.getUser());
        values.put(RosterConstants.ALIAS, getName(entry));

        Presence presence = mRoster.getPresence(entry.getUser());
        values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
        values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
        values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));

        return values;
    }

    private String getGroup(Collection<RosterGroup> groups) {
        for (RosterGroup group : groups) {
            return group.getName();
        }
        return "";
    }

    private String getName(RosterEntry rosterEntry) {
        String name = rosterEntry.getName();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = StringUtils.parseName(rosterEntry.getUser());
        if (name.length() > 0) {
            return name;
        }
        return rosterEntry.getUser();
    }

    private StatusMode getStatus(Presence presence) {
        if (presence.getType() == Presence.Type.available) {
            if (presence.getMode() != null) {
                return StatusMode.valueOf(presence.getMode().name());
            }
            return StatusMode.available;
        }
        return StatusMode.offline;
    }

    private int getStatusInt(final Presence presence) {
        return getStatus(presence).ordinal();
    }

    public void setStatusFromConfig() {
        boolean messageCarbons = PrefUtils.getPrefBoolean(
                PrefUtils.MESSAGE_CARBONS, true);
        String statusMode = PrefUtils.getPrefString(
                PrefUtils.STATUS_MODE, PrefUtils.AVAILABLE);
        String statusMessage = PrefUtils.getPrefString(
                PrefUtils.STATUS_MESSAGE,
                mContext.getString(R.string.status_online));
        int priority = PrefUtils.getPrefInt(
                PrefUtils.PRIORITY, 0);
        if (messageCarbons)
            CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
                    true);

        Presence presence = new Presence(Presence.Type.available);
        Mode mode = Mode.valueOf(statusMode);
        presence.setMode(mode);
        presence.setStatus(statusMessage);
        presence.setPriority(priority);
        mXMPPConnection.sendPacket(presence);
    }

    /**
     * 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
     */
    private void initServiceDiscovery() {
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager
                .getInstanceFor(mXMPPConnection);
        if (sdm == null)
            sdm = new ServiceDiscoveryManager(mXMPPConnection);

        sdm.addFeature("http://jabber.org/protocol/disco#info");

        // reference PingManager, set ping flood protection to 10s
        PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
                10 * 1000);
        // reference DeliveryReceiptManager, add listener

        DeliveryReceiptManager dm = DeliveryReceiptManager
                .getInstanceFor(mXMPPConnection);
        dm.enableAutoReceipts();
        dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener() {
            public void onReceiptReceived(String fromJid, String toJid,
                                          String receiptId) {
                Log.d(TAG, "got delivery receipt for " + receiptId);
                changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
            }
        });
    }

    @Override
    public boolean isAuthenticated() {
        if (mXMPPConnection != null) {
            return (mXMPPConnection.isConnected() && mXMPPConnection
                    .isAuthenticated());
        }
        return false;
    }

    /**
     * **************************** end 联系人数据库事件处理 *********************************
     */

    @Override
    public void addRosterItem(String user, String alias, String group)
            throws XMPPException {

        addRosterEntry(user, alias, group);
    }

    private void addRosterEntry(String user, String alias, String group)
            throws XMPPException {
        mRoster = mXMPPConnection.getRoster();
        try {
            mRoster.createEntry(user, alias, new String[]{group});
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e.getLocalizedMessage());
        }
    }

    @Override
    public void removeRosterItem(String user) throws XMPPException {

        Log.d(TAG, "removeRosterItem(" + user + ")");

        removeRosterEntry(user);
        mSmackListener.onRosterChanged();
    }

    private void removeRosterEntry(String user) throws XMPPException {
        mRoster = mXMPPConnection.getRoster();
        try {
            RosterEntry rosterEntry = mRoster.getEntry(user);

            if (rosterEntry != null) {
                mRoster.removeEntry(rosterEntry);
            }
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e.getLocalizedMessage());
        }
    }

    @Override
    public void renameRosterItem(String user, String newName)
            throws XMPPException {

        mRoster = mXMPPConnection.getRoster();
        RosterEntry rosterEntry = mRoster.getEntry(user);

        if (!(newName.length() > 0) || (rosterEntry == null)) {
            throw new XMPPException("JabberID to rename is invalid!");
        }
        rosterEntry.setName(newName);
    }

    @Override
    public void moveRosterItemToGroup(String user, String group)
            throws XMPPException {

        tryToMoveRosterEntryToGroup(user, group);
    }

    private void tryToMoveRosterEntryToGroup(String userName, String groupName)
            throws XMPPException {

        mRoster = mXMPPConnection.getRoster();
        RosterGroup rosterGroup = getRosterGroup(groupName);
        RosterEntry rosterEntry = mRoster.getEntry(userName);

        removeRosterEntryFromGroups(rosterEntry);

        if (groupName.length() == 0)
            return;
        else {
            try {
                rosterGroup.addEntry(rosterEntry);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e.getLocalizedMessage());
            }
        }
    }

    private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
            throws XMPPException {
        Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

        for (RosterGroup group : oldGroups) {
            tryToRemoveUserFromGroup(group, rosterEntry);
        }
    }

    private void tryToRemoveUserFromGroup(RosterGroup group,
                                          RosterEntry rosterEntry) throws XMPPException {
        try {
            group.removeEntry(rosterEntry);
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e.getLocalizedMessage());
        }
    }

    private RosterGroup getRosterGroup(String groupName) {
        RosterGroup rosterGroup = mRoster.getGroup(groupName);

        // create group if unknown
        if ((groupName.length() > 0) && rosterGroup == null) {
            rosterGroup = mRoster.createGroup(groupName);
        }
        return rosterGroup;

    }

    @Override
    public void renameRosterGroup(String group, String newGroup) {

        Log.i(TAG, "oldgroup=" + group + ", newgroup=" + newGroup);
        mRoster = mXMPPConnection.getRoster();
        RosterGroup groupToRename = mRoster.getGroup(group);
        if (groupToRename == null) {
            return;
        }
        groupToRename.setName(newGroup);
    }

    @Override
    public void requestAuthorizationForRosterItem(String user) {

        Presence response = new Presence(Presence.Type.subscribe);
        response.setTo(user);
        mXMPPConnection.sendPacket(response);
    }

    @Override
    public void addRosterGroup(String group) {

        mRoster = mXMPPConnection.getRoster();
        mRoster.createGroup(group);
    }

    @Override
    public void sendMessage(String toJID, String message, Collection<PacketExtension> extensions) {

        final Message newMessage = new Message(toJID, Message.Type.chat);
        newMessage.setBody(message);
        newMessage.addExtension(new DeliveryReceiptRequest());
        if (extensions != null) {
            //TODO: merge tttalk extensions
            for (PacketExtension extension : extensions) {
                newMessage.addExtension(extension);
            }
        }

        if (isAuthenticated()) {
            addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
                    ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
                    newMessage.getPacketID());
            mXMPPConnection.sendPacket(newMessage);
        } else {
            // send offline -> store to DB
            addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
                    ChatConstants.DS_NEW, System.currentTimeMillis(),
                    newMessage.getPacketID());
        }
    }

    @Override
    public void sendServerPing() {
        if (mPingID != null) {
            Log.d(TAG, "Ping: requested, but still waiting for " + mPingID);
            return; // a ping is still on its way
        }
        Ping ping = new Ping();
        ping.setType(Type.GET);
        ping.setTo(PrefUtils.getPrefString(
                PrefUtils.Server, PrefUtils.GMAIL_SERVER));
        mPingID = ping.getPacketID();
        mPingTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Ping: sending ping " + mPingID);
        mXMPPConnection.sendPacket(ping);

        mSmackListener.registerTimeoutAlarm();
    }

    @Override
    public String getNameForJID(String jid) {
        if (null != this.mRoster.getEntry(jid)
                && null != this.mRoster.getEntry(jid).getName()
                && this.mRoster.getEntry(jid).getName().length() > 0) {
            return this.mRoster.getEntry(jid).getName();
        } else {
            return jid;
        }
    }

    @Override
    public boolean createAccount(String username, String password) {
        return false;
    }

    @Override
    public byte[] getAvatar(String jid) throws XMPPException {
        try {
            VCard vcard = new VCard();
            vcard.load(mXMPPConnection, jid);
            return vcard.getAvatar();
        } catch (Exception e) {
            throw new XMPPException(e.getMessage());
        }
    }

    @Override
    public String getUser() {
        return mXMPPConnection.getUser();
    }

    @Override
    public boolean logout() {
        Log.d(TAG, "unRegisterCallback()");
        ServerUtilities.unregisterOpenfirePushOnServer();
        // remove callbacks _before_ tossing old connection
        try {
            mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
            mXMPPConnection.removePacketListener(mPacketListener);
            mXMPPConnection
                    .removePacketSendFailureListener(mSendFailureListener);
            mXMPPConnection.removePacketListener(mPongListener);

            mSmackListener.onLogout();
        } catch (Exception e) {
            // ignore it!
            return false;
        }
        if (mXMPPConnection.isConnected()) {
            // work around SMACK's #%&%# blocking disconnect()
            new Thread() {
                public void run() {
                    Log.d(TAG, "shutDown thread started");
                    mXMPPConnection.disconnect();
                    Log.d(TAG, "shutDown thread finished");
                }
            }.start();
        }
        setStatusOffline();

        this.mSmackListener = null;
        return true;
    }

    private void setStatusOffline() {
        ContentValues values = new ContentValues();
        values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
        mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
    }

}
