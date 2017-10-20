package fckdroid.polyglot.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;

import fckdroid.polyglot.model.Level;

@Entity(tableName = "levels",
        primaryKeys = "id")
public class LevelEntity implements Level {
    private long id;
    private String label;
    private int rate;
    @ColumnInfo(name = "min_score")
    private int minScore;
    @ColumnInfo(name = "max_score")
    private int maxScore;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    @Override
    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }

    @Override
    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }
}
