package com.ycm.demo.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ycm.demo.data.model.User;

import java.io.ByteArrayOutputStream;

public class UserDatabase {
    private MySQLiteHelper mMySQLiteHelper;

    public UserDatabase(Context context) {
        mMySQLiteHelper = new MySQLiteHelper(context);
    }

    public long insert(User user) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", user.getId());
        contentValues.put("account_id", user.getAccountId());
        contentValues.put("user_name", user.getUserName());
        contentValues.put("nick_name", user.getNickName());
        contentValues.put("sex", user.getSex());

        Bitmap bmpout = user.getIcon();
        if (bmpout != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bmpout.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            if (byteArray.length > 0) {
                contentValues.put("icon", byteArray);
            }
        }

        contentValues.put("create_time", user.getCreateTime());
        contentValues.put("modify_time", user.getModifyTime());

        long status = database.insert(MySQLiteHelper.TABLE_USER, null, contentValues);
        return  status;
    }

    public long update(User user) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("account_id", user.getAccountId());
        contentValues.put("user_name", user.getUserName());
        contentValues.put("nick_name", user.getNickName());
        contentValues.put("sex", user.getSex());

        Bitmap bmpout = user.getIcon();
        if (bmpout != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bmpout.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            if (byteArray.length > 0) {
                contentValues.put("icon", byteArray);
            }
        }

        contentValues.put("create_time", user.getCreateTime());
        contentValues.put("modify_time", user.getModifyTime());

        long status = database.update(MySQLiteHelper.TABLE_USER, contentValues, "id = ?", new String[]{String.valueOf(user.getId())});
        return  status;
    }

    public User query() {
        SQLiteDatabase database = mMySQLiteHelper.getReadableDatabase();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_USER, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            int accountId = cursor.getInt(1);
            String user_name = cursor.getString(2);
            String nick_name = cursor.getString(3);
            int sex = cursor.getInt(4);
            byte[] icon = cursor.getBlob(5);
            String createTime = cursor.getString(6);
            String modifyTime = cursor.getString(7);

            Bitmap bmpout = null;
            if (icon != null && icon.length > 0) {
                bmpout = BitmapFactory.decodeByteArray(icon, 0, icon.length);
            }

            User user = new User();
            user.setId(id);
            user.setAccountId(accountId);
            user.setUserName(user_name);
            user.setNickName(nick_name);
            user.setSex(sex);
            user.setIcon(bmpout);
            user.setCreateTime(Long.valueOf(createTime));
            user.setModifyTime(Long.valueOf(modifyTime));

            cursor.close();
            return user;
        }
        cursor.close();
        return null;
    }

    public void clear() {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        database.delete(MySQLiteHelper.TABLE_USER, null, null);
    }

    public void close() {
        mMySQLiteHelper.close();
    }
}
