package com.ashishjha.wordbuzzer.loader;

import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.ashishjha.wordbuzzer.model.Translation;
import com.ashishjha.wordbuzzer.utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by ashish on 30/1/16.
 */
/*
* Class responsible for parsing "words.json"
* (translation file from language "one" to language "two").
* It used JsonReader to parse file to avoid loading entire file in RAM.
* To have minimum RAM footprint - it initiates DB insertion in batch of 100 translations
* */
public class WordLoader {

    private static final String TAG = "WordLoader";

    /* Load the dictionary (translation file) - words.json
     * (translation file from language "one" to language "two") to DB.
     * */
    public int loadDictionary(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            return readJsonStream(context, is);
        } catch (IOException ex) {
            Log.d(TAG, "loadDictionary() ERROR");
            ex.printStackTrace();
        }
        return 0;
    }

    private int readJsonStream(Context context, InputStream in) throws IOException {
        // JsonReader is used to parse file to avoid loading entire file in RAM.
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readTranslationArray(context, reader);
        } finally {
            reader.close();
        }
    }

    private int readTranslationArray(Context context, JsonReader reader) throws IOException {
        ArrayList<Translation> translations = new ArrayList<>();
        InsertDictionaryWords insertWords = new InsertDictionaryWords();
        reader.beginArray();
        int pos = 0;
        while (reader.hasNext()) {
            ++pos;
            Translation translation = getTranslation(reader);
            translations.add(translation);
            // Keep only MAX_TRANSLATIONS_IN_MEMORY at once.
            // To have minimum RAM footprint - it initiates DB insertion in batch of 100 translations
            // When count reaches MAX_TRANSLATIONS_IN_MEMORY, insert them into DB and clear data structure
            if (pos % Util.MAX_TRANSLATIONS_IN_MEMORY == 0) {
                insertWords.insertDictionaryWordsInDB(context, translations, pos + 1 - Util.MAX_TRANSLATIONS_IN_MEMORY);
                translations.clear();
            }
        }
        reader.endArray();
        int nextPos = (pos / Util.MAX_TRANSLATIONS_IN_MEMORY);
        nextPos *= Util.MAX_TRANSLATIONS_IN_MEMORY;
        nextPos++;
        insertWords.insertDictionaryWordsInDB(context, translations, nextPos);
        Log.d(TAG, "Total Words= " + pos);
        return pos;
    }

    private Translation getTranslation(JsonReader reader) throws IOException {
        String english = null, spa = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(Util.TEXT_ENGLISH)) {
                english = reader.nextString();
            } else if (name.equals(Util.TEXT_SPA)) {
                spa = reader.nextString();
            }
        }
        reader.endObject();
        return new Translation(english, spa);
    }
}
