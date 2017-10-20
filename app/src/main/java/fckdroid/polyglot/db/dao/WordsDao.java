package fckdroid.polyglot.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import fckdroid.polyglot.db.entity.WordEntity;
import io.reactivex.Single;

@Dao
public interface WordsDao {
    @Query("SELECT * FROM words ORDER BY RANDOM() WHERE level = :levelId LIMIT 1")
    Single<WordEntity> loadNextWord(long levelId);
}
