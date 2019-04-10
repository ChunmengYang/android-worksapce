package com.ycm.demo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.List;

public class BookDatabase {
    private MySQLiteHelper mMySQLiteHelper;

    public BookDatabase(Context context) {
        mMySQLiteHelper = new MySQLiteHelper(context);
    }

    public long insert(String name, String desc, byte[] cover) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("desc", desc);
        contentValues.put("cover", cover);
        long status = database.insert(MySQLiteHelper.TABLE_BOOK, null, contentValues);
        database.close();
        return  status;
    }

    public long updateCover(int id, byte[] cover) {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("cover", cover);
        long status = database.update(MySQLiteHelper.TABLE_BOOK, contentValues, "id = ?", new String[id]);
        database.close();
        return  status;
    }

    public List<Book> queryList(int pageIndex, int pageSize) {
        SQLiteDatabase database = mMySQLiteHelper.getReadableDatabase();
        List<Book> results = new ArrayList<Book>();


        String sqlLimt = " " + pageSize;
        if (pageIndex > 0) {
            sqlLimt += " offset " + (pageIndex * pageSize + 1);
        }
        Cursor cursor = database.query(MySQLiteHelper.TABLE_BOOK, null, null, null
        , null, null, null, sqlLimt);


        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String desc = cursor.getString(2);
            byte[] cover = cursor.getBlob(3);

            Bitmap bmpout = null;
            if (cover.length > 0) {
                bmpout = BitmapFactory.decodeByteArray(cover, 0, cover.length);
            }

            results.add(new Book(id, name, desc, bmpout));
        }
        database.close();
        return results;
    }

    public void clear() {
        SQLiteDatabase database = mMySQLiteHelper.getWritableDatabase();
        database.delete(MySQLiteHelper.TABLE_BOOK, null, null);
        database.close();
    }
}