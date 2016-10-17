package com.kevinwang.simpleweather.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kevinwang.simpleweather.db.WeatherDBHelper;

public class WeatherContentProvider extends ContentProvider {
    public static final String WEATHER_CONTENT_PROVIDER = "WeatherContentProvider";
    private WeatherDBHelper mWeatherDBHelper = null;
    private UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        mUriMatcher = ContentData.uriMatcher;
        mWeatherDBHelper = new WeatherDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        //strings is projection--The list of columns to put into the cursor. If null all columns are included.
        //s -- selection, s1 -- sortOrder
        //strings1 -- selectionArgs: You may include ?s in selection, which will be replaced by the values from selectionArgs
        SQLiteDatabase db = mWeatherDBHelper.getReadableDatabase();
        String[] projection = strings;
        String selection = s;
        String[] selctionArgs = strings1;
        String sortOrder = s1;
        switch (mUriMatcher.match(uri)) {
            case ContentData.CITYWEATHER:
                Log.i(WEATHER_CONTENT_PROVIDER, "查询数据库");
                return db.query(WeatherDBHelper.TABLE_NAME, projection, selection, selctionArgs, null, null, sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mWeatherDBHelper.getWritableDatabase();
        switch (mUriMatcher.match(uri)) {
            case ContentData.CITYWEATHER:
                db.beginTransaction();
                // 返回的是记录的行号，主键为int，实际上就是主键值
                long id = db.insert(WeatherDBHelper.TABLE_NAME, null, contentValues);
                db.setTransactionSuccessful();
                db.endTransaction();
                db.close();
                return ContentUris.withAppendedId(uri, id);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        SQLiteDatabase db = mWeatherDBHelper.getWritableDatabase();
        String selection = s;
        String[] selectionArgs = strings;
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case ContentData.CITYWEATHER:
                db.beginTransaction();
                count = db.delete(WeatherDBHelper.TABLE_NAME, selection, selectionArgs);
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        db.close();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        SQLiteDatabase db = mWeatherDBHelper.getWritableDatabase();
        String selection = s;
        String[] selectionArgs = strings;
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case ContentData.CITYWEATHER:
                db.beginTransaction();
                count = db.update(WeatherDBHelper.TABLE_NAME, contentValues, selection, selectionArgs);
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        db.close();
        return count;
    }
}
