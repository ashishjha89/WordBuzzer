package com.ashishjha.wordbuzzer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ashishjha.wordbuzzer.utils.Util;

/**
 * Created by ashish on 30/1/16.
 */
public class WordBuzzerSqliteOpenHelper extends SQLiteOpenHelper {

    private static final String TRANSLATION_TABLE_CREATE = "create table "
            + Util.DATABASE_NAME + "("
            + Util.TEXT_ENGLISH + " text not null, "
            + Util.TEXT_SPA + " text not null, "
            + Util.WORD_POSITION + " int"
            + ");";

    public WordBuzzerSqliteOpenHelper(Context context) {
        super(context, Util.DATABASE_NAME, null,
                Util.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TRANSLATION_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TRANSLATION_TABLE_CREATE);
        onCreate(db);
    }
}
