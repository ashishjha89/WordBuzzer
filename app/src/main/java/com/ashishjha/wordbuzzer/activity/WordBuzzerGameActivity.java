package com.ashishjha.wordbuzzer.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.ashishjha.wordbuzzer.R;
import com.ashishjha.wordbuzzer.services.WordLoaderService;
import com.ashishjha.wordbuzzer.utils.Util;

/**
 * Created by ashish on 30/1/16.
 */
/*
* This activity representing the Game.
*
* The game screen contains button in each corners of the screen - one for each possible player.
*
* There will be a word in language "one“ in the middle of the screen.
* While this word is displayed, a word in language "two“ will fade in from left (or right) border (alternatively) of the screen,
* move over the screen and then fade out again within 2 seconds. After one word is faded out, a new word is faded in (from opposite direction).
* The players hit their buzzer when they think the correct translation set is presented.
*
* The first one hitting his button will get score incremented and score is display as text on button. Wrong answer decrements score.
* No answer keeps score unchanged.
*
* The correct words are arranged randomly at a frequency of 20% (but placed randomly in the sequence).
*/
public class WordBuzzerGameActivity extends AppCompatActivity implements WordLoaderService.QuizChangeListener {

    // Buttons representing four players at four corners of screen
    private Button mBuzzer1, mBuzzer2, mBuzzer3, mBuzzer4;

    // Score displayed on button. Correct answer increment score while wrong answer decrement. No answer keeps score unchanged.
    private int mScore1, mScore2, mScore3, mScore4;

    // The words (language "two") will fade in the screen alternatively left-to-right and right-to-left
    private boolean mIsLeftToRightTransitionInProgress;

    // Represents if word transition animation should continue.
    // For example, there should be no animation while user has clicked on buzzer and button animation is playing
    private boolean mIsGameInProgress;

    // Click Listener for buzzer button click. Correct Answer gives button green color animation with "win" sound.
    // Wrong answer gives red color animation with "loose" sound.
    // Also, new words (language "one" and "two") needs to be loaded. New set of random words to be shown also needs to be loaded.
    private OnBuzzerClickListener mClickListener;

    // Animation Listener for transition of language "two" words. Once animation ends, the direction of transition must be reverse from previous.
    private WordTransitionAnimationListener mWordAnimationListener;

    private Context mContext;

    // To represent transition animations alternatively from left-to-right and right-to-left, two TextViews are saved (left-aligned and right-aligned)
    private TextView mLeftToRightTV, mRightToLeftTV;

    // The TextView representing challenge (word in language "one")
    private TextView mQuizTV;

    // Service with which this activity binds. The service is responsible for loading random words in background (50 at a time).
    // Service also finds next "challenge" (language "one" word) and its associated correct answer.
    // Service notifies Activity through QuizChangeListener (defined in the service) which is implemented by this activity.
    // Activity gets its required members through instance of service fetched during "onServiceConnected" callback.
    private WordLoaderService mWordLoaderService;

    // Stores array of random words. WordLoaderService provides about 50 random words which can be shown as options to players.
    // Only 50 words are fetched in service as query in DB is done through random "word positions" stored in DB by using "IN" operator
    // which is slow for large number of arguments
    private String[] mRandomWords;

    // It holds the current index of the String displayed as option to players from mRandomWords
    private int mCurrentRandomWordIndex;

    // Stores correct translation (in language "two" for language "one")
    private String mCorrectAnswer;

    // Stores the current challenge String (language "one" displayed in center of screen")
    private String mCurrentChallenge;

    // Stores current String which is displayed to players. This variable is useful for comparing equality with mCorrectAnswer.
    private String mCurrentOptionInString;

    // Is this activity bound to WordLoaderService
    private boolean mBound = false;

    private WordBuzzerGameActivity mSelf;

    // Display ProgressDialog while data is fetched using background thread from WordLoaderService
    private ProgressDialog mLoadingProgressDialog;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to WordLoaderService, cast the IBinder and get WordLoaderService instance
            WordLoaderService.WordLoaderBinder binder = (WordLoaderService.WordLoaderBinder) service;
            mWordLoaderService = binder.getService();
            // Notify WordLoaderService that this Activity implements QuizChangeListener. Service uses this listener to notify Activity (callback)
            mWordLoaderService.setQuizChangeListener(mSelf);
            // Initiate loading of 50 random words. Finding next random challenge and its correct translation.
            // Activity is notified through QuizChangeListener callback
            mWordLoaderService.initGameWithWords();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_buzzer_activity);

        mContext = this;
        mSelf = this;

        mQuizTV = (TextView) findViewById(R.id.quiz_word);
        mLeftToRightTV = (TextView) findViewById(R.id.quiz_sol_top);
        mRightToLeftTV = (TextView) findViewById(R.id.quiz_sol_bottom);

        mBuzzer1 = (Button) findViewById(R.id.buzzer_1);
        mBuzzer2 = (Button) findViewById(R.id.buzzer_2);
        mBuzzer3 = (Button) findViewById(R.id.buzzer_3);
        mBuzzer4 = (Button) findViewById(R.id.buzzer_4);

        mClickListener = new OnBuzzerClickListener();
        mWordAnimationListener = new WordTransitionAnimationListener();

        mBuzzer1.setOnClickListener(mClickListener);
        mBuzzer2.setOnClickListener(mClickListener);
        mBuzzer3.setOnClickListener(mClickListener);
        mBuzzer4.setOnClickListener(mClickListener);

        if (savedInstanceState != null) {
            // Currently we are saving and restoring only the scores
            // TODO - We can extend it later to save and restore "challenge" and associated random word list and correct answer
            mScore1 = savedInstanceState.getInt(Util.BUZZER_SCORE_1);
            mScore2 = savedInstanceState.getInt(Util.BUZZER_SCORE_2);
            mScore3 = savedInstanceState.getInt(Util.BUZZER_SCORE_3);
            mScore4 = savedInstanceState.getInt(Util.BUZZER_SCORE_4);
        }

        mBuzzer1.setText(mScore1 + "");
        mBuzzer2.setText(mScore2 + "");
        mBuzzer3.setText(mScore3 + "");
        mBuzzer4.setText(mScore4 + "");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO - Currently we reset game whenever Activity enters onStart state. We need to extend it to save and restore entire previous game state.
        clearAllWordsTransition();
        if (mLoadingProgressDialog == null) {
            mLoadingProgressDialog = new ProgressDialog(this);
        }
        mLoadingProgressDialog.setCancelable(false);
        mLoadingProgressDialog.show();
        // Bind to the service
        Intent intent = new Intent(this, WordLoaderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mCurrentRandomWordIndex = 0;
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearAllWordsTransition();
        mIsGameInProgress = false;
        // Dismiss Progress Dialog if visible
        if (mLoadingProgressDialog != null && mLoadingProgressDialog.isShowing()) {
            mLoadingProgressDialog.dismiss();
        }
        if (mBound) {
            // Unbind from service
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Util.BUZZER_SCORE_1, mScore1);
        outState.putInt(Util.BUZZER_SCORE_2, mScore2);
        outState.putInt(Util.BUZZER_SCORE_3, mScore3);
        outState.putInt(Util.BUZZER_SCORE_4, mScore4);
    }

    /*
    * This callback is called from WordLoaderService when reset state is called (WordLoaderService.initGameWithWords).
    * This is the first callback called as a result of call to the WordLoaderService.initGameWithWords.
    * String[] randomWords - array of about 50 random words which can be shown as options to user
    */
    @Override
    public void setNextRandomWords(String[] randomWords) {
        mCurrentRandomWordIndex = 0;
        mRandomWords = randomWords;
    }

    /*
    * This method is called after call to setNextRandomWords().
    * String nextChallenge - The challenge (language "one" quiz) to be displayed at center of screen
    */
    @Override
    public void setNextChallenge(String nextChallenge) {
        mCurrentChallenge = nextChallenge;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Set Current Challenge Word
                mQuizTV.setText(mCurrentChallenge);
            }
        });
    }

    /*
    * This method is called after call to setNextChallenge().
    * String correctAnswer - The correct translation (in language "two") of the "challenge" displayed in center of screen (in language "one"
    */
    @Override
    public void setCorrectAnswer(String correctAnswer) {
        mCorrectAnswer = correctAnswer;
        // Update random word list by inserting correct answer at random positions
        updateListWithCorrectAnswers();
        // Send request to service to prepare next list of random words
        // for the same challenge
        mWordLoaderService.getNextRandomWords();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Dismiss Progress Dialog
                if (mLoadingProgressDialog != null && mLoadingProgressDialog.isShowing()) {
                    mLoadingProgressDialog.dismiss();
                }
                // Start animations - word transition animations since we have now all the data to start the game.
                mIsGameInProgress = true;
                showNextOptionForQuiz();
            }
        });
    }

    /*
    * Inserts correct answer at frequency of 20% in the  mRandomWords[] (list of random words) obtained from service.
    * The placement of correct answer is also based on random order.
    * Therefore as a result of this array - mRandomWords is extended to included correct answers at frequency of 20% at random position.
    */
    private void updateListWithCorrectAnswers() {
        // 20% of random words should be correct answers
        int noOfEntryToAddForCorrect = mRandomWords.length / 5;
        int newSize = mRandomWords.length + noOfEntryToAddForCorrect;
        String[] newRandomStr = new String[newSize];
        for (int i = 0; i < mRandomWords.length; i++) {
            newRandomStr[i] = mRandomWords[i];
        }
        int indexForAdding = mRandomWords.length;
        for (int i = 0; i < noOfEntryToAddForCorrect; i++) {
            int randomIndex = Util.getRandomNumber(0, mRandomWords.length - 1);
            insertCorrectAnswer(newRandomStr, randomIndex, indexForAdding++);
        }
        mCurrentRandomWordIndex = 0;
        mRandomWords = newRandomStr;
    }

    private void insertCorrectAnswer(String[] newRandomStr, int x, int y) {
        newRandomStr[y] = newRandomStr[x];
        newRandomStr[x] = mCorrectAnswer;
    }

    private void clearAllWordsTransition() {
        mLeftToRightTV.clearAnimation();
        mRightToLeftTV.clearAnimation();
    }

    class OnBuzzerClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v == null) {
                return;
            }
            showBuzzerResponseAnimation(v);
        }
    }

    class WordTransitionAnimationListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            showNextOptionForQuiz();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    /* Responsible for setting correct or wrong animation on the buzzer */
    private void showBuzzerResponseAnimation(View v) {
        mIsGameInProgress = false;
        clearAllWordsTransition();
        setAnimationBasedOnAnswer(v);
    }

    private void setAnimationBasedOnAnswer(final View view) {
        boolean isCorrect = mCurrentOptionInString.equals(mCorrectAnswer);
        if (isCorrect) {
            setViewBackground(view, R.drawable.correct_answer_drawable);
        } else {
            setViewBackground(view, R.drawable.wrong_answer_drawable);
        }
        Animation anim = AnimationUtils.loadAnimation(WordBuzzerGameActivity.this,
                R.anim.ans_anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                boolean isCorrect = mCurrentOptionInString.equals(mCorrectAnswer);
                playSoundForAnswer(isCorrect);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                boolean isCorrect = mCurrentOptionInString.equals(mCorrectAnswer);
                switch (view.getId()) {
                    case R.id.buzzer_1:
                        setViewBackground(view, R.drawable.buzzer_1_drawable);
                        if (isCorrect) {
                            mScore1++;
                        } else {
                            mScore1--;
                        }
                        mBuzzer1.setText(mScore1 + "");
                        break;
                    case R.id.buzzer_2:
                        setViewBackground(view, R.drawable.buzzer_2_drawable);
                        if (isCorrect) {
                            mScore2++;
                        } else {
                            mScore2--;
                        }
                        mBuzzer2.setText(mScore2 + "");
                        break;
                    case R.id.buzzer_3:
                        setViewBackground(view, R.drawable.buzzer_3_drawable);
                        if (isCorrect) {
                            mScore3++;
                        } else {
                            mScore3--;
                        }
                        mBuzzer3.setText(mScore3 + "");
                        break;
                    case R.id.buzzer_4:
                        setViewBackground(view, R.drawable.buzzer_4_drawable);
                        if (isCorrect) {
                            mScore4++;
                        } else {
                            mScore4--;
                        }
                        mBuzzer4.setText(mScore4 + "");
                        break;
                }

                // Refresh all data
                mWordLoaderService.initGameWithWords();

                // mLoadingProgressDialog = ProgressDialog.show(mContext, "Loading...", "", true);
                // mLoadingProgressDialog.setCancelable(false);
                // mLoadingProgressDialog.show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(anim);
    }

    /*
    * Responsible for showing next word option (word in language "two").
    */
    private void showNextOptionForQuiz() {
        if (mIsGameInProgress) {
            if (mCurrentRandomWordIndex == mRandomWords.length) {
                // All previous random word list finished. Fetch new random word list from service.
                mRandomWords = mWordLoaderService.getRandomWords();
                updateListWithCorrectAnswers();
            }
            mCurrentOptionInString = mRandomWords[mCurrentRandomWordIndex];
            startWordTransitionAnimation();
        }
    }

    /*
    * Responsible for controlling direction of transition animation for word option (word in language "two").
    */
    private void startWordTransitionAnimation() {
        if (mIsLeftToRightTransitionInProgress) {
            // previous transition was "left to right". Now start "right to left"
            mIsLeftToRightTransitionInProgress = false;
            mLeftToRightTV.setVisibility(View.GONE);
            mRightToLeftTV.setVisibility(View.VISIBLE);

            mRightToLeftTV.setText(mRandomWords[mCurrentRandomWordIndex++]);

            Animation anim = AnimationUtils.loadAnimation(WordBuzzerGameActivity.this,
                    R.anim.right_to_left);
            mRightToLeftTV.startAnimation(anim);
            anim.setAnimationListener(mWordAnimationListener);
        } else {
            // previous transition was "right to left". Now start "left to right"
            mIsLeftToRightTransitionInProgress = true;
            mRightToLeftTV.setVisibility(View.GONE);
            mLeftToRightTV.setVisibility(View.VISIBLE);

            mLeftToRightTV.setText(mRandomWords[mCurrentRandomWordIndex++]);

            Animation anim = AnimationUtils.loadAnimation(WordBuzzerGameActivity.this,
                    R.anim.left_to_right);
            mLeftToRightTV.setAnimation(anim);
            anim.setAnimationListener(mWordAnimationListener);
        }
    }

    private void playSoundForAnswer(boolean isCorrect) {
        MediaPlayer mediaPlayer;
        if (isCorrect) {
            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.correct_answer);
        } else {
            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.wrong_answer);
        }
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    private void setViewBackground(View view, int drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            view.setBackground(mContext.getResources().getDrawable(drawable, null));
        } else {
            view.setBackground(mContext.getResources().getDrawable(drawable));
        }
    }
}
