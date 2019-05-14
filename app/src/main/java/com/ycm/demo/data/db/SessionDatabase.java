package com.ycm.demo.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ycm.demo.data.model.Session;

public class SessionDatabase {
    private MySQLiteHelper mMySQLiteHelper;

    public SessionDatabase(Context context) {
        mMySQLiteHelper = new MySQLiteHelper(context);
    }

    public long insert(Session session) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", session.getId());
        contentValues.put("account_id", session.getAccountId());
        contentValues.put("sign", session.getSign());
        contentValues.put("create_time", session.getCreateTime());
        long status = database.insert(MySQLiteHelper.TABLE_SESSION, null, contentValues);
        return  status;
    }

    public long update(Session session) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("account_id", session.getAccountId());
        contentValues.put("sign", session.getSign());
        contentValues.put("create_time", session.getCreateTime());
        long status = database.update(MySQLiteHelper.TABLE_SESSION, contentValues, "id = ?", new String[session.getId()]);
        return  status;
    }

    public Session query() {
        SQLiteDatabase database = mMySQLiteHelper.getReadableDatabase();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_SESSION, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            int accountId = cursor.getInt(1);
            String sign = cursor.getString(2);
            String createTime = cursor.getString(3);

            Session session = new Session();
            session.setId(id);
            session.setAccountId(accountId);
            session.setSign(sign);
            session.setCreateTime(Long.valueOf(createTime));

            cursor.close();
            return session;
        }
        cursor.close();
        return null;
    }

    public void clear() {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        database.delete(MySQLiteHelper.TABLE_SESSION, null, null);
    }

    public void close() {
        mMySQLiteHelper.close();
    }
}