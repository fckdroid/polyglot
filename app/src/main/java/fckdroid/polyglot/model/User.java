package fckdroid.polyglot.model;


public interface User {
    long getId();
    int getScore();
    long getLevel();
    void onHintClick(int wordRate);
    void onWrongAnswer(int wordRate);
}
