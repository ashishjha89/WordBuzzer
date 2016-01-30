package com.ashishjha.wordbuzzer.utils;

import java.util.Random;

/**
 * Created by ashish on 30/1/16.
 */
public class Util {

    public static final String TEXT_ENGLISH = "text_eng";

    public static final String TEXT_SPA = "text_spa";

    public static final String WORD_POSITION = "word_position";

    public static final String DATABASE_NAME = "word_buzzer_db";

    public static final int DATABASE_VERSION = 1;

    public static final String WORD_BUZZER_PREFS = "ord_buzzer_prefs";

    public static final String IS_CLEAN_LAUNCH = "is_clean_launch";

    public static final String WORDS_COUNT_IN_DICTIONARY = "words_count_in_dirctionary";

    public static final String LOAD_DICTIONARY = "load_dictionary";

    public static final String LOAD_DICTIONARY_COMPLETE = "load_dictionary_complete";

    public static final int MAX_TRANSLATIONS_IN_MEMORY = 100;

    public static final String BUZZER_SCORE_1 = "buzzer_score_1";

    public static final String BUZZER_SCORE_2 = "buzzer_score_2";

    public static final String BUZZER_SCORE_3 = "buzzer_score_3";

    public static final String BUZZER_SCORE_4 = "buzzer_score_4";


    public static int getRandomNumber(int startNo, int lastNo) {
        Random random = new Random();
        return random.nextInt(lastNo - startNo) + startNo;
    }
}
