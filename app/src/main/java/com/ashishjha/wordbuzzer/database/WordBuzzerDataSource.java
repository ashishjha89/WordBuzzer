package com.ashishjha.wordbuzzer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by ashish on 30/1/16.
 */
/*
* Abstraction for SqliteDatabase class - WordBuzzerSqliteOpenHelper
* Provides interface for insertion, deletion and query
* */
public class WordBuzzerDataSource {

    private SQLiteDatabase mDatabase;
    private WordBuzzerSqliteOpenHelper mWordBuzzerSQLiteOpenHelper;

    public WordBuzzerDataSource(Context context) {
        mWordBuzzerSQLiteOpenHelper = new WordBuzzerSqliteOpenHelper(
                context);
    }

    public void open() throws SQLException {
        mDatabase = mWordBuzzerSQLiteOpenHelper.getWritableDatabase();
    }

    public void close() {
        mWordBuzzerSQLiteOpenHelper.close();
    }

    public long insertIntoTable(String tableName, ContentValues values) {
        return mDatabase.insert(tableName, null, values);
    }

    public long deleteFromTable(String tableName, String where, String[] whereArgs) {
        return mDatabase.delete(tableName, where, whereArgs);
    }

    public Cursor getContents(String tableName, String[] projection, String where, String[] whereArgs) {
        return mDatabase.query(tableName, projection, where, whereArgs, null, null, null);
    }

}
