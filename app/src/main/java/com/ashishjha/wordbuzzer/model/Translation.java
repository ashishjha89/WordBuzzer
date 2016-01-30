package com.ashishjha.wordbuzzer.model;

/**
 * Created by ashish on 30/1/16.
 */
/*
* Model representing single translation from language "one" to language "two"
* */
public class Translation {

    private String mWordInLanguageOne, mWordInLanguageTwo;

    public Translation(String languageOne, String languageTwo) {
        mWordInLanguageOne = languageOne;
        mWordInLanguageTwo = languageTwo;
    }

    public String getWordInLanguageOne() {
        return mWordInLanguageOne;
    }

    public String getWordInLanguageTwo() {
        return mWordInLanguageTwo;
    }
}
