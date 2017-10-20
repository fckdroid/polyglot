package fckdroid.polyglot.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import fckdroid.polyglot.db.entity.LevelEntity;
import io.reactivex.Single;

@Dao
public interface LevelsDao {
    @Query("SELECT * FROM levels WHERE id = :level")
    Single<LevelEntity> loadLevelById(long level);
}
