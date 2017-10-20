package fckdroid.polyglot.model;


public interface Word {
    long getId();
    String getWord();
    String getGrammar();
    String getHint();
    String getTranslation();
    long getLevel();
}
