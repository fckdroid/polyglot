package fckdroid.polyglot.model;


public interface User {
    long getId();
    int getScore();
    long getLevel();
    void onSkipWord(int wordRate);
    boolean onWrongAnswer(int wordRate, Level prevLevel);
    boolean onRightAnswer(int wordRate, Level nextLevel);
}
