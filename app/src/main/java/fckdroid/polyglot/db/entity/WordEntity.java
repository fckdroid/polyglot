package fckdroid.polyglot.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;

import fckdroid.polyglot.model.Word;

@Entity(tableName = "words",
        primaryKeys = "id",
        foreignKeys = {
                @ForeignKey(entity = LevelEntity.class,
                        parentColumns = "id",
                        childColumns = "level",
                        onUpdate = ForeignKey.CASCADE,
                        onDelete = ForeignKey.CASCADE)
        },
        indices = @Index(value = "level"))
public class WordEntity implements Word {
    private long id;
    private String word;
    private String grammar;
    private String hint;
    private String translation;
    private long level;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    @Override
    public String getGrammar() {
        return grammar;
    }

    public void setGrammar(String grammar) {
        this.grammar = grammar;
    }

    @Override
    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    @Override
    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }
}
