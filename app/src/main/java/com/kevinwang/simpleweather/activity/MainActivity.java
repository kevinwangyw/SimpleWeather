package com.kevinwang.simpleweather.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.kevinwang.simpleweather.R;
import com.kevinwang.simpleweather.db.WeatherDBHelper;
import com.kevinwang.simpleweather.frag.CityWeatherFrag;
import com.kevinwang.simpleweather.provider.ContentData;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String SHARE_PREF_FILE_NAME = "com.kevinwang.simpleweather.weather_preference";
    public static final String CUR_POS_IN_PAGER = "cur_pos_in_pager";
    public static final String FIRST_INSTALLED = "firstInstalled";
    public static final String CUR_CITY = "cur_city";
    public static final String MAIN_ACTIVITY = "MainActivity";
    private ViewPager mViewPager;
    private PagerAdapter mCirCleTabAdapter;
    public static ArrayList<Fragment> mFrag_list;
    public static ArrayList<String> mCityList;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ImageButton mAdd_btn;
    private ImageButton mTrend_btn;
    private ImageButton mSignal_btn;
    private static int add_btn_click_count = 0;
    private static int trend_btn_click_count = 0;
    private static int signal_btn_click_count = 0;
    private ViewStub mAdd_viewStub;
    private EditText mAddEditText;
    private Button mAdd_viewStub_btn;
    private GridView mGridView;
    public static GridAdapter mGridAdapter;
    private CirclePageIndicator mCirclePageIndicator;
    private boolean mFirstInstalled;
    private ViewStub mSignal_viewStub;
    private TextView mComf_text;
    private TextView mCw_text;
    private TextView mDrsg_text;
    private TextView mFlu_textxt;
    private TextView mSport_text;
    private TextView mUv_text;
    private ViewStub mTrend_viewStub;
    private LineChart mLine_chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(MAIN_ACTIVITY, "onCreate()");
        mCityList = new ArrayList<String>();
        mFrag_list = new ArrayList<Fragment>();
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences(SHARE_PREF_FILE_NAME, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

        mFirstInstalled = mSharedPreferences.getBoolean(FIRST_INSTALLED, true);
        if (mFirstInstalled) {
            mEditor.putInt(CUR_POS_IN_PAGER, 0).commit();
            mEditor.putString(CUR_CITY, "北京").commit();
            mFrag_list.add(CityWeatherFrag.newFragment("北京", true));
            Log.i(MAIN_ACTIVITY, "onCreate()--->添加新城市");
            mCityList.add("北京");
            mEditor.putBoolean(FIRST_INSTALLED, false).commit();
        } else {
            String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
            Uri uri = Uri.parse(uriStr);
            String[] projection = new String[]{"city"};
            String sortOrder = "id ASC";
            Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                mCityList.add(cursor.getString(0));
                cursor.moveToNext();
            }
            getFrag(mCityList);
        }

        mAdd_btn = (ImageButton) findViewById(R.id.add_city_btn);
        mAdd_btn.setOnClickListener(this);
        mTrend_btn = (ImageButton) findViewById(R.id.trend_btn);
        mTrend_btn.setOnClickListener(this);
        mSignal_btn = (ImageButton) findViewById(R.id.signal_btn);
        mSignal_btn.setOnClickListener(this);

        mViewPager = (ViewPager) findViewById(R.id.frag_pager);
        mCirCleTabAdapter = new PagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mCirCleTabAdapter);
        mViewPager.setCurrentItem(mSharedPreferences.getInt(CUR_POS_IN_PAGER, 0));

        mCirclePageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);

        mCirclePageIndicator.setViewPager(mViewPager);

        mAdd_viewStub = (ViewStub) findViewById(R.id.add_viewstub);
        mSignal_viewStub = (ViewStub)findViewById(R.id.signal_viewstub);
        mTrend_viewStub = (ViewStub)findViewById(R.id.trend_viewstub);
    }

    private void getFrag(ArrayList<String> cityList) {
        for (int i = 0; i < cityList.size(); i++) {
            mFrag_list.add(CityWeatherFrag.newFragment(cityList.get(i), false));
            Log.i(MAIN_ACTIVITY, "onCreate()--->获取数据库中的城市天气数据");
        }
    }

    class PagerAdapter extends FragmentStatePagerAdapter {
        FragmentManager mFragmentManager;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public Fragment getItem(int position) {
            return mFrag_list.get(position);
        }

        @Override
        public int getCount() {
            return mFrag_list.size();
        }
    }

    class GridAdapter extends BaseAdapter {
        private final Context mContext;
        LayoutInflater mLayoutInflater;
        private ViewHolder viewHolder;

        public GridAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mCityList.size();
        }

        @Override
        public Object getItem(int i) {
            return mCityList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.grid_item, null);
                viewHolder = new ViewHolder();
                viewHolder.mTextView = (TextView) view.findViewById(R.id.single_city_text);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            final int tmp_pos = i;
            viewHolder.mTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("删除城市");
                    builder.setMessage("确认删除该城市吗？");
                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.i("removeCity", "删除城市位置为：" + tmp_pos + " " + mCityList.get(tmp_pos));
                            String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper
                                    .TABLE_NAME;
                            Uri uri = Uri.parse(uriStr);
                            String selection = "city=?";
                            String[] selctionArgs = new String[]{mCityList.get(tmp_pos)};
                            getContentResolver().delete(uri, selection, selctionArgs);
                            mCityList.remove(tmp_pos);
                            mFrag_list.remove(tmp_pos);
                            mGridAdapter.notifyDataSetChanged();
                            mCirCleTabAdapter.notifyDataSetChanged();
                            int pos = mViewPager.getCurrentItem();
                            if (tmp_pos < pos) {
                                pos--;
                            } else if (tmp_pos == pos) {
                                if (pos == mCityList.size()) {  //删除的城市为当前视图城市，且位于列表最后一个
                                    pos = mCityList.size() - 1;
                                }
                            }
                            mViewPager.setAdapter(mCirCleTabAdapter);
                            mViewPager.setCurrentItem(pos);
                            mCirclePageIndicator.notifyDataSetChanged();
                            add_btn_click_count = ++add_btn_click_count % 2;
                            mAdd_viewStub.setVisibility(View.GONE);
                            mAdd_btn.setSelected(false);
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.i("removeCity", "取消删除城市位置为：" + tmp_pos + " " + mCityList.get(tmp_pos));
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return true;
                }
            });
            viewHolder.mTextView.setText(mCityList.get(i));
            return view;
        }

        class ViewHolder {
            public TextView mTextView;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_city_btn:
                if ((add_btn_click_count++) % 2 == 0) {
                    if ((trend_btn_click_count) % 2 != 0) {
                        trend_btn_click_count = 0;
                        mTrend_viewStub.setVisibility(View.GONE);
                        mTrend_btn.setSelected(false);
                    }
                    if ((signal_btn_click_count) % 2 != 0) {
                        signal_btn_click_count = 0;
                        mSignal_viewStub.setVisibility(View.GONE);
                        mSignal_btn.setSelected(false);
                    }
                    String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
                    Uri uri = Uri.parse(uriStr);
                    String[] projection = new String[]{"city"};
                    String sortOrder = "id ASC";
                    Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder);
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        mCityList.set(i, cursor.getString(0));
                        cursor.moveToNext();
                    }
                    //显示隐藏页面
                    try {
                        View add_view = mAdd_viewStub.inflate();
                        mAddEditText = (EditText) add_view.findViewById(R.id.add_city_viewstub_edit);
                        mAdd_viewStub_btn = (Button) add_view.findViewById(R.id.add_city_viewstub_btn);
                        mAdd_viewStub_btn.setOnClickListener(this);
                        mGridView = (GridView) add_view.findViewById(R.id.cities_grid);
                        mGridAdapter = new GridAdapter(this);
                        mGridView.setAdapter(mGridAdapter);
                    } catch (Exception e) {
                        mAdd_viewStub.setVisibility(View.VISIBLE);
                    }

                    mAdd_btn.setSelected(true);
                } else {
                    //隐藏页面
                    mAdd_viewStub.setVisibility(View.GONE);
                    mAddEditText.setText("");
                    mAdd_btn.setSelected(false);
                }
                add_btn_click_count %= 2;
                break;
            case R.id.trend_btn:
                if ((trend_btn_click_count++) % 2 == 0) {
                    if ((add_btn_click_count) % 2 != 0) {
                        add_btn_click_count = 0;
                        mAdd_viewStub.setVisibility(View.GONE);
                        mAddEditText.setText("");
                        mAdd_btn.setSelected(false);
                    }
                    if ((signal_btn_click_count) % 2 != 0) {
                        signal_btn_click_count = 0;
                        mSignal_viewStub.setVisibility(View.GONE);
                        mSignal_btn.setSelected(false);
                    }
                    String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
                    Uri uri = Uri.parse(uriStr);
                    String[] projection = new String[]{"today", "next1th", "next2th", "next3th", "next4th"};
                    String selection = "city=?";
                    String[] selectionArgs = new String[]{mCityList.get(mViewPager.getCurrentItem())};
                    Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
                    cursor.moveToFirst();

                    //显示隐藏页面
                    try {
                        View trend_view = mTrend_viewStub.inflate();
                        mLine_chart = (LineChart)trend_view.findViewById(R.id.line_chart);
                    } catch (Exception e) {
                        mTrend_viewStub.setVisibility(View.VISIBLE);
                    } finally {
                        if (cursor.getCount() != 0) {
                            if (!TextUtils.isEmpty(cursor.getString(0))) {
                                String[] tmpMinMax = new String[5];
                                final String[] dateStr = new String[5];
                                for (int i = 0; i < 5; i++) {
                                    String[] tmp = cursor.getString(i).split(";");
                                    tmpMinMax[i] = tmp[2].substring(0, 5);
                                    dateStr[i] = tmp[0];
                                }
                                List<Entry> tmpMin = new ArrayList<Entry>();
                                List<Entry> tmpMax = new ArrayList<Entry>();
                                for (int i = 0; i < 5; i++) {
                                    String[] tmp = tmpMinMax[i].split("~");
                                    tmpMin.add(new Entry(i, Integer.valueOf(tmp[0])));
                                    tmpMax.add(new Entry(i, Integer.valueOf(tmp[1])));
                                }

                                ArrayList<ILineDataSet> lines = new ArrayList<ILineDataSet>();

                                ValueFormatter valueFormatter = new ValueFormatter() {
                                    @Override
                                    public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                                                    ViewPortHandler viewPortHandler) {
                                        return String.valueOf((int)value) + "°";
                                    }
                                };

                                LineDataSet lineDataSetMin = new LineDataSet(tmpMin, null);
                                lineDataSetMin.setCircleRadius(6);
                                lineDataSetMin.setLineWidth(3);
                                lineDataSetMin.setValueTextSize(10);
                                lineDataSetMin.setValueFormatter(valueFormatter);

                                LineDataSet lineDataSetMax = new LineDataSet(tmpMax, null);
                                lineDataSetMax.setColor(Color.RED);
                                lineDataSetMax.setCircleColor(Color.RED);
                                lineDataSetMax.setCircleRadius(6);
                                lineDataSetMax.setLineWidth(3);
                                lineDataSetMax.setValueTextSize(10);
                                lineDataSetMax.setValueFormatter(valueFormatter);

                                lines.add(lineDataSetMin);
                                lines.add(lineDataSetMax);

                                YAxis leftAxis = mLine_chart.getAxisLeft();
                                leftAxis.setEnabled(false);

                                YAxis rightAxis = mLine_chart.getAxisRight();
                                rightAxis.setDrawLabels(false);
                                rightAxis.setDrawGridLines(false);
                                //rightAxis.setEnabled(false);

                                AxisValueFormatter axisValueFormatter = new AxisValueFormatter() {
                                    @Override
                                    public String getFormattedValue(float value, AxisBase axis) {
                                        return dateStr[(int)value];
                                    }

                                    @Override
                                    public int getDecimalDigits() {
                                        return 0;
                                    }
                                };

                                XAxis xAxis = mLine_chart.getXAxis();
                                //xAxis.setDrawGridLines(false);
                                xAxis.setDrawAxisLine(false);
                                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
                                xAxis.setValueFormatter(axisValueFormatter);
                                //xAxis.setDrawLabels(false);
                                //xAxis.setEnabled(false);

                                mLine_chart.setDescription("");
                                mLine_chart.setNoDataTextDescription("无天气数据");
                                mLine_chart.getLegend().setEnabled(false);
                                mLine_chart.setData(new LineData(lines));
                                mLine_chart.setTouchEnabled(false);
                                mLine_chart.invalidate();
                            }
                        }

                    }
                    mTrend_btn.setSelected(true);
                } else {
                    //隐藏页面
                    mTrend_viewStub.setVisibility(View.GONE);
                    mTrend_btn.setSelected(false);
                }
                trend_btn_click_count %= 2;
                break;
            case R.id.signal_btn:
                if ((signal_btn_click_count++) % 2 == 0) {
                    if ((add_btn_click_count) % 2 != 0) {
                        add_btn_click_count = 0;
                        mAdd_viewStub.setVisibility(View.GONE);
                        mAddEditText.setText("");
                        mAdd_btn.setSelected(false);
                    }
                    if ((trend_btn_click_count) % 2 != 0) {
                        trend_btn_click_count = 0;
                        mTrend_viewStub.setVisibility(View.GONE);
                        mTrend_btn.setSelected(false);
                    }
                    String uriStr = ContentData.PREFIX + ContentData.AUTHORITY + "/" + WeatherDBHelper.TABLE_NAME;
                    Uri uri = Uri.parse(uriStr);
                    String[] projection = new String[]{"comf", "cw", "drsg", "flu", "sport", "uv"};
                    String selection = "city=?";
                    String[] selectionArgs = new String[]{mCityList.get(mViewPager.getCurrentItem())};
                    Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
                    cursor.moveToFirst();

                    //显示隐藏页面
                    try {
                        View signal_view = mSignal_viewStub.inflate();
                        mComf_text = (TextView)signal_view.findViewById(R.id.comf_text);
                        mCw_text = (TextView)signal_view.findViewById(R.id.cw_text);
                        mDrsg_text = (TextView)signal_view.findViewById(R.id.drsg_text);
                        mFlu_textxt = (TextView)signal_view.findViewById(R.id.flu_text);
                        mSport_text = (TextView)signal_view.findViewById(R.id.sport_text);
                        mUv_text = (TextView)signal_view.findViewById(R.id.uv_text);
                    } catch (Exception e) {
                        mSignal_viewStub.setVisibility(View.VISIBLE);
                    } finally {
                        if (cursor.getCount() != 0) {
                            mComf_text.setText(cursor.getString(0));
                            mCw_text.setText(cursor.getString(1));
                            mDrsg_text.setText(cursor.getString(2));
                            mFlu_textxt.setText(cursor.getString(3));
                            mSport_text.setText(cursor.getString(4));
                            mUv_text.setText(cursor.getString(5));
                        }

                    }
                    mSignal_btn.setSelected(true);
                } else {
                    //隐藏页面
                    mSignal_viewStub.setVisibility(View.GONE);
                    mSignal_btn.setSelected(false);
                }
                signal_btn_click_count %= 2;
                break;
            case R.id.add_city_viewstub_btn:
                String city_str = mAddEditText.getText().toString();
                Log.i("add_viewstub", "点击添加按钮");
                if (TextUtils.isEmpty(city_str)) {
                    Toast.makeText(getApplicationContext(), "请输入城市名", Toast.LENGTH_SHORT).show();
                } else {
                    if(isNetworkAvailable(this)) {
                        //添加新城市，并获取天气数据
                        Log.i(MAIN_ACTIVITY, "addViewStub--->点击添加按钮，添加城市天气数据");
                        mFrag_list.add(CityWeatherFrag.newFragment(city_str, true));
                        mCirCleTabAdapter.notifyDataSetChanged();
                        mCityList.add(city_str);
                        mGridAdapter.notifyDataSetChanged();
                        mEditor.putInt(CUR_POS_IN_PAGER, mCityList.size() - 1).commit();
                        mViewPager.setCurrentItem(mCityList.size() - 1);
                        add_btn_click_count = ++add_btn_click_count % 2;
                        mAddEditText.setText("");
                        mAdd_viewStub.setVisibility(View.GONE);
                        mAdd_btn.setSelected(false);

                    } else {
                        Toast.makeText(getApplicationContext(), "无法获取网络数据", Toast.LENGTH_SHORT).show();
                    }

                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEditor.putInt(CUR_POS_IN_PAGER, mViewPager.getCurrentItem()).commit();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();  //当无网络时，返回null
        if (activeNetwork == null) {
            return false;
        } else {
            return activeNetwork.isConnectedOrConnecting();
        }
    }

/*    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        if(ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view != null && !(view instanceof ViewStub) && !(view instanceof ImageButton)) {
                if (add_btn_click_count % 2 != 0) {
                    add_btn_click_count = 0;
                    mAdd_viewStub.setVisibility(View.GONE);
                    mAddEditText.setText("");
                    mAdd_btn.setSelected(false);
                }
                if (trend_btn_click_count % 2 != 0) {
                    trend_btn_click_count = 0;
                    mTrend_viewStub.setVisibility(View.GONE);
                    mTrend_btn.setSelected(false);
                }
                if (signal_btn_click_count % 2 != 0) {
                    signal_btn_click_count = 0;
                    mSignal_viewStub.setVisibility(View.GONE);
                    mSignal_btn.setSelected(false);
                }
            }
            return true;
        }
        return false;
    }*/
}
