package com.ashishjha.wordbuzzer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.ashishjha.wordbuzzer.R;
import com.ashishjha.wordbuzzer.services.DictionaryBuilderService;
import com.ashishjha.wordbuzzer.utils.Util;

/**
 * Created by ashish on 30/1/16.
 */
/*
* Launches directly WordBuzzerGameActivity if flag is set in SharedPreference.
* Activity responsible for loading dictionary.
* It starts service which loads dictionary to DB.
* Once Dictionary is loaded, this activity receives BroadcastReceiver
* and then launches WordBuzzerGameActivity
* */
public class SplashScreenActivity extends Activity {

    private boolean mIsCleanApp = true;

    private BroadcastReceiver mInterestingShowAdditionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Util.LOAD_DICTIONARY_COMPLETE)) {
                launchWordBuzzerActivity();
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIsCleanLaunch();
        if (mIsCleanApp) {
            setContentView(R.layout.splash_screen_layout);
            startDictionaryBuilderService();
        } else {
            launchWordBuzzerActivity();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check again - status of dictionary load may be changed while app in background
        checkIsCleanLaunch();
        if (!mIsCleanApp) {
            launchWordBuzzerActivity();
            finish();
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Util.LOAD_DICTIONARY_COMPLETE);
            LocalBroadcastManager.getInstance(this).registerReceiver(mInterestingShowAdditionReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mInterestingShowAdditionReceiver);
        } catch (IllegalArgumentException e) {
            // BroadcastReceiver was not registered
            e.printStackTrace();
        }
    }

    private void launchWordBuzzerActivity() {
        Intent intent = new Intent(this, WordBuzzerGameActivity.class);
        startActivity(intent);
    }

    private void startDictionaryBuilderService() {
        Intent intent = new Intent(this, DictionaryBuilderService.class);
        intent.setAction(Util.LOAD_DICTIONARY);
        startService(intent);
    }

    private void checkIsCleanLaunch() {
        SharedPreferences prefs = getSharedPreferences(Util.WORD_BUZZER_PREFS, MODE_PRIVATE);
        mIsCleanApp = prefs.getBoolean(Util.IS_CLEAN_LAUNCH, true);
    }

}
