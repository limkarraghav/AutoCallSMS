package com.example.autocallsms;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AppDatabase.User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "autocallsms_users.db"
                    ).build();
                }
            }
        }
        return instance;
    }

    @Entity(tableName = "users")
    public static class User {

        @PrimaryKey
        @NonNull
        public String username;

        public String password;

        public User(@NonNull String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    @Dao
    public interface UserDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(User user);

        @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
        User getUserByUsername(String username);
    }
}