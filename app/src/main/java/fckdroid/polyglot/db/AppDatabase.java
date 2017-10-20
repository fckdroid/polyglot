package fckdroid.polyglot.db;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import fckdroid.polyglot.db.dao.LevelsDao;
import fckdroid.polyglot.db.dao.UsersDao;
import fckdroid.polyglot.db.dao.WordsDao;
import fckdroid.polyglot.db.entity.LevelEntity;
import fckdroid.polyglot.db.entity.UserEntity;
import fckdroid.polyglot.db.entity.WordEntity;
import fckdroid.polyglot.util.sqlite_asset.AssetSQLiteOpenHelperFactory;

@Database(entities = {LevelEntity.class, UserEntity.class, WordEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    static final String DATABASE_NAME = "polyglot-app.db";
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            .openHelperFactory(new AssetSQLiteOpenHelperFactory())
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract UsersDao usersDao();
    public abstract WordsDao wordsDao();
    public abstract LevelsDao levelsDao();
}
