package com.ycm.demo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "books.db";

    public static final int DB_VERSION = 1;

    public static final String TABLE_BOOK = "book";
    private static final String BOOK_CREATE_TABLE_SQL = "create table " + TABLE_BOOK + "("
            + "id INTEGER primary key autoincrement,"
            + "name TEXT not null,"
            + "desc TEXT not null,"
            + "cover BLOB"
            + ");";

    public MySQLiteHelper(Context context) {
        // 传递数据库名与版本号给父类
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BOOK_CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
