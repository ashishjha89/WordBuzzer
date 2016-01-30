package com.ashishjha.wordbuzzer.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ashishjha.wordbuzzer.database.WordBuzzerDataSource;
import com.ashishjha.wordbuzzer.loader.WordLoader;
import com.ashishjha.wordbuzzer.utils.Util;

/**
 * Created by ashish on 30/1/16.
 */
/*
* It is responsible for loading translations in "words.json"
(translation file from language "one" to language "two") to DB..
* It takes help of WordLoader for this purpose.
* Once Dictionary is built (translations are stored in DB),
* set SharedPreference to indicate - Clean App flag to false.
* Also send broadcast to notify SplashScreenActivity to finish and launch WordBuzzerGameActivity
* */
public class DictionaryBuilderService extends IntentService {

    private static final String TAG = "DictionaryService";

    public DictionaryBuilderService() {
        super("DictionaryBuilderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(Util.LOAD_DICTIONARY)) {
            Log.d(TAG, "onHandleIntent()");
            clearDB();
            int wordCount = populateTranslationsInDB("words.json");
            setSharedPref(wordCount);
            notifyDictionaryLoadComplete();
        }
    }

    private void clearDB() {
        WordBuzzerDataSource tvListDataSource = new WordBuzzerDataSource(this);
        tvListDataSource.open();
        tvListDataSource.deleteFromTable(Util.DATABASE_NAME, null, null);
        tvListDataSource.close();
    }

    private int populateTranslationsInDB(String fileName) {
        WordLoader wordLoader = new WordLoader();
        return wordLoader.loadDictionary(this, fileName);
    }

    private void setSharedPref(int wordCount) {
        SharedPreferences.Editor editor = getSharedPreferences(Util.WORD_BUZZER_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(Util.IS_CLEAN_LAUNCH, false);
        editor.putInt(Util.WORDS_COUNT_IN_DICTIONARY, wordCount);
        editor.apply();
    }

    private void notifyDictionaryLoadComplete() {
        Intent intent = new Intent();
        intent.setAction(Util.LOAD_DICTIONARY_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
