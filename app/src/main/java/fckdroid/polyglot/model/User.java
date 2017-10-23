package fckdroid.polyglot.model;


import android.support.annotation.Nullable;

public interface User {
    long getId();
    int getScore();
    long getLevel();

    /**
     * @return true if level has been changed, false otherwise.
     */
    boolean onSkipWord(int wordRate, @Nullable Level prevLevel);

    /**
     * @return true if level has been changed, false otherwise.
     */
    boolean onWrongAnswer(int wordRate, @Nullable Level prevLevel);

    /**
     * @return true if level has been changed, false otherwise.
     */
    boolean onRightAnswer(int wordRate, @Nullable Level nextLevel);
}
