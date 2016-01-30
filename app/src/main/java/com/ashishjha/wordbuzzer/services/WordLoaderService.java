package com.ashishjha.wordbuzzer.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.ashishjha.wordbuzzer.database.WordBuzzerDataSource;
import com.ashishjha.wordbuzzer.utils.Util;

/**
 * Created by ashish on 30/1/16.
 */
/*
* Service with which WordBuzzerGameActivity binds.
* This service is responsible for loading random words in background (50 at a time).
* Service also finds next "challenge" (language "one" word) and its associated correct answer.
* Service notifies Activity through QuizChangeListener (defined in the service) which is implemented by this activity.
* Activity gets its required members through instance of service fetched during its "onServiceConnected" callback.
* */
public class WordLoaderService extends Service {

    private Looper mServiceLooper;

    // Handler that runs on background thread to process message one at a time.
    private ServiceHandler mServiceHandler;

    // Stores array of random words. Only 50 words are fetched in service.
    // This is because query in DB is done through random "word positions" stored in DB
    // by using "IN" operation which is slow for large number of arguments
    private String[] mNextRandomWords;

    // DB holds position of translations.
    // This variable stores array of random no of positions (of translations)
    private int[] mNextRandomWordPositions;

    // Total words in dictionary
    private int mTotalWordCount;

    // Current Challenge being handled at WordBuzzerGameActivity
    private String mCurrentChallenge;

    private Context mContext;

    private final static int FIND_CORRECT_ANSWER_ID = 2;

    private final static int FIND_NEXT_WRONG_WORDS_ID = 3;

    private final static int FIND_NEXT_QUIZ_ID = 4;

    private final int ARRAY_SIZE = Util.MAX_TRANSLATIONS_IN_MEMORY / 2;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case FIND_NEXT_WRONG_WORDS_ID:
                    // Prefetch about 50 random words
                    prefetchRandomWords();
                    // Notify listener (WordBuzzerGameActivity)
                    mListener.setNextRandomWords(mNextRandomWords);
                    break;
                case FIND_NEXT_QUIZ_ID:
                    // Fetch "challenge" that needs to be displayed in center of screen
                    String next = getNextQuizWord();
                    mCurrentChallenge = next;
                    // Notify listener (WordBuzzerGameActivity)
                    mListener.setNextChallenge(next);
                    break;
                case FIND_CORRECT_ANSWER_ID:
                    // Fetch correct translation of challenge displayed in center of screen
                    String val = getCorrectAnswer();
                    // Notify listener (WordBuzzerGameActivity)
                    mListener.setCorrectAnswer(val);
                    break;
            }
        }
    }

    private final IBinder mBinder = new WordLoaderBinder();

    public class WordLoaderBinder extends Binder {
        public WordLoaderService getService() {
            // Return this instance of WordLoaderBinder so clients can call public methods
            return WordLoaderService.this;
        }
    }

    private QuizChangeListener mListener;

    /*
    * Interface used by service to communicate with WordBuzzerGameActivity
    * (which implements this interface).
    * Useful to setting all data used for a game session (translation of a word in language "one")
    * */
    public interface QuizChangeListener {
        /*
        *  Fetch about 50 random words.
        *  These words (language "two") fade in and out (one at a time).
        *  Correct answer should be appended in the random words set by this method
        *  so that player can choose correct answer.
        */
        void setNextRandomWords(String[] randomWords);

        /*
        *  Fetch next word (language "one") that is to be displayed
        *  at center of screen whose correct translation player needs to select
        */
        void setNextChallenge(String nextChallenge);

        /*
        * Fetch correct translation word (language "two") for the
        * challenge (language "one" word) displayed at center of screen
        */
        void setCorrectAnswer(String correctAnswer);
    }

    @Override
    public void onCreate() {
        mContext = this;
        HandlerThread thread = new HandlerThread("LoadNextWordThread");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mTotalWordCount = getSharedPreferences(Util.WORD_BUZZER_PREFS, MODE_PRIVATE).getInt(Util.WORDS_COUNT_IN_DICTIONARY, 0);
        mNextRandomWords = new String[ARRAY_SIZE];
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /* WordBuzzerGameActivity registers itself as a listener
    *  Using this listener, service is able to communicate data to activity like
    *  array of random words, language "one" challenge and correct answer of challenge
     */
    public void setQuizChangeListener(QuizChangeListener listener) {
        mListener = listener;
    }

    /*
    *  Fetch about 50 random words.
    *  These words (language "two") fade in and out (one at a time).
    *  Correct answer should be appended in the random words set by this method
    *  so that player can choose correct answer.
    */
    public synchronized void getNextRandomWords() {
        String[] selectionArgs = new String[ARRAY_SIZE];
        StringBuilder selectionBuilder = new StringBuilder();
        // In operator in query is slow for large no of parameters.
        // Hence we restrict it to 50
        selectionBuilder.append(Util.WORD_POSITION + " in (");
        for (int i = 0; i < ARRAY_SIZE - 1; i++) {
            selectionBuilder.append("?,");
            selectionArgs[i] = mNextRandomWordPositions[i] + "";
        }
        selectionBuilder.append("?)");
        String selection = selectionBuilder.toString();

        selectionArgs[ARRAY_SIZE - 1] = mNextRandomWordPositions[ARRAY_SIZE - 1] + "";
        String[] projection = {Util.TEXT_SPA};
        WordBuzzerDataSource wordBuzzerDataSource = new WordBuzzerDataSource(mContext);
        wordBuzzerDataSource.open();
        Cursor cursor = wordBuzzerDataSource.getContents(Util.DATABASE_NAME, projection, selection, selectionArgs);
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }
        int i = 0;
        mNextRandomWords = new String[cursor.getCount()];
        while (cursor.moveToNext()) {
            mNextRandomWords[i++] = cursor.getString(cursor.getColumnIndexOrThrow(Util.TEXT_SPA));
        }
        cursor.close();
        wordBuzzerDataSource.close();
    }

    /*
    *  Fetch next word (language "one") that is to be displayed
    *  at center of screen whose correct translation player needs to select
    */
    private String getNextQuizWord() {
        int pos = Util.getRandomNumber(1, mTotalWordCount);
        String[] projection = {Util.TEXT_ENGLISH};
        String selection = Util.WORD_POSITION + "=?";
        String[] selectionArgs = {pos + ""};
        WordBuzzerDataSource wordBuzzerDataSource = new WordBuzzerDataSource(mContext);
        wordBuzzerDataSource.open();
        Cursor cursor = wordBuzzerDataSource.getContents(Util.DATABASE_NAME, projection, selection, selectionArgs);
        if (cursor == null || cursor.getCount() == 0) {
            return "random string";
        }
        cursor.moveToFirst();
        String val = cursor.getString(cursor.getColumnIndexOrThrow(Util.TEXT_ENGLISH));
        cursor.close();
        wordBuzzerDataSource.close();
        return val;
    }

    /*
    * Fetch correct translation word (language "two") for the
    * challenge (language "one" word) displayed at center of screen
    */
    public String getCorrectAnswer() {
        String[] projection = {Util.TEXT_SPA};
        String selection = Util.TEXT_ENGLISH + "=?";
        String[] selectionArgs = {mCurrentChallenge};
        WordBuzzerDataSource wordBuzzerDataSource = new WordBuzzerDataSource(mContext);
        wordBuzzerDataSource.open();
        Cursor cursor = wordBuzzerDataSource.getContents(Util.DATABASE_NAME, projection, selection, selectionArgs);
        if (cursor == null || cursor.getCount() == 0) {
            return "random string";
        }
        cursor.moveToFirst();
        String val = cursor.getString(cursor.getColumnIndexOrThrow(Util.TEXT_SPA));
        cursor.close();
        wordBuzzerDataSource.close();
        return val;
    }

    /*
    * Initializes Game state by loading all required data including:
    * 1) Initiate loading of 50 random words.
    * 2) Finding next random challenge and
    * 3) its correct translation.
    * Activity is notified through QuizChangeListener callback
    * */
    public void initGameWithWords() {
        Message msg1 = mServiceHandler.obtainMessage();
        msg1.arg1 = FIND_NEXT_WRONG_WORDS_ID;
        mServiceHandler.sendMessage(msg1);

        Message msg2 = mServiceHandler.obtainMessage();
        msg2.arg1 = FIND_NEXT_QUIZ_ID;
        mServiceHandler.sendMessage(msg2);

        Message msg3 = mServiceHandler.obtainMessage();
        msg3.arg1 = FIND_CORRECT_ANSWER_ID;
        mServiceHandler.sendMessage(msg3);
    }

    // Prefetch next 50 random wrong words
    private void prefetchRandomWords() {
        mNextRandomWordPositions = new int[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            // Prefetch a random "position" for translation
            mNextRandomWordPositions[i] = Util.getRandomNumber(1, mTotalWordCount);
        }
        getNextRandomWords();
    }

    public String[] getRandomWords() {
        return mNextRandomWords;
    }
}
