package fckdroid.polyglot.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import fckdroid.polyglot.db.entity.UserEntity;
import io.reactivex.Single;

@Dao
public interface UsersDao {
    @Query("SELECT * FROM users")
    Single<UserEntity> loadUser();

    @Update
    void updateUser(UserEntity user);
}
