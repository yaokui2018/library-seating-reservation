package ltd.yaokui.seat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.bravin.btoast.BToast;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ltd.yaokui.seat.utils.HttpUtils;
import ltd.yaokui.seat.utils.TxtFileUtil;

import static java.lang.Integer.parseInt;

public class CrazyActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCANHtml = 98;//记录页
    private int dbVersion = 1;
    //sign值获取时间
    private long getSignTime = 0;

    SharedPreferences mShare;

    SwipeRefreshLayout refreshLayout;

    SharedPreferences settingShare;

    boolean skipLoading, autoSeat, autoBook;
    int bookshow, sort, countID;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crazy);

        settingShare = this.getSharedPreferences("setting", MODE_PRIVATE);
        mShare = getSharedPreferences("author", MODE_PRIVATE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // 添加菜单
        /*
         * pram1:组号 pram2:唯一的ID号 pram3:排序号 pram4:标题
         */
        menu.add(0, 2, 2, "预约记录");
        menu.add(0, 4, 4, "座位地图");
        menu.add(0, 5, 5, "Sign列表");
        menu.add(0, 50, 50, "退出");

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 响应每个菜单项(通过菜单项的ID)
        switch (item.getItemId()) {
            case R.id.navigation_setting:
                startActivity(new Intent(getApplicationContext(),SettingActivity.class));
                break;
            case 0:
                Intent intent1 = new Intent(CrazyActivity.this,ImportActivity.class);
                startActivity(intent1);
                break;
            case 2:
                Intent intent = new Intent(CrazyActivity.this,HistoryActivity.class);
                startActivity(intent);
                break;
            case 4:
                Intent mapintent = new Intent(CrazyActivity.this,SeatMapActivity.class);
                mapintent.putExtra("room", "202");
                startActivity(mapintent);
                break;
            case 5://获取signlist
                mapintent = new Intent(CrazyActivity.this,SignActivity.class);
                startActivity(mapintent);
                break;
            case R.id.Sign://获取sign
                AlertDialog.Builder builder11 = new AlertDialog.Builder(CrazyActivity.this);
                builder11.setIcon(R.mipmap.ic_launcher);
                builder11.setTitle("更新Sign");
                builder11.setMessage("本地sign2值数量："+querySign2Size()+"\n\n当前sign值对应时间："+settingShare.getString("seatDate","")+"\n\n【说明】目前学校主要验证参数为sign2 + sign 。\n\nsign2：每发送一次预约/入座请求时就会消耗掉一个。\n\nsign：只要保持sign值对应的时间和当前时间在误差允许范围内就可以了。\n\n联网获取：\n1.获取sign2值，请选择第一项。\n2.底下的数量选择是获取sign值的。");//\n\n提示：sign值获取方式已经调整为本地存储。\n\n即：一次性获取服务器n条数据，暂存在本地，发送相关请求时直接从本地调取，无需手动获取sign值了。" +
                // "只需保证本地sign值充裕即可。（闲来没事点两下联网获取就行了）\n\n联网获取：\n一次性获取1000条SIGN值记录，存放到本地。（可以连续获取多次哦。一整天的sign数量在800左右)");
                builder11.setCancelable(true);
                builder11.setNeutralButton("查看本地记录", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mapintent = new Intent(CrazyActivity.this,SignActivity.class);
                        startActivity(mapintent);
                    }
                });
                builder11.setNegativeButton("好的", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Sign sign = querySign();
                        if (sign.getSign()==null) {
                            getSignTime = 0;
                            Toast.makeText(getApplicationContext(),"本地sign值不足，请联网获取！",Toast.LENGTH_SHORT).show();
                        }
                        else {
                            settingShare.edit().putString("seatSign", sign.getSign()).commit();
                            settingShare.edit().putString("seatDate", sign.getDate()).commit();

                            getSignTime = Long.parseLong(sign.getSign().split("\\.")[1]);
                            Toast.makeText(getApplicationContext(),"已成功调取本地最新可用Sign值~",Toast.LENGTH_SHORT).show();
                        }
                    }

                });
                builder11.create().show();
                break;
            case 50:
                finish();
                break;
            case 99:
                Intent intent2 = new Intent();
                intent2.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse("http://yaokui.ltd:8080");
                intent2.setData(content_url);
                startActivity(intent2);
                break;
            default:
                // 对没有处理的事件，交给父类来处理
                return super.onOptionsItemSelected(item);
        }
        //返回true表示处理完菜单项的事件，不需要将该事件继续传播下去了
        return true;
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            return new PlaceholderFragment().newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            Toast.makeText(getApplicationContext(), "防误触：右上角菜单退出", Toast.LENGTH_SHORT).show();

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //sign查询
    public Sign querySign(){
        int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
        int reGetSignTimeSub = settingShare.getInt("reGetSignTimeSub",0);
        int reGetSignTimeSwtich = settingShare.getInt("reGetSignTimeSwtich",0);
        String reGetSignTimeS="";
        if (reGetSignTimeSwtich==1){
            Calendar instance = Calendar.getInstance();
            int year = instance.get(Calendar.YEAR);//获取年份
            int month=instance.get(Calendar.MONTH)+1;//获取月份
            int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
            int hour=instance.get(Calendar.HOUR_OF_DAY);//小时
            reGetSignTimeS = "date >= '"+year+"-"+(month<10?"0":"")+month+"-"+(day<10?"0":"")+day+" "+(hour<10?"0":"")+hour+":00:00' and";
        }
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("sign", new String[]{"id","sign","date","id_"}, reGetSignTimeS+" date > datetime('now','-"+reGetSignTimeAdd+" seconds','localtime') and date < datetime('now','"+reGetSignTimeSub+" seconds','localtime')", null, null, null, "date", "1");
        //利用游标遍历所有数据对象
        Sign db = new Sign();
        if(cursor.moveToNext()){
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setSign(cursor.getString(cursor.getColumnIndex("sign")));
            db.setDate(cursor.getString(cursor.getColumnIndex("date")));
            db.setId_(cursor.getInt(cursor.getColumnIndex("id_")));
        }
        return db;
    }


    //sign2删除
    public boolean deleteSign2(int id){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign2", "id=?", new String[]{id+""})>0;
    }
    //sign2查询
    public Sign2 querySign2(){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("sign2", new String[]{"id","sign2"}, null, null, null, null, "id", "1");
        //利用游标遍历所有数据对象
        Sign2 db = new Sign2();
        if(cursor.moveToNext()){
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setSign2(cursor.getString(cursor.getColumnIndex("sign2")));

            deleteSign2(cursor.getInt(cursor.getColumnIndex("id")));
        }
        return db;
    }
    //sign2查询数量
    public int querySign2Size(){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.rawQuery("select count(id) from sign2 ",null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }


}

