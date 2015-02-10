package com.ruptech.tttalk_android.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.ruptech.tttalk_android.BuildConfig;
import com.ruptech.tttalk_android.model.Chat;

import java.util.ArrayList;

public class ChatProvider extends ContentProvider {

    public static final String AUTHORITY = "com.ruptech.tttalk_android.provider.Chats";
    public static final String TABLE_NAME = "chats";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + TABLE_NAME);

    private static final UriMatcher URI_MATCHER = new UriMatcher(
            UriMatcher.NO_MATCH);

    private static final int MESSAGES = 1;
    private static final int MESSAGE_ID = 2;

    static {
        URI_MATCHER.addURI(AUTHORITY, "chats", MESSAGES);
        URI_MATCHER.addURI(AUTHORITY, "chats/#", MESSAGE_ID);
    }

    private static final String TAG = "ChatProvider";
    public static SQLiteDatabase.CursorFactory mCursorFactory = new SQLiteDatabase.CursorFactory() {
        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                                String editTable, SQLiteQuery query) {
            if (BuildConfig.DEBUG)
                Log.i(TAG, query.toString());
            return new SQLiteCursor(db, driver, editTable, query);
        }
    };
    private SQLiteOpenHelper mOpenHelper;

    public ChatProvider() {
    }

    private static void infoLog(String data) {
        Log.i(TAG, data);
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(url)) {

            case MESSAGES:
                count = db.delete(TABLE_NAME, where, whereArgs);
                break;
            case MESSAGE_ID:
                String segment = url.getPathSegments().get(1);

                if (TextUtils.isEmpty(where)) {
                    where = "_id=" + segment;
                } else {
                    where = "_id=" + segment + " AND (" + where + ")";
                }

                count = db.delete(TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + url);
        }

        getContext().getContentResolver().notifyChange(url, null);
        return count;
    }

    @Override
    public String getType(Uri url) {
        int match = URI_MATCHER.match(url);
        switch (match) {
            case MESSAGES:
                return ChatConstants.CONTENT_TYPE;
            case MESSAGE_ID:
                return ChatConstants.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        if (URI_MATCHER.match(url) != MESSAGES) {
            throw new IllegalArgumentException("Cannot insert into URL: " + url);
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        for (String colName : ChatConstants.getRequiredColumns()) {
            if (values.containsKey(colName) == false) {
                throw new IllegalArgumentException("Missing column: " + colName);
            }
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(TABLE_NAME, ChatConstants.DATE, values);

        if (rowId < 0) {
            throw new SQLException("Failed to insert row into " + url);
        }

        Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(noteUri, null);
        return noteUri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ChatDatabaseHelper(getContext(), mCursorFactory);
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        int match = URI_MATCHER.match(url);

        switch (match) {
            case MESSAGES:
                qBuilder.setTables(TABLE_NAME);
                break;
            case MESSAGE_ID:
                qBuilder.setTables(TABLE_NAME);
                qBuilder.appendWhere("_id=");
                qBuilder.appendWhere(url.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = ChatConstants.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qBuilder.query(db, projectionIn, selection, selectionArgs,
                null, null, orderBy);

        if (ret == null) {
            infoLog("ChatProvider.query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), url);
        }

        return ret;
    }

    @Override
    public int update(Uri url, ContentValues values, String where,
                      String[] whereArgs) {
        int count;
        long rowId = 0;
        int match = URI_MATCHER.match(url);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (match) {
            case MESSAGES:
                count = db.update(TABLE_NAME, values, where, whereArgs);
                break;
            case MESSAGE_ID:
                String segment = url.getPathSegments().get(1);
                rowId = Long.parseLong(segment);
                count = db.update(TABLE_NAME, values, "_id=" + rowId, null);
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URL: " + url);
        }

        infoLog("*** notifyChange() rowId: " + rowId + " url " + url);

        getContext().getContentResolver().notifyChange(url, null);
        return count;

    }

    public static Chat parseChat(Cursor cursor) {
        Chat chat = new Chat();
        chat.setDate(cursor.getLong(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DATE)));

        chat.setId(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants._ID)));
        chat.setMessage(cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.MESSAGE)));
        chat.setTo_content(cursor.getString(cursor
                .getColumnIndex(ChatConstants.TO_MESSAGE)));
        chat.setFromMe(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DIRECTION)));// 消息来自
        chat.setJid(cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.JID)));
        chat.setPid(cursor.getString(cursor
                .getColumnIndex(ChatConstants.PACKET_ID)));
        chat.setRead(cursor.getInt(cursor
                .getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS)));
        return chat;
    }

    private static class ChatDatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "chat.db";
        private static final int DATABASE_VERSION = 7;

        public ChatDatabaseHelper(Context context, SQLiteDatabase.CursorFactory cf) {
            super(context, DATABASE_NAME, cf, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            infoLog("creating new chat table");

            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + ChatConstants._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ChatConstants.DATE + " INTEGER,"
                    + ChatConstants.DIRECTION + " INTEGER," + ChatConstants.JID
                    + " TEXT," + ChatConstants.MESSAGE + " TEXT," + ChatConstants.TO_MESSAGE + " TEXT,"
                    + ChatConstants.DELIVERY_STATUS + " INTEGER,"
                    + ChatConstants.PACKET_ID + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion ==6) {
                db.execSQL("ALTER TABLE "+ TABLE_NAME +" ADD "+ ChatConstants.TO_MESSAGE + " TEXT;");
            }

        }

    }

    public static final class ChatConstants implements BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.chat";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.chat";
        public static final String DEFAULT_SORT_ORDER = "_id ASC"; // sort by
        public static final String DATE = "date";
        public static final String DIRECTION = "from_me";
        public static final String JID = "jid";
        public static final String MESSAGE = "message";
        public static final String TO_MESSAGE = "to_content";
        public static final String DELIVERY_STATUS = "read"; // SQLite can not
        // rename
        // columns,
        // reuse old
        // name
        public static final String PACKET_ID = "pid";
        // boolean mappings
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
        public static final int DS_NEW = 0; // < this message has not been
        // sent/displayed yet
        public static final int DS_SENT_OR_READ = 1; // < this message was sent
        // but not yet acked, or
        // it was received and
        // read
        public static final int DS_ACKED = 2; // < this message was XEP-0184

        private ChatConstants() {
        }
        // acknowledged

        public static ArrayList<String> getRequiredColumns() {
            ArrayList<String> tmpList = new ArrayList<>();
            tmpList.add(DATE);
            tmpList.add(DIRECTION);
            tmpList.add(JID);
            tmpList.add(MESSAGE);
            return tmpList;
        }

    }

}
