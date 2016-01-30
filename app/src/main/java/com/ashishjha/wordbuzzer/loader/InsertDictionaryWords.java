package com.ashishjha.wordbuzzer.loader;

import android.content.ContentValues;
import android.content.Context;

import com.ashishjha.wordbuzzer.database.WordBuzzerDataSource;
import com.ashishjha.wordbuzzer.model.Translation;
import com.ashishjha.wordbuzzer.utils.Util;

import java.util.ArrayList;

/**
 * Created by ashish on 30/1/16.
 */
public class InsertDictionaryWords {

    // startPos - position of 1st word in translationList
    // translationList - list of translations to be inserted in DB
    void insertDictionaryWordsInDB(Context context, ArrayList<Translation> translationList, int startPos) {
        if (translationList == null) {
            return;
        }
        ContentValues values = new ContentValues();
        WordBuzzerDataSource dataSource = new WordBuzzerDataSource(context);
        dataSource.open();
        for (Translation translation : translationList) {
            values.put(Util.TEXT_ENGLISH, translation.getWordInLanguageOne());
            values.put(Util.TEXT_SPA, translation.getWordInLanguageTwo());
            values.put(Util.WORD_POSITION, startPos++);
            dataSource.insertIntoTable(Util.DATABASE_NAME, values);
        }
        dataSource.close();
    }
}
