package com.kevinwang.simpleweather.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WeatherDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "weather.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "Weather";
    String create_sql;

    public WeatherDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        create_sql = "CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY autoincrement,city VARCHAR(15),aqi VARCHAR(10),updateTime TEXT,weather VARCHAR(10)," +
                "temperature VARCHAR(10),wind TEXT,comf TEXT,cw TEXT,drsg TEXT,flu TEXT,sport TEXT,uv TEXT," +
                "today Text,next1th TEXT,next2th TEXT,next3th TEXT,next4th TEXT)";
        sqLiteDatabase.execSQL(create_sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DB_NAME);
        onCreate(sqLiteDatabase);
    }
}
