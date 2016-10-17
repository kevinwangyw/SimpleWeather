package com.kevinwang.simpleweather.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.kevinwang.simpleweather.db.WeatherDBHelper;

public class ContentData {
    public static final String PREFIX = "content://";
    public static final String AUTHORITY = "com.kevinwang.simpleweather.helper.weathercontentprovider";
    public static final Uri CONTENT_URI = Uri.parse(PREFIX + AUTHORITY +  "/" + WeatherDBHelper.TABLE_NAME);
    //匹配码
    public static final int CITYWEATHER = 1;

    public static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, WeatherDBHelper.TABLE_NAME, CITYWEATHER);
    }
}
