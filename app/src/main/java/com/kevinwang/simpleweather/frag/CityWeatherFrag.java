package com.kevinwang.simpleweather.frag;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kevinwang.simpleweather.Bean.WeatherData;
import com.kevinwang.simpleweather.R;
import com.kevinwang.simpleweather.activity.MainActivity;
import com.kevinwang.simpleweather.db.WeatherDBHelper;
import com.kevinwang.simpleweather.provider.ContentData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CityWeatherFrag extends Fragment implements View.OnClickListener {
    public static final String CITYNAME = "cityname";
    public static final String FIRSTCREATE = "firstcreate";
    public static final String CITY_WEATHER_FRAG = "CityWeatherFrag";
    private ListView mListView;
    private ImageView mWeather_bkg_img;
    private ImageView mRefresh_bth;
    private ImageView mWeather_img;
    private TextView mCur_temperature;
    private TextView mCur_Weather;
    private TextView mWind;
    private TextView mAir_quality;
    private TextView mUpdateTime;
    private TextView mCity;
    private Context mContext;
    private String mCity_name;
    private SharedPreferences mSharedPreferences;
    private boolean mFisrtCreate;
    private ArrayList<String> mDailyWeather;
    private ListAdapter mListAdapter;
    private Animation mOperatingAnim;
    private LinearInterpolator mLin;

    //private final Context mContext;
    public static CityWeatherFrag newFragment(String city_name, boolean firstCreate) {
        CityWeatherFrag frag = new CityWeatherFrag();
        Bundle args = new Bundle();
        args.putString(CITYNAME, city_name);
        args.putBoolean(FIRSTCREATE, firstCreate);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(CITY_WEATHER_FRAG, "onCreate()");
        mDailyWeather = new ArrayList<String>();
        mContext = getContext();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        Log.i(CITY_WEATHER_FRAG, "onCreateView()");
        View view = inflater.inflate(R.layout.frag_layout, container, false);

        mRefresh_bth = (ImageView) view.findViewById(R.id.refresh_btn);
        mRefresh_bth.setOnClickListener(this);
        mOperatingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
        mLin = new LinearInterpolator();
        mOperatingAnim.setInterpolator(mLin);
        //setInterpolator表示设置旋转速率。LinearInterpolator为匀速效果，
        // Accelerateinterpolator为加速效果、DecelerateInterpolator为减速效果

        mWeather_img = (ImageView) view.findViewById(R.id.weather_image);

        mCur_temperature = (TextView) view.findViewById(R.id.cur_temperature_text);
        mCur_Weather = (TextView) view.findViewById(R.id.cur_weather_text);
        mWind = (TextView) view.findViewById(R.id.wind_text);
        mAir_quality = (TextView) view.findViewById(R.id.air_quality_text);
        mUpdateTime = (TextView) view.findViewById(R.id.update_time_text);
        mCity = (TextView) view.findViewById(R.id.city_text);

        mSharedPreferences = getActivity().getSharedPreferences(MainActivity.SHARE_PREF_FILE_NAME, Context
                .MODE_PRIVATE);
        Bundle arguments = getArguments();
        mCity_name = arguments.getString(CITYNAME);
        mFisrtCreate = arguments.getBoolean(FIRSTCREATE);

        if (!mFisrtCreate) {
            Log.i(CITY_WEATHER_FRAG, "onCreateView()--->获取数据库中的天气数据");
            String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
            Uri uri = Uri.parse(uriStr);
            mCity.setText(mCity_name);
            String[] projection = new String[]{"aqi", "updateTime", "weather", "wind", "temperature", "today",
                    "next1th", "next2th", "next3th", "next4th"};
            String selection = "city=?";
            String[] selectionArgs = new String[]{mCity_name};
            Log.i(CITY_WEATHER_FRAG, "oncreteView -- > mFirstCreate == false: the query city name = " + mCity_name);
            Cursor cursor = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            Log.i(CITY_WEATHER_FRAG, "oncreteView -- > mFirstCreate == false: cursor.getCount() = " + cursor.getCount());
            cursor.moveToFirst();
            mAir_quality.setText("空气质量 " + cursor.getString(0));
            mUpdateTime.setText(cursor.getString(1) + " 发布");
            mCur_Weather.setText(cursor.getString(2));
            mWind.setText(cursor.getString(3));
            mCur_temperature.setText(cursor.getString(4));
            String weather = cursor.getString(2);
            setWeatherImag(weather);
            mDailyWeather.add(cursor.getString(5));
            mDailyWeather.add(cursor.getString(6));
            mDailyWeather.add(cursor.getString(7));
            mDailyWeather.add(cursor.getString(8));
            mDailyWeather.add(cursor.getString(9));
        }

        if (mFisrtCreate) {
            if (MainActivity.isNetworkAvailable(mContext)) {  //网络状态良好
                UpdateWeather updateWeather = new UpdateWeather();
                updateWeather.execute(mCity_name);
            } else {  //无法连接到网络
                Toast.makeText(mContext, "无法获取网络数据", Toast.LENGTH_SHORT).show();
                insertNewCity(mCity_name, Uri.parse(ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME));
                mFisrtCreate = false;
            }

            Log.i(CITY_WEATHER_FRAG, "onCreate(): mFirstCreate == true, 插入新城市");
            arguments.putBoolean(FIRSTCREATE, false);
            //防止FragmentStatePagerAdapter在新建已经插入的fragment的时候插入新的数据
        }

        mListView = (ListView) view.findViewById(R.id.weather_info_list);
        mListAdapter = new ListAdapter();
        mListView.setAdapter(mListAdapter);

        return view;
    }

    private void setWeatherImag(String weather) {
        switch (weather) {
            case "晴":
                mWeather_img.setImageResource(R.drawable.sunny);
                break;
            case "多云":
                mWeather_img.setImageResource(R.drawable.cloudy);
                break;
            case "少云":
                mWeather_img.setImageResource(R.drawable.few_cloudy);
                break;
            case "晴间多云":
                mWeather_img.setImageResource(R.drawable.partly_cloudy);
                break;
            case "阴":
                mWeather_img.setImageResource(R.drawable.cloudy);
                break;
            case "有风":
            case "微风":
            case "和风":
            case "清风":
            case "强风/劲风":
            case "疾风":
            case "大风":
            case "烈风":
                mWeather_img.setImageResource(R.drawable.windy);
                break;
            case "风暴":
            case "狂暴风":
                mWeather_img.setImageResource(R.drawable.tropical_storm);
                break;
            case "阵雨":
                mWeather_img.setImageResource(R.drawable.shower_rain);
                break;
            case "强阵雨":
                mWeather_img.setImageResource(R.drawable.shower_rain);
                break;
            case "雷阵雨":
            case "强雷阵雨":
                mWeather_img.setImageResource(R.drawable.thunder_rain);
                break;
            case "小雨":
                mWeather_img.setImageResource(R.drawable.light_rain);
                break;
            case "中雨":
                mWeather_img.setImageResource(R.drawable.moderate_rain);
                break;
            case "大雨":
                mWeather_img.setImageResource(R.drawable.heavy_rain);
                break;
            case "暴雨":
                mWeather_img.setImageResource(R.drawable.heavy_rain);
                break;
            case "大暴雨":
            case "特大暴雨":
                mWeather_img.setImageResource(R.drawable.heavy_rain);
                break;
            case "冻雨":
                mWeather_img.setImageResource(R.drawable.freezing_rain);
                break;
            case "小雪":
                mWeather_img.setImageResource(R.drawable.light_snow);
                break;
            case "中雪":
                mWeather_img.setImageResource(R.drawable.moderate_snow);
                break;
            case "大雪":
                mWeather_img.setImageResource(R.drawable.heavy_snow);
                break;
            case "暴雪":
                mWeather_img.setImageResource(R.drawable.snowstorm);
                break;
            case "雨夹雪":
                mWeather_img.setImageResource(R.drawable.rain_and_snow);
                break;
            case "雨雪天气":
                mWeather_img.setImageResource(R.drawable.rain_and_snow);
                break;
            case "阵雨夹雪":
                mWeather_img.setImageResource(R.drawable.rain_and_snow);
                break;
            case "阵雪":
                mWeather_img.setImageResource(R.drawable.light_snow);
                break;
            case "薄雾":
            case "雾":
                mWeather_img.setImageResource(R.drawable.foggy);
                break;
            case "霾":
                mWeather_img.setImageResource(R.drawable.haze);
                break;
            case "浮尘":
                mWeather_img.setImageResource(R.drawable.dust);
                break;
            case "扬沙":
                mWeather_img.setImageResource(R.drawable.dust);
                break;
            case "沙尘暴":
                mWeather_img.setImageResource(R.drawable.duststorm);
                break;
            case "强沙尘暴":
                mWeather_img.setImageResource(R.drawable.heavyduststorm);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh_btn:
                Log.i(CITY_WEATHER_FRAG, "点击更新天气按钮");
                if (MainActivity.isNetworkAvailable(mContext)) {  //网络状态良好
                    UpdateWeather updateWeather = new UpdateWeather();
                    updateWeather.execute(mCity_name);
                } else {
                    Toast.makeText(mContext, "无法获取网络数据", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    public class UpdateWeather extends AsyncTask<String, Integer, String> {
        public static final String httpUrl = "http://apis.baidu.com/heweather/pro/weather";
        String httpArg;
        private String city_name;
        private String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRefresh_bth.startAnimation(mOperatingAnim);
        }

        @Override
        protected String doInBackground(String... strings) {
            city_name = strings[0];
            httpArg = "city=" + city_name;
            result = requestData(httpUrl, httpArg);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            Gson gson = new Gson();
            WeatherData weatherData = gson.fromJson(result, WeatherData.class);
            if (TextUtils.equals(weatherData.getWeatherDataService().get(0).getStatus(), "unknown city")) {
                Toast.makeText(mContext, "无法获取该城市数据", Toast.LENGTH_SHORT).show();
                return;
            }
            String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
            Uri uri = Uri.parse(uriStr);
            if (mFisrtCreate) {
                Log.i(CITY_WEATHER_FRAG, "asynctask-->onPostExecute: 插入新的城市天气数据");
                insertNewCity(weatherData, uri);
                mFisrtCreate = false;
            } else {
                Log.i(CITY_WEATHER_FRAG, "asynctask-->onPostExecute: 更新城市天气数据");
                updateCityWeatherInfo(weatherData, uri);
            }

            WeatherData.WeatherDataServiceBean serviceBean = weatherData.getWeatherDataService().get(0);
            mCur_temperature.setText(serviceBean.getNow().getTmp() + "°");
            mCur_Weather.setText(serviceBean.getNow().getCond().getTxt());
            mWind.setText(serviceBean.getNow().getWind().getDir() + serviceBean.getNow().getWind()
                    .getSc() + "级");
            mAir_quality.setText("空气质量 " + serviceBean.getAqi().getCity().getQlty());
            mUpdateTime.setText(serviceBean.getBasic().getUpdate().getLoc().substring(5) + " 发布");
            mCity_name = serviceBean.getBasic().getCity();
            mCity.setText(mCity_name);

            String weather = serviceBean.getNow().getCond().getTxt();
            setWeatherImag(weather);

            mRefresh_bth.clearAnimation();
        }
    }

    private void updateCityWeatherInfo(WeatherData weatherData, Uri uri) {
        WeatherData.WeatherDataServiceBean serviceBean = weatherData.getWeatherDataService().get(0);
        List<WeatherData.WeatherDataServiceBean.DailyForecastBean> dailyForecastBean = serviceBean
                .getDaily_forecast();

        ContentValues contentValues = new ContentValues();

        initialContentValue(serviceBean, dailyForecastBean, contentValues);

        String selection = "city=?";
        String[] selectionArgs = new String[]{mCity_name};

        mContext.getContentResolver().update(uri, contentValues, selection, selectionArgs);

        mDailyWeather.set(0, contentValues.getAsString("today"));
        mDailyWeather.set(1, contentValues.getAsString("next1th"));
        mDailyWeather.set(2, contentValues.getAsString("next2th"));
        mDailyWeather.set(3, contentValues.getAsString("next3th"));
        mDailyWeather.set(4, contentValues.getAsString("next4th"));
        mListAdapter.notifyDataSetChanged();
        mListView.setAdapter(mListAdapter);
    }

    private void insertNewCity(String cityName, Uri uri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("city", cityName);
        contentValues.put("aqi", "未知");
        contentValues.put("updateTime", "未知");
        contentValues.put("weather", "未知");
        contentValues.put("temperature", "未知");
        contentValues.put("wind", "未知");

        contentValues.put("comf", "未知");
        contentValues.put("cw", "未知");
        contentValues.put("drsg", "未知");
        contentValues.put("flu","未知" );
        contentValues.put("sport", "未知");
        contentValues.put("uv", "未知");

        contentValues.put("today", "");
        contentValues.put("next1th", "");
        contentValues.put("next2th", "");
        contentValues.put("next3th", "");
        contentValues.put("next4th", "");

        mDailyWeather.add(contentValues.getAsString(""));
        mDailyWeather.add(contentValues.getAsString(""));
        mDailyWeather.add(contentValues.getAsString(""));
        mDailyWeather.add(contentValues.getAsString(""));
        mDailyWeather.add(contentValues.getAsString(""));

        mContext.getContentResolver().insert(uri, contentValues);
    }

    private void insertNewCity(WeatherData weatherData, Uri uri) {
        WeatherData.WeatherDataServiceBean serviceBean = weatherData.getWeatherDataService().get(0);
        List<WeatherData.WeatherDataServiceBean.DailyForecastBean> dailyForecastBean = serviceBean
                .getDaily_forecast();

        ContentValues contentValues = new ContentValues();

        initialContentValue(serviceBean, dailyForecastBean, contentValues);

        mDailyWeather.add(contentValues.getAsString("today"));
        mDailyWeather.add(contentValues.getAsString("next1th"));
        mDailyWeather.add(contentValues.getAsString("next2th"));
        mDailyWeather.add(contentValues.getAsString("next3th"));
        mDailyWeather.add(contentValues.getAsString("next4th"));
        mListAdapter.notifyDataSetChanged();

        Uri uri_tmp = mContext.getContentResolver().insert(uri, contentValues);
        Log.i("insertNewCity", "the return uri is " + uri_tmp);
    }

    private void initialContentValue(WeatherData.WeatherDataServiceBean serviceBean, List<WeatherData
            .WeatherDataServiceBean.DailyForecastBean> dailyForecastBean, ContentValues contentValues) {
        contentValues.put("city", serviceBean.getBasic().getCity());
        contentValues.put("aqi", serviceBean.getAqi().getCity().getQlty());
        contentValues.put("updateTime", serviceBean.getBasic().getUpdate().getLoc().substring(5));
        contentValues.put("weather", serviceBean.getNow().getCond().getTxt());
        contentValues.put("temperature", serviceBean.getNow().getTmp() + "°");
        contentValues.put("wind", serviceBean.getNow().getWind().getDir() + serviceBean.getNow().getWind()
                .getSc() + "级");

        contentValues.put("comf", serviceBean.getSuggestion().getComf().getTxt());
        contentValues.put("cw", serviceBean.getSuggestion().getCw().getTxt());
        contentValues.put("drsg", serviceBean.getSuggestion().getDrsg().getTxt());
        contentValues.put("flu", serviceBean.getSuggestion().getFlu().getTxt());
        contentValues.put("sport", serviceBean.getSuggestion().getSport().getTxt());
        contentValues.put("uv", serviceBean.getSuggestion().getUv().getTxt());

        contentValues.put("today", getDailyForecast(dailyForecastBean, 0));
        contentValues.put("next1th", getDailyForecast(dailyForecastBean, 1));
        contentValues.put("next2th", getDailyForecast(dailyForecastBean, 2));
        contentValues.put("next3th", getDailyForecast(dailyForecastBean, 3));
        contentValues.put("next4th", getDailyForecast(dailyForecastBean, 4));
    }

    private String getDailyForecast(List<WeatherData.WeatherDataServiceBean.DailyForecastBean> dailyForecastBean, int
            day) {
        String weather_d = dailyForecastBean.get(day).getCond().getTxt_d();
        String weather_n = dailyForecastBean.get(day).getCond().getTxt_n();
        //利用;分割开Date, weather, temperature
        String dailyForecastStr = dailyForecastBean.get(day).getDate().substring(5) + ";" +
                (TextUtils.equals(weather_d, weather_n) ? weather_d : (weather_d + "转" + weather_n)) + ";" +
                dailyForecastBean.get(day).getTmp().getMin() + "~" + dailyForecastBean.get(day).getTmp().getMax() + "°";
        return dailyForecastStr;
    }

    /**
     * @param httpUrl   :请求接口
     * @param city_name :参数
     * @return 返回结果
     */
    public static String requestData(String httpUrl, String city_name) {
        BufferedReader reader = null;
        String result = null;
        StringBuffer sbf = new StringBuffer();
        httpUrl = httpUrl + "?" + city_name;

        try {
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            // 填入apikey到HTTP header
            connection.setRequestProperty("apikey", "248873392f24dfe0558da6f773a65f72");
            connection.connect();
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    class ListAdapter extends BaseAdapter {
        LayoutInflater mLayoutInflater = (LayoutInflater) getContext().getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        ViewHolder mViewHolder;

        public ListAdapter() {
            mViewHolder = new ViewHolder();
        }

        @Override
        public int getCount() {
            return mDailyWeather.size();
        }

        @Override
        public Object getItem(int i) {
            return mDailyWeather.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.list_item, null);
                mViewHolder.date_text = (TextView) view.findViewById(R.id.date_text);
                mViewHolder.weather_text = (TextView) view.findViewById(R.id.weather_text);
                mViewHolder.temperature_text = (TextView) view.findViewById(R.id.temperature_text);
                view.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) view.getTag();
            }

            if (!mFisrtCreate) {
                if (TextUtils.isEmpty(mDailyWeather.get(i))) {
                    mViewHolder.date_text.setText("");
                    mViewHolder.weather_text.setText("");
                    mViewHolder.temperature_text.setText("");
                } else {
                    String[] daily_forecast = mDailyWeather.get(i).split(";");
                    mViewHolder.date_text.setText(daily_forecast[0]);
                    mViewHolder.weather_text.setText(daily_forecast[1]);
                    mViewHolder.temperature_text.setText(daily_forecast[2]);
                }

            }

            return view;
        }

        class ViewHolder {
            public TextView date_text;
            public TextView weather_text;
            public TextView temperature_text;
        }
    }
}
