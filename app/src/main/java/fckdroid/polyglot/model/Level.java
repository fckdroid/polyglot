package fckdroid.polyglot.model;


public interface Level {
    long getId();
    String getLabel();
    int getRate();
    int getMinScore();
    int getMaxScore();
    void onHintClick();
}
