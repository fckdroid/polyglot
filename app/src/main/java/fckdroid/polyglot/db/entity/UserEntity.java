package fckdroid.polyglot.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.support.annotation.Nullable;

import fckdroid.polyglot.model.Level;
import fckdroid.polyglot.model.User;

@Entity(tableName = "users",
        primaryKeys = "id",
        foreignKeys = {
                @ForeignKey(entity = LevelEntity.class,
                        parentColumns = "id",
                        childColumns = "level",
                        onUpdate = ForeignKey.CASCADE,
                        onDelete = ForeignKey.CASCADE)
        },
        indices = @Index(value = "level"))
public class UserEntity implements User {
    private long id;
    private int score;
    private long level;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    @Ignore
    @Override
    public boolean onSkipWord(int wordRate, @Nullable Level prevLevel) {
        score -= wordRate / 10;
        if (score < 0) {
            score = 0;
        }
        if (prevLevel != null && score <= prevLevel.getMaxScore()) {
            level = prevLevel.getId();
            return true;
        }
        return false;
    }


    @Ignore
    @Override
    public boolean onWrongAnswer(int wordRate, @Nullable Level prevLevel) {
        score -= wordRate;
        if (score < 0) {
            score = 0;
            return false;
        }
        if (prevLevel != null && score <= prevLevel.getMaxScore()) {
            level = prevLevel.getId();
            return true;
        }
        return false;
    }


    @Ignore
    @Override
    public boolean onRightAnswer(int wordRate, @Nullable Level nextLevel) {
        score += wordRate;
        if (nextLevel != null && score >= nextLevel.getMinScore()) {
            level = nextLevel.getId();
            return true;
        }
        return false;
    }
}
