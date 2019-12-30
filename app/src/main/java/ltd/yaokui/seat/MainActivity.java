package ltd.yaokui.seat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.a520wcf.yllistview.YLListView;
import com.bigkoo.alertview.AlertView;
import com.bigkoo.alertview.OnDismissListener;
import com.bigkoo.alertview.OnItemClickListener;
import com.bravin.btoast.BToast;
import com.carlt.networklibs.NetType;
import com.carlt.networklibs.NetworkManager;
import com.carlt.networklibs.annotation.NetWork;
import com.carlt.networklibs.utils.Constants;

import net.lemonsoft.lemonhello.LemonHello;
import net.lemonsoft.lemonhello.LemonHelloAction;
import net.lemonsoft.lemonhello.LemonHelloInfo;
import net.lemonsoft.lemonhello.LemonHelloView;
import net.lemonsoft.lemonhello.interfaces.LemonHelloActionDelegate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ltd.yaokui.seat.utils.HttpUtils;
import ltd.yaokui.seat.utils.TxtFileUtil;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE_SCAN = 99;//扫码
    private static final int REQUEST_CODE_SCANHtml = 98;//记录页
    //加载框变量
    private ProgressDialog progressDialog;
    //sign值获取时间
    private long getSignTime = 0;

    private WebView webView;
    private YLListView listView;
    List<Db> dbList;
    static DemoAdapter demoAdapter;
    private int dbVersion = 1;

    String resultTitle;//服务器返回结果
    String resultContent;//服务器返回结果
    String resultMost;//服务器返回结果 1.强制更新
    String resultNotice;//服务器返回结果 1.显示通知
    String resultDownload;//服务器返回结果 更新链接
    String resultUpdate;//服务器返回结果 更新说明
    String resultVersion;//服务器返回结果 最新版本
    String appSign;//服务器返回结果 签名
    final String urlpath = "http://yaokui.ltd:8080/baoming/seat_index.jsp";
    SharedPreferences mShare;

    BottomNavigationView navigation;

    SwipeRefreshLayout refreshLayout;

    SharedPreferences settingShare;
    boolean skipLoading,autoSeat,autoBook;
    int bookshow,sort,countID;


    class DemoAdapter extends BaseAdapter {
        int pos;

        public void refresh() {
            countID = settingShare.getInt("countID",0);
            dbList.clear();
            dbList.addAll(query());
            notifyDataSetChanged();
            if (webView.getUrl().equals("file:///android_asset/allStuId.html"))
                webView.loadUrl("file:///android_asset/allStuId.html");
        }

        @Override
        public int getCount() {
            return dbList.size()==0?1:dbList.size();
        }
        @Override
        public Object getItem(int position) {
            return position;
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (dbList.size()==0){
                View view = View.inflate(getApplicationContext(),R.layout.no_data,null);
                return view;
            }
            View view = View.inflate(getApplicationContext(),R.layout.list_view,null);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final EditText inputServer = new EditText(MainActivity.this);
                    inputServer.setText(dbList.get(position).getRemark());
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("修改备注").setIcon(R.drawable.ic_person_outline_black_24dp).setView(inputServer)
                            .setNegativeButton("取消", null);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            update(dbList.get(position).getId(),dbList.get(position).getNumber(),dbList.get(position).getPassword(),inputServer.getText().toString());
                            refresh();
                        }
                    });
                    builder.show();
                    return true;
                }
            });
            TextView textView = view.findViewById(R.id.text_id);
            if (countID!=1) {
                textView.setText(position + 1 + "");
            }
            else {
                textView.setText(dbList.get(position).getId() + "");
            }
            textView = view.findViewById(R.id.text_num);
            textView.setText(dbList.get(position).getNumber()+"");
            LinearLayout linearLayout = view.findViewById(R.id.text_num_layout);
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this,HistoryActivity.class);
                    startActivity(intent);
                }
            });
            textView = view.findViewById(R.id.text_remark);
            textView.setText(dbList.get(position).getRemark());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(),"长按可修改备注。",Toast.LENGTH_SHORT).show();
                }
            });
            Button button = view.findViewById(R.id.text_del);
            button.setOnClickListener(new View.OnClickListener() {
                int pos = position;
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(android.R.drawable.ic_delete);
                    builder.setTitle("温馨提示");
                    builder.setMessage("确定要删除学号 "+dbList.get(pos).getNumber()+" 吗？");
                    builder.setCancelable(true);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //删除
                            if(delete(dbList.get(pos).getId())){
                                Toast.makeText(getApplicationContext(),"删除成功。",Toast.LENGTH_SHORT).show();
                                refresh();
                            }
                            else
                                Toast.makeText(getApplicationContext(),"删除失败。",Toast.LENGTH_SHORT).show();

                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /*
                             *  在这里实现你自己的业务逻辑
                             */
                        }

                    });
                    builder.create().show();
                    Log.i("定义构造方法", "点击事件");
                }
            });
            return view;
        }

    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_account:
                    webView.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    demoAdapter.refresh();
//                    webView.loadUrl("file:///android_asset/history.html");
                    return true;
                case R.id.navigation_book:
                    webView.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    if (resultVersion==null){
                        okhttpDate();
                    }
                    if (!webView.getUrl().equals("file:///android_asset/allStuId.html"))
                        webView.loadUrl("file:///android_asset/allStuId.html");
                    return true;
                case R.id.navigation_binding:
                    webView.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    if (!webView.getUrl().equals("file:///android_asset/authorize.html"))
                        webView.loadUrl("file:///android_asset/authorize.html");
                    return true;
                case R.id.navigation_query:
                    webView.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    if (!webView.getUrl().equals("file:///android_asset/findStuID.html"))
                        webView.loadUrl("file:///android_asset/findStuID.html");
                    return true;
                case R.id.navigation_setting:
                    startActivity(new Intent(getApplicationContext(),SettingActivity.class));
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BToast.Config.getInstance()
                .apply(getApplication());// must call



        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);
        countID = settingShare.getInt("countID",0);

        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);

        mShare = getSharedPreferences("author",MODE_PRIVATE);
        String author = mShare.getString("author", "");
        //授权码验证
        if (author.length()!=22) {
            if (author.equals("ok1")){
                mShare.edit().putString("author","").commit();
            }
            else if (author.equals("limit")){
                Toast.makeText(getApplicationContext(),"限制使用",Toast.LENGTH_LONG).show();
            }
            Intent intent = new Intent(MainActivity.this, AuthorizationActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        String d = "12-21 23:59:59";
        SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm:ss");
        Date date= new Date();
        try {
            date = df.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int kaoyanDay = HistoryActivity.differentDays(new Date(),date);
        setTitle(settingShare.getString("title","Library seating reservation")+" ["+kaoyanDay+"]");

        int ky_day = settingShare.getInt("kaoyanDay",99);
        if (ky_day-kaoyanDay>=3){
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            builder1.setIcon(R.mipmap.ic_launcher);
            builder1.setTitle("考研倒计时："+kaoyanDay+"天");
            builder1.setMessage("又过去"+(ky_day-kaoyanDay)+"天了。。。\n\n这"+(ky_day-kaoyanDay)+"天时间里学习了哪些内容？\n\n别忘了总结和反思呀~");
            builder1.setCancelable(true);
            builder1.setPositiveButton("我知道啦", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder1.create().show();
//            Toast.makeText(getApplication(),"又过去3天了，这3天时间里学习了哪些内容？\n别忘了总结和反思呀~",Toast.LENGTH_LONG).show();
            settingShare.edit().putInt("kaoyanDay",kaoyanDay).commit();
        }
        if (querySign2Size()<100){
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            builder1.setMessage("本地sign2值不足100条了哦，别忘了联网获取~");
            builder1.setCancelable(false);
            builder1.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder1.create().show();
        }

        listView = (YLListView) findViewById(R.id.listView);
        // 不添加也有默认的头和底
        View topView=View.inflate(this,R.layout.top,null);
        listView.addHeaderView(topView);
        View bottomView=new View(getApplicationContext());
        listView.addFooterView(bottomView);

        // 顶部和底部也可以固定最终的高度 不固定就使用布局本身的高度
        listView.setFinalBottomHeight(200);
        listView.setFinalTopHeight(100);
        dbList = query();
        demoAdapter = new DemoAdapter();
        listView.setAdapter(demoAdapter);

        //YLListView默认有头和底  处理点击事件位置注意减去
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position=position-listView.getHeaderViewsCount();
            }
        });

        //定时任务
        if (autoBook&&!AutoBookService.isServiceRunning()){
            Intent intentOne = new Intent(getApplicationContext(), AutoBookService.class);
            stopService(intentOne);
            startService(intentOne);
        }
        if (autoSeat&&!AutoSeatService.isServiceRunning()){
            Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
            stopService(intentOne);
            startService(intentOne);
        }

//        app_update("发现新版本","快快更新吧");
        okhttpDate();

        refreshLayout = findViewById(R.id.swipe_fresh);
        webView = (WebView) findViewById(R.id.message);
        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (settingShare.getBoolean("fresh",true)){
            refreshLayout.setColorSchemeResources(R.color.colorPrimary);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if(!refreshLayout.canChildScrollUp()) {
                        webView.reload();
                        refreshLayout.setRefreshing(false);
                    }
                }
            });
        }
        else
            refreshLayout.setEnabled(false);

        setWebView(webView);
//        String ua = webView.getSettings().getUserAgentString();//原来获取的UA
//        webView.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
//        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView,true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }else{
            try {
                Class<?> clazz = webView.getSettings().getClass();
                Method method = clazz.getMethod("setAllowUniversalAccessFromFileURLs", boolean.class);
                if (method != null) {
                    method.invoke(webView.getSettings(), true);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCacheMaxSize(1024*1024*8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webView.getSettings().setAppCachePath(appCachePath);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.loadUrl("file:///android_asset/allStuId.html");
        webView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // 添加菜单
        menu.add(0,0,0,"导入账号");
        menu.add(0, 1, 1, "导出账号");
        /*
         * pram1:组号 pram2:唯一的ID号 pram3:排序号 pram4:标题
         */
        menu.add(0, 2, 2, "预约记录");
        menu.add(0, 3, 3, "清空账号");
        menu.add(0, 4, 4, "座位地图");
        menu.add(0, 5, 5, "Sign列表");
        menu.add(0, 50, 50, "退出");
//        menu.add(0, 99, 99, "☺ 薄荷你玩");

        // 希望显示菜单就返回true
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
                Intent intent1 = new Intent(MainActivity.this,ImportActivity.class);
                startActivity(intent1);
                break;
            case 2:
                Intent intent = new Intent(MainActivity.this,HistoryActivity.class);
                startActivity(intent);
                break;
            case 1://导出账号
                AlertDialog.Builder builder11 = new AlertDialog.Builder(MainActivity.this);
                builder11.setIcon(R.mipmap.ic_launcher);
                builder11.setTitle("提示");
                builder11.setMessage("确定要导出账号吗？");
                builder11.setCancelable(true);
                builder11.setNeutralButton("复制至剪贴板", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Db> dbList = query();
                        if (dbList.size()==0){
                            Toast.makeText(getApplicationContext(),"一个账号都木有，导出个皮球哦~",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String counts = "";
                        for (Db db : dbList){
                            counts += db.getNumber()+","+db.getPassword()+","+db.getRemark()+"\n";
                        }
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        // 将文本内容放到系统剪贴板里。
                        cm.setText(counts);
                        Toast.makeText(MainActivity.this, "账号信息已复制至剪贴板。", Toast.LENGTH_LONG).show();
                    }
                });
                builder11.setPositiveButton("导出文件", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Db> dbList = query();
                        if (dbList.size()==0){
                            Toast.makeText(getApplicationContext(),"一个账号都木有，导出个皮球哦~",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String counts = "";
                        for (Db db : dbList){
                            counts += db.getNumber()+","+db.getPassword()+","+db.getRemark()+"\n";
                        }

                        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String filename = sdf3.format(new Date())+".txt";
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                        }
                        TxtFileUtil.initData(filename,counts);

                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                        builder1.setIcon(R.mipmap.ic_launcher);
                        builder1.setTitle("导出成功");
                        builder1.setMessage("账号信息已导出至\n/sdcard/seat/out/"+filename+" 。");
                        builder1.setCancelable(true);
                        builder1.setPositiveButton("我知道啦", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        });
                        builder1.create().show();

                    }
                });
                builder11.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {}

                });
                builder11.create().show();
                break;
            case 3:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setIcon(android.R.drawable.ic_delete);
                builder.setTitle("提示");
                builder.setMessage("确定要清空所有账号吗？？");
                builder.setCancelable(true);
                builder.setPositiveButton("【确定】", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //删除
                        if(deleteAll()){
                            Toast.makeText(getApplicationContext(),"已清空。",Toast.LENGTH_SHORT).show();
                            demoAdapter.refresh();
                        }
                        else
                            Toast.makeText(getApplicationContext(),"清空失败。",Toast.LENGTH_SHORT).show();

                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {}

                });
                builder.create().show();
                break;
            case 4:
                Intent mapintent = new Intent(MainActivity.this,SeatMapActivity.class);
                mapintent.putExtra("room", "202");
                startActivity(mapintent);
                break;
            case 5://获取signlist
                mapintent = new Intent(MainActivity.this,SignActivity.class);
                startActivity(mapintent);
                break;
            case R.id.Sign://获取sign
                builder11 = new AlertDialog.Builder(MainActivity.this);
                builder11.setIcon(R.mipmap.ic_launcher);
                builder11.setTitle("更新Sign");
                builder11.setMessage("本地sign2值数量："+querySign2Size()+"\n\n当前sign值对应时间："+settingShare.getString("seatDate","")+"\n\n【说明】目前学校主要验证参数为sign2 + sign 。\n\nsign2：每发送一次预约/入座请求时就会消耗掉一个。\n\nsign：只要保持sign值对应的时间和当前时间在误差允许范围内就可以了。\n\n联网获取：\n1.获取sign2值，请选择第一项。\n2.底下的数量选择是获取sign值的。");//\n\n提示：sign值获取方式已经调整为本地存储。\n\n即：一次性获取服务器n条数据，暂存在本地，发送相关请求时直接从本地调取，无需手动获取sign值了。" +
                       // "只需保证本地sign值充裕即可。（闲来没事点两下联网获取就行了）\n\n联网获取：\n一次性获取1000条SIGN值记录，存放到本地。（可以连续获取多次哦。一整天的sign数量在800左右)");
                builder11.setCancelable(true);
                builder11.setNeutralButton("查看本地记录", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mapintent = new Intent(MainActivity.this,SignActivity.class);
                        startActivity(mapintent);
                    }
                });
                builder11.setPositiveButton("联网获取", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                        final String[] items = new String[] { "获取sign2值[sign2!!]","获取sign值[1000]","获取sign值[5000]","获取sign值[10000]","获取sign值[999999]" };
                        builder.setTitle("选择获取数量[Max]");
                        builder.setSingleChoiceItems(items,-1, new DialogInterface.OnClickListener() {
                            @SuppressLint("WrongConstant")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int number = 1000;//= settingShare.getInt("signNumber", 1000);
                                switch (which){
                                    case 0:
                                        number = -1;//sign2
                                        break;
                                    case 1:
                                        number = 1000;
                                        break;
                                    case 2:
                                        number = 5000;
                                        break;
                                    case 3:
                                        number = 10000;
                                        break;
                                    case 4:
                                        number = 999999;
                                        break;
                                }
                                showProgressDialog(MainActivity.this, "获取中...");

                                int id_ = settingShare.getInt("signId", 0);
                                getSignTime = 0;
                                getSign(null,number,id_);
                                dialog.dismiss();
                            }
                        });
                        builder.show();
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
//                        Calendar instance = Calendar.getInstance();
//                        int year = instance.get(Calendar.YEAR);//获取年份
//                        int month = instance.get(Calendar.MONTH);//获取月份
//                        int day = instance.get(Calendar.DAY_OF_MONTH);//获取日
//                        String dateStr = year + "-" + (month + 1) + "-" + day + "%2011:59";
//                        Toast.makeText(getApplicationContext(),"获取中...",Toast.LENGTH_SHORT).show();
////                        getSign(dateStr);
                    }

                });
                builder11.create().show();
                break;
            case 50:
                Toast.makeText(getApplicationContext(), "溜啦溜啦~", Toast.LENGTH_SHORT).show();
                Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
                //传递参数
                intentOne.putExtra("data","exit");
                startService(intentOne);
                intentOne = new Intent(getApplicationContext(), AutoBookService.class);
                intentOne.putExtra("data","exit");
                startService(intentOne);
                finish();
                System.exit(0);
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

    public void getSign(final String dateStr,final int number,final int id_){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String strUrlPath = "http://yaokui.ltd:8080/baoming/getSignList_json.jsp?id="+id_+"&author="+mShare.getString("author", "")+"&number="+number;
                if (number==-1){//获取sign2
                    if (querySign2Size()>settingShare.getInt("Sign2MaxLimit",10000)) {
                        BToast.error(getApplicationContext())
                                .text("你获取的sign2数量已经够多了哦~给其他人留一点吧")
                                .show();
                        dismissProgressDialog();
                        return;
                    }
                    strUrlPath = "http://yaokui.ltd:8080/baoming/getSign2List_json.jsp?number="+settingShare.getInt("getSign2Max",1000);;
                }
                if (dateStr != null)
                    strUrlPath += "&date=" + dateStr;
                String strResult = HttpUtils.submitPostData(strUrlPath, "", "utf-8");
                strResult = strResult.replace("\r","").replace("\n","");

                if (strResult.equals("error")) {
                    Message msg = new Message();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putString("text", "数据获取失败，请检查网络连接。");  //往Bundle中存放数据
                    bundle.putInt("time", 1);  //往Bundle中put数据
                    msg.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(msg);

                    dismissProgressDialog();
                    return;
                }
                JSONObject jsonObject = null;
                try {
                    String[] strResultList = strResult.split(";");
                    int success =0,fail = 0,skip=0;
                    if (number==-1){//sign2
                        //创建一个DatabaseHelper对象
                        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db", dbVersion);
                        //取得一个只读的数据库对象
                        SQLiteDatabase db = dbHelper1.getReadableDatabase();
                        db.beginTransaction(); // 手动设置开始事务

                        for (String s : strResultList) {
                            jsonObject = new JSONObject(s);
                            String sign2 = jsonObject.getString("sign2");
                            ContentValues values = new ContentValues();
                            //像ContentValues中存放数据
                            values.put("sign2", sign2);
                            if (sign2.length() != 53) {
                                skip++;
                                continue;
                            }
                            long i = db.insertWithOnConflict("sign2", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                            if (i < 1) {
                                fail++;
                            } else
                                success++;

                        }
                        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
                        db.endTransaction(); // 处理完成
                        db.close();
                        dismissProgressDialog();
                        Message msg = new Message();
                        msg.what = 4;
                        Bundle bundle = new Bundle();
                        bundle.putString("content", "成功添加" + success + "条sign2数据，失败" + fail + "条，跳过" + skip + "条。");  //往Bundle中存放数据
                        msg.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(msg);
                    }
                    else {
                        String date1 = "", date = "无";
                        int id_ = -1;
                        //创建一个DatabaseHelper对象
                        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db", dbVersion);
                        //取得一个只读的数据库对象
                        SQLiteDatabase db = dbHelper1.getReadableDatabase();
                        db.beginTransaction(); // 手动设置开始事务

                        for (String s : strResultList) {
                            jsonObject = new JSONObject(s);
                            String sign = jsonObject.getString("sign");
                            id_ = jsonObject.getInt("id");
                            date = jsonObject.getString("date").replace(".0", "");
                            if (date1.equals(""))
                                date1 = date;
                            ContentValues values = new ContentValues();
                            //像ContentValues中存放数据
                            values.put("sign", sign);
                            values.put("date", date);
                            values.put("id_", id_);
                            if (sign.length() != 46) {
                                skip++;
                                continue;
                            }
                            long i = db.insertWithOnConflict("sign", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                            if (i < 1) {
                                fail++;
                            } else
                                success++;

                        }
                        db.setTransactionSuccessful(); // 设置事务处理成功，不设置会自动回滚不提交
                        db.endTransaction(); // 处理完成
                        db.close();
                        dismissProgressDialog();
                        if (id_ > 0)
                            settingShare.edit().putInt("signId", id_).commit();

                        Message msg = new Message();
                        msg.what = 4;
                        Bundle bundle = new Bundle();
                        bundle.putString("content", "成功添加" + success + "条数据，失败" + fail + "条，跳过" + skip + "条。\n\n日期范围：\n" + date1 + "\nto\n" + date);  //往Bundle中存放数据
                        msg.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    dismissProgressDialog();
                    Message msg = new Message();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putString("text", "数据解析出错，请稍后再试。");  //往Bundle中存放数据
                    bundle.putInt("time", 1);  //往Bundle中put数据
                    msg.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(msg);
                    System.out.print(e);
                }
            }
        }).start();
    }

    /**
     * webview设置
     * @param webview
     */
    public void setWebView(final WebView webview) {
        WebSettings webSettings = webview.getSettings();
        webSettings.setBuiltInZoomControls(true);
//        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36");
//自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
//自动缩放
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
//支持获取手势焦点
//        webView.requestFocusFromTouch();

        //不显示webview缩放按钮
        webSettings.setDisplayZoomControls(false);

        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);

        String cacheDirPath = this.getFilesDir().getAbsolutePath() + "cache/";
        webSettings.setAppCachePath(cacheDirPath);
        // 1. 设置缓存路径

        webSettings.setAppCacheMaxSize(20 * 1024 * 1024);
        // 2. 设置缓存大小

        webSettings.setAppCacheEnabled(true);
        // 3. 开启Application Cache存储机制
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webview.setWebChromeClient(new MyWebChromeClient());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        webview.addJavascriptInterface(new JsInterface(), "seat");
    }

    // 用来计算返回键的点击间隔时间
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                //弹出提示，可以有多种方式

                BToast.warning(this)
                        .text("再按一次退出程序")
//                        .animate(true)
                        .show();
//                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                webView.destroy();
                finish();
//                System.exit(0);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 与js进行交互
     */
    public class JsInterface {
        List<Db> list = new ArrayList();

        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void showToast2(String toast,int time) {
            Toast.makeText(MainActivity.this, toast, time).show();
        }
        //自定义吐司
        @JavascriptInterface
        public void showMyToast(String toast,int flag) {//flag:0.error 1.success 2.warn 3.info
            switch (flag){
                case 2:
                    BToast.warning(getApplicationContext())
                        .text(toast)
                        .show();
                    break;
                case 0:
                    BToast.error(getApplicationContext())
                        .text(toast)
                        .show();
                    break;
                case 1:
                    BToast.success(getApplicationContext())
                        .text(toast)
                        .show();
                    break;
                case 3:
                    BToast.info(getApplicationContext())
                        .text(toast)
                        .show();
                    break;
            }
        }
        @JavascriptInterface
        public void debug_(String data) {
            System.out.println(data);
        }
        //绑定账号
        @JavascriptInterface
        public boolean insert_(String number,String password,String remark,String username) {
            if (remark.equals(""))
                remark = username+"_"+number.substring(6);
            Long n = insert(parseInt(number),password,remark);
            String toast = "绑定失败";
            boolean bl = false;
            if (n>0){
                toast = "账号绑定成功";
                bl = true;
            }
            else if (n==-1){
                toast = "该学号已被绑定！";
            }
            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            return bl;
        }
        //解绑
        @JavascriptInterface
        public boolean delete_(String id) {
            boolean bl = delete(parseInt(id));
            String toast = "解绑失败";
            if (bl){
                toast = "解绑成功";
            }
            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            return bl;
        }
        //座位预约
        @JavascriptInterface
        public void query_() {
            list = query();
        }
        @JavascriptInterface
        public Db getDbObject(int index) {
            return list.get(index);
        }
        @JavascriptInterface
        public String getNumToken(int index) {
            return list.get(index).getToken();
        }
        @JavascriptInterface
        public void setListNumToken(int index,String token) {
            list.get(index).setToken(token);
        }
        @JavascriptInterface
        public String getNumTokenByNum(int number) {
            return selectByNum(number).getToken();
        }
        @JavascriptInterface
        public void setNumToken(String token,String number) {
            updateToken(token,number);
        }
        @JavascriptInterface
        public void setTitle(String s) {
            getActionBar().setTitle(s);
        }
        @JavascriptInterface
        public String getTitle(String s) {
            return getActionBar().getTitle().toString();
        }
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(text);
//            Toast.makeText(MainActivity.this, "内容已复制至剪贴板。", Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void copyCount(int number) {
            Db db = selectByNum(number);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(db.getNumber()+","+db.getPassword()+","+db.getRemark());
            Toast.makeText(MainActivity.this, "账号信息已复制至剪贴板。", Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public int getSize() {
            return list.size();
        }
        //跳过loading动画
        @JavascriptInterface
        public boolean skipLoading() {
            return settingShare.getBoolean("skipLoading",false);
        }
        @JavascriptInterface
        public int getBookshow() {
            return settingShare.getInt("bookshow",2);
        }
        @JavascriptInterface
        public boolean getStateBr() {
            return settingShare.getBoolean("stateBr",true);
        }
        @JavascriptInterface
        public boolean getSkip() {
            return settingShare.getBoolean("skip",false);
        }
        @JavascriptInterface
        public boolean getAllListMode() {
            return settingShare.getBoolean("AllListMode",true);
        }
        @JavascriptInterface
        public void AllListMode() {
            if(settingShare.getBoolean("AllListMode",true)){
                settingShare.edit().putBoolean("AllListMode", false).commit();
                Toast.makeText(getApplicationContext(),"已切换至兼容模式",Toast.LENGTH_SHORT).show();
            }
            else {
                settingShare.edit().putBoolean("AllListMode", true).commit();
                Toast.makeText(getApplicationContext(),"已切换至急速模式",Toast.LENGTH_SHORT).show();
            }
        }
        @JavascriptInterface
        public String getSeatVersion() {//获取官方软件版本号
            return settingShare.getString("seatVersion","2.4.0");
        }
        //获取官方软件sign
        @JavascriptInterface
        public String getSeatSign() {
            long sysTime = System.currentTimeMillis();
            int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
            int reGetSignTimeSub = settingShare.getInt("reGetSignTimeSub",0);

            String d = settingShare.getString("seatDate","");
            Calendar instance = Calendar.getInstance();
            int year = instance.get(Calendar.YEAR);//获取年份
            int month=instance.get(Calendar.MONTH)+1;//获取月份
            int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
            int hour=instance.get(Calendar.HOUR_OF_DAY);//小时
            String s = year+"-"+(month<10?"0":"")+month+"-"+(day<10?"0":"")+day+" "+(hour<10?"0":"")+hour;
            if (!d.startsWith(s)||sysTime-getSignTime>reGetSignTimeAdd*1000||getSignTime-sysTime>reGetSignTimeSub*1000) {
                Sign sign = querySign();
                if (sign.getSign()==null) {
                    getSignTime = 0;
                    Toast.makeText(getApplicationContext(),"本地sign值不足，请联网获取！",Toast.LENGTH_SHORT).show();
                }
                else {
                    settingShare.edit().putString("seatSign", sign.getSign()).commit();
                    settingShare.edit().putString("seatDate", sign.getDate()).commit();

                    getSignTime = Long.parseLong(sign.getSign().split("\\.")[1]);
                }
            }
            return settingShare.getString("seatSign","99be17c86d7169e81f7ec6416398dadb.1631546119553");
        }
        //获取官方软件sign2
        @JavascriptInterface
        public String getSeatSign2() {

                Sign2 sign = querySign2();
                if (sign.getSign2()==null) {
                    Toast.makeText(getApplicationContext(),"本地sign2值不足，请联网获取！",Toast.LENGTH_SHORT).show();
                    return "";
                }
            return sign.getSign2();
        }
        @JavascriptInterface
        public String getSeatDate() {
            return settingShare.getString("seatDate","-");
        }
        @JavascriptInterface
        public void setSeatSign(String sign,String date) {
            settingShare.edit().putString("seatSign", sign).commit();
            settingShare.edit().putString("seatDate", date).commit();
            insertSign("0",sign,date);
        }
        @JavascriptInterface
        public String getBookSort() {
            boolean booksort = settingShare.getBoolean("booksort",true);
            if (!booksort)
                return "";
            String s = "ID";
            switch (settingShare.getInt("sort",3)){
                case 1:
                    s="学号";
                    break;
                case 2:
                    s="备注";
                    break;
                case 3:
                    s="seat";
                    break;
                case 4:
                    s="ID·降序";
                    break;
                case 5:
                    s="学号/降序";
                    break;
                case 6:
                    s="备注/降序";
                    break;
                case 7:
                    s="book";
                    break;
            }
            return s;
        }
        @JavascriptInterface
        public boolean getDelBind() {
            boolean delBind = settingShare.getBoolean("delBind",true);
            return delBind;
        }
        //获取登录token
        @JavascriptInterface
        public String getToken(String num,String time) {
            return MD5Utils.MD5Encode(num+"_"+time,"utf-8");
        }
        //入座
        @JavascriptInterface
        public void seat(final String etagMd5,final String i) {
            AlertDialog.Builder builder11 = new AlertDialog.Builder(MainActivity.this);
            builder11.setIcon(R.mipmap.ic_launcher);
            builder11.setTitle("入座方式");
            builder11.setMessage("在图书馆建议使用扫码入座。");
            builder11.setCancelable(true);
            builder11.setNeutralButton("直接入座", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    Toast.makeText(getApplicationContext(),"操作中，请稍等",Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("javascript:doSeat('"+etagMd5+"');");
                        }
                    });
                }
            });
            builder11.setPositiveButton("扫码入座", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("javascript:$(\"#record"+i+"\").click();");
                        }
                    });
                }
            });
            builder11.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {}

            });
            builder11.create().show();
        }
        //离开
        @JavascriptInterface
        public void leave(final String i,final String number,final String id,final String seatId,final String userId) {
            AlertDialog.Builder builder11 = new AlertDialog.Builder(MainActivity.this);
            builder11.setIcon(R.mipmap.ic_launcher);
            builder11.setTitle("离开");
            builder11.setMessage("请选择你要进行的操作。");
            builder11.setCancelable(true);
            builder11.setNeutralButton("长离", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    Toast.makeText(getApplicationContext(),"操作中，请稍等",Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("javascript:Leave("+id+","+i+","+userId+",2);");
                        }
                    });
                }
            });
            builder11.setPositiveButton("暂离", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("javascript:Leave("+id+","+i+","+userId+",1);");
                        }
                    });
                }
            });
            builder11.setNegativeButton("回座", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("javascript:backSeat("+id+","+i+","+number+","+seatId+");");
                        }
                    });
                }

            });
            builder11.create().show();
        }

        //预约记录
        @JavascriptInterface
        public void addHistory(String room,String canSeat,String day,String startTime,String SuserPhysicalCard){
            //创建一个DatabaseHelper对象
            DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",dbVersion);
            //取得一个只读的数据库对象
            SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
            ContentValues values = new ContentValues();
            //像ContentValues中存放数据
            values.put("room",room);
            values.put("seat",canSeat);
            values.put("day", day);
            values.put("time", startTime);
            values.put("status", 0);
            values.put("number", SuserPhysicalCard);
            db1.insert("history",null,values);

            values = new ContentValues();
            values.put("status",1);
            values.put("daytime",day+" "+startTime);
            db1.update("account", values, "number=?", new String[]{SuserPhysicalCard});

//            updateHis(SuserPhysicalCard,room,canSeat,day,startTime);
        }
        //更新账号状态
        @JavascriptInterface
        public void updateStatus(String Time,int status,String SuserPhysicalCard){
            //创建一个DatabaseHelper对象
            DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",dbVersion);
            //取得一个只读的数据库对象
            SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
            ContentValues values = new ContentValues();
            values.put("status",status);
            if (Time!=null)
                values.put("daytime",Time);
            db1.update("account", values, "number=?", new String[]{SuserPhysicalCard});
        }
        //取消预约/入座
        @JavascriptInterface
        public void CancelOrSeat(String number,int flag){//flag : 1.取消 2.入座
            DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",dbVersion);
            SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
            ContentValues values = new ContentValues();
            values.put("status",0);
            db1.update("account", values, "number=?", new String[]{number});
            //更新预约记录
            Cursor cursor = db1.query("history", new String[]{"id"}, "number=?", new String[]{number}, null, null, "id desc", "1");
            int id =0;
            if(cursor.moveToNext()) {
                id = cursor.getInt(cursor.getColumnIndex("id"));
            }
            values = new ContentValues();
            values.put("status",flag);
            db1.update("history", values, "id=?", new String[]{id+""});

        }
        //更改排序状态
        @JavascriptInterface
        public void sort(){
            sort =  settingShare.getInt("sort",3);
            if (sort==3){
                sort = 0;
            }
            else if (sort==7)
                sort = 1;
            else if (sort<3)
                sort+=2;
            else if (sort>3)
                sort++;
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
            final String[] items = new String[] { "入座模式","预约模式","ID","学号","备注","ID（降序）","学号（降序）","备注（降序）" };
            builder.setTitle("账号排序方式");
            builder.setSingleChoiceItems(items,sort, new DialogInterface.OnClickListener() {
                @SuppressLint("WrongConstant")
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sort = which;
                    if (sort==0){
                        sort = 3;
                    }
                    else if (sort==1)
                        sort = 7;
                    else if (sort<5)
                        sort-=2;
                    else if (sort>=5)
                        sort--;
                    settingShare.edit().putInt("sort",sort).commit();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.reload();
                        }
                    });
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        //座位地图
        @JavascriptInterface
        public void roomMap(String room){
            Intent mapintent = new Intent(MainActivity.this,SeatMapActivity.class);
            mapintent.putExtra("room", room);
            startActivity(mapintent);
        }
        //记录-同官方
        @JavascriptInterface
        public void record(String number,String pass,int i){
            Intent mapintent = new Intent(MainActivity.this,RecordActivity.class);
            mapintent.putExtra("number", number);
            mapintent.putExtra("pass", pass);
            mapintent.putExtra("i", i);
            startActivityForResult(mapintent,REQUEST_CODE_SCANHtml);
        }
        //疯狂抢座
        @JavascriptInterface
        public void toCrazy(String seatNum,String seatNumRemark){
            if (seatNum.equals("")){
                Toast.makeText(getApplicationContext(),"请等列表数据加载完全后再点哦~",Toast.LENGTH_SHORT).show();
                return;
            }
            settingShare.edit().putString("seatNum",seatNum).commit();
            settingShare.edit().putString("seatNumRemark",seatNumRemark).commit();
            Intent mapintent = new Intent(MainActivity.this,CrazyActivity.class);
            startActivity(mapintent);
        }
        //官网
        @JavascriptInterface
        public void link() {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri content_url = Uri.parse("http://yaokui.ltd");
            intent.setData(content_url);
            startActivity(intent);
        }
        //限制授权
        @JavascriptInterface
        public void modifyAuthor(String s) {
            mShare.edit().putString("author",s).commit();
        }
        //获取授权数据
        @JavascriptInterface
        public String getAuthor() {
            return mShare.getString("author","");
        }
        //疯狂抢座模式是否可用 1.可用
        @JavascriptInterface
        public String getCrazy() {
            return mShare.getString("crazy","0");
        }
        //获取版本号信息
        @JavascriptInterface
        public String getAppVersion() {
            return getVersion();
        }
        //获取自动预约日志
        @JavascriptInterface
        public String getBookLogs(int i) {
            String auto_book_logs = settingShare.getString("auto_book_logs","");
            String auto_book_logsTime = settingShare.getString("auto_book_logsTime","无");
            if (i == 0)
                return auto_book_logsTime;
            return auto_book_logsTime+" "+auto_book_logs;
        }
        //获取自动入座日志
        @JavascriptInterface
        public String getSeatLogs(int i) {
            String auto_book_logs = settingShare.getString("auto_seat_logs","");
            String auto_book_logsTime = settingShare.getString("auto_seat_logsTime","无");
            if (i == 0)
                return auto_book_logsTime;
            return auto_book_logsTime+" "+auto_book_logs;
        }
        //获取数据
        @JavascriptInterface
        public String getDate(String key,int flag) {//flag 1.setting 2.author
            String s;
            if (flag==1)
                s = settingShare.getString(key,"");
            else
                s = mShare.getString(key,"");
            return s;
        }
        //设置数据
        @JavascriptInterface
        public void setDate(String key,String content,int flag) {//flag 1.setting 2.author
            if (flag==1)
                settingShare.edit().putString(key,content).commit();
            else
                mShare.edit().putString(key,content).commit();
        }
        @JavascriptInterface
        public void close() {
            finish();
        }
        @JavascriptInterface
        public void exit() {
            System.exit(0);
        }

    }

    //回调函数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 记录回传
        if (requestCode == REQUEST_CODE_SCANHtml) {
            if (data != null) {
                int status = data.getIntExtra("status", -1);
                int SeatStatus = data.getIntExtra("SeatStatus", 0);
                String  num = data.getStringExtra("number");
                int i = data.getIntExtra("i",-1);
                int oldStatus = selectByNum(parseInt(num)).getStatus();
                if (status==oldStatus||status==-1)
                    return;
                else {
                    webView.loadUrl("javascript:cancelOrseat("+num+","+i+","+SeatStatus+");");
                }
            }
        }
    }

    //数据库操作
    public long insert(int number,String password,String remark){//插入
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        //取得一个只读的数据库对象
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("account", new String[]{"id","number","password","remark"}, "number=?", new String[]{number+""}, null, null, null, null);
        //利用游标遍历所有数据对象
        while(cursor.moveToNext()){
            return -1;
        }
        ContentValues values = new ContentValues();
        //像ContentValues中存放数据
        values.put("number",number);
        values.put("password",password);
        values.put("remark", remark);
        return db1.insert("account",null,values);
    }
    public void update(int id,int number,String password,String remark){//更新
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("number",number);
        values.put("password",password);
        values.put("remark", remark);
        db1.update("account", values, "id=?", new String[]{id+""});
    }
    public void updateToken(String token,String number){//更新token
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("token", token);
        long a = db1.update("account", values, "number=?", new String[]{number});
        return;
    }
    public Db selectByNum(int number){//通过学号查询
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("account", new String[]{"id","number","password","remark","status","daytime","token"}, "number=?", new String[]{number+""}, null, null, null);
        //利用游标遍历所有数据对象
        Db db = new Db();
        while(cursor.moveToNext()){
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setNumber(cursor.getInt(cursor.getColumnIndex("number")));
            db.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            db.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            db.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            db.setDaytime(cursor.getString(cursor.getColumnIndex("daytime")));
            db.setToken(cursor.getString(cursor.getColumnIndex("token")));
            String name = cursor.getString(cursor.getColumnIndex("number"));
            //日志打印输出
            Log.i("查询结果：","query-->"+name+","+db.getRemark());
        }
        return db;
    }
    public List<Db> query(){//查询
        int mysort = settingShare.getInt("sort",3);
        String s = "id";
        switch (mysort){
            case 1:
                s="number";
                break;
            case 2:
                s="remark COLLATE LOCALIZED";
                break;
            case 3:
                s="status desc,daytime COLLATE LOCALIZED";
                break;
            case 4:
                s="id desc";
                break;
            case 5:
                s="number desc";
                break;
            case 6:
                s="remark COLLATE LOCALIZED desc";
                break;
            case 7:
                s="status,daytime COLLATE LOCALIZED desc";
                break;
        }
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("account", new String[]{"id","number","password","remark","status","daytime","token"}, null, null, null, null, s, null);
        //利用游标遍历所有数据对象
        List<Db> list = new ArrayList<>();
        while(cursor.moveToNext()){
            Db db = new Db();
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setNumber(cursor.getInt(cursor.getColumnIndex("number")));
            db.setPassword(cursor.getString(cursor.getColumnIndex("password")));
            db.setRemark(cursor.getString(cursor.getColumnIndex("remark")));
            db.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            db.setDaytime(cursor.getString(cursor.getColumnIndex("daytime")));
            db.setToken(cursor.getString(cursor.getColumnIndex("token")));
            String name = cursor.getString(cursor.getColumnIndex("number"));
            list.add(db);
            //日志打印输出
            Log.i("查询结果：","query-->"+name+","+db.getRemark());
        }
        return list;
    }
    public boolean delete(int id){//删除
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("account", "id=?", new String[]{id+""})>0;
    }
    public boolean deleteAll(){//删除所有
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("account",null,null)>0;
    }
    //sign插入
    public long insertSign(String id_,String sign,String date){
        if (sign.length()!=46)//格式不对
            return -2;
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        //取得一个只读的数据库对象
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("sign", new String[]{"id","sign","date","id_"}, "date=?", new String[]{date+""}, null, null, null, null);
        //利用游标遍历所有数据对象
        while(cursor.moveToNext()){
            return -1;
        }
        ContentValues values = new ContentValues();
        //像ContentValues中存放数据
        values.put("sign",sign);
        values.put("date",date);
        values.put("id_", id_);
        return db1.insert("sign",null,values);
    }
    //sign删除
    public boolean deleteSign(int id){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign", "id=?", new String[]{id+""})>0;
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

    //sign查询数量
    public int querySignSize(){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.rawQuery("select count(id) from sign where date > datetime('now','-1 minutes','localtime')",null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
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


    //通知
    public void app_notice(String title,String content){
        boolean notice3 = settingShare.getBoolean("notice3",false);
        int noticeTime = settingShare.getInt("noticeTime",0);
        String noticeStr = settingShare.getString("noticeStr","");
        if (!title.equals("sign值更新成功")&&notice3&&noticeStr.equals(content)&&noticeTime>=3){
            return;
        }

        if(Build.VERSION.SDK_INT >= 26){
            new AlertView(title, content, null, new String[]{"我知道啦"}, null, this,
                    AlertView.Style.Alert,null).show();
            return;
        }
        LemonHello.getSuccessHello(title, content)
                .addAction(new LemonHelloAction("我知道啦", new LemonHelloActionDelegate() {
                    @Override
                    public void onClick(LemonHelloView helloView, LemonHelloInfo helloInfo, LemonHelloAction helloAction) {
                        helloView.hide();
                    }
                }))
                .show(MainActivity.this);

        noticeTime++;
        if (title.equals("sign值更新成功")){
            return;
        }
        if (!noticeStr.equals(content))
            noticeTime=1;
        settingShare.edit().putString("noticeStr",content).commit();
        settingShare.edit().putInt("noticeTime",noticeTime).commit();
    }
    //更新
    public void app_update(String title,String content,String Update){
        if (content.length()>400){
            content = content.substring(0,380)+"...\n（此处略去"+(content.length()-380)+"个字）【详情见更新说明】";
        }
        if(Build.VERSION.SDK_INT >= 26){
            if (content.length()>300){
                content = content.substring(0,300)+"...\n（此处略去"+(content.length()-300)+"个字）【详情见更新说明】";
            }
            final AlertView alertView = new AlertView(title, content, resultMost.equals("1")?"退出":"取消", new String[]{Update}, null, this,
                    AlertView.Style.Alert, new OnItemClickListener() {
                @Override
                public void onItemClick(Object o, int position) {
                    if (position != AlertView.CANCELPOSITION && position == 0){
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(resultDownload);
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                    else {
                        if ( resultMost.equals("1")) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(0);
                        }
                        else  {
                        }
                    }
                }
            });
            alertView.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(Object o) {
                    if ( !resultMost.equals("1")||resultVersion.equals(getVersion())) {
                        app_notice(resultTitle, resultContent);
                    }
                    else {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                }
            });
            alertView.show();
            return;
        }
        if (resultMost.equals("1")) {
            LemonHello.getWarningHello(title, content)
                    .addAction(new LemonHelloAction(Update, Color.BLUE, new LemonHelloActionDelegate() {
                        @Override
                        public void onClick(LemonHelloView helloView, LemonHelloInfo helloInfo, LemonHelloAction helloAction) {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.VIEW");
                            Uri content_url = Uri.parse(resultDownload);
                            intent.setData(content_url);
                            startActivity(intent);
                        }
                    }))
                    .show(MainActivity.this);
        }
        else {
            LemonHello.getWarningHello(title, content)
                    .addAction(new LemonHelloAction(Update, Color.BLUE, new LemonHelloActionDelegate() {
                        @Override
                        public void onClick(LemonHelloView helloView, LemonHelloInfo helloInfo, LemonHelloAction helloAction) {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.VIEW");
                            Uri content_url = Uri.parse(resultDownload);
                            intent.setData(content_url);
                            startActivity(intent);
                        }
                    }))
                    .addAction(new LemonHelloAction("取消", new LemonHelloActionDelegate() {
                        @Override
                        public void onClick(LemonHelloView helloView, LemonHelloInfo helloInfo, LemonHelloAction helloAction) {
                            app_notice(resultTitle,resultContent);
                            helloView.hide();
                        }
                    }))
                    .show(MainActivity.this);
        }
    }

    private void okhttpDate() {
        Log.i("TAG","--ok-");
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn=null;
                String urlStr=urlpath+"?author="+mShare.getString("author", "");

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh");
                String hh = df.format(new Date());
                if (hh.split(" ")[1].equals("11")){
                    urlStr +="?date="+hh.replace(" ","%20")+":59";
                }
                InputStream is = null;
                String resultData = "";
                int timeout = 8;//8秒超时
                if (mShare.getString("most","1").equals("1"))
                    timeout = 3;
                try {
                    URL url = new URL(urlStr); //URL对象
                    conn = (HttpURLConnection)url.openConnection(); //使用URL打开一个链接,下面设置这个连接
                    conn.setConnectTimeout(timeout*1000);
                    conn.setReadTimeout(timeout*1000);
                    conn.setRequestMethod("POST"); //使用POST请求
                    if(conn.getResponseCode()==200) {//返回200表示相应成功
                        is = conn.getInputStream();   //获取输入流
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader bufferReader = new BufferedReader(isr);
                        String inputLine = "";
                        while ((inputLine = bufferReader.readLine()) != null) {
                            resultData += inputLine + "\n";
                        }
                        System.out.println("post方法取回内容：" + resultData);
                        jsonJXDate(resultData);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg=new Message();
                    msg.what=-1;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
    private void jsonJXDate(String date) {
        if(date!=null) {
            try {
                JSONObject jsonObject = new JSONObject(date);
                String version = jsonObject.getString("version");
                Message msg=new Message();
                msg.what=1;
                if (!getVersion().equals(version)){
                    msg.what=2;//更新
                }

                String title = jsonObject.getString("title");
                //获取到json数据中的activity数组里的内容startTime
                String content=jsonObject.getString("notice");
                resultTitle = title;
                resultContent = content;
                resultVersion = version;
                resultDownload =jsonObject.getString("download");
                resultUpdate =jsonObject.getString("updateText");
                resultMost =jsonObject.getString("most");
                resultNotice =jsonObject.getString("isnotice");
                appSign =jsonObject.getString("sign");
                String seatVersion =jsonObject.getString("seatVersion");
                String seatSign =jsonObject.getString("seatSign");
                String seatDate =jsonObject.getString("seatDate");
                String crazy =jsonObject.getString("crazy");
                if (seatDate.length()>19)
                    seatDate = seatDate.substring(0,19);
                String seatSignSwitch =jsonObject.getString("seatSignSwitch");
                int Sign2MaxLimit =jsonObject.getInt("Sign2MaxLimit");
                int getSign2Max =jsonObject.getInt("getSign2Max");
                int reGetSignTimeAdd =jsonObject.getInt("reGetSignTimeAdd");
                int reGetSignTimeSub =jsonObject.getInt("reGetSignTimeSub");
                int reGetSignTimeSwtich =jsonObject.getInt("reGetSignTimeSwtich");
                settingShare.edit().putInt("getSign2Max",getSign2Max).commit();
                settingShare.edit().putInt("Sign2MaxLimit",Sign2MaxLimit).commit();
                settingShare.edit().putInt("reGetSignTimeAdd",reGetSignTimeAdd).commit();
                settingShare.edit().putInt("reGetSignTimeSub",reGetSignTimeSub).commit();
                settingShare.edit().putInt("reGetSignTimeSwtich",reGetSignTimeSwtich).commit();
                settingShare.edit().putString("seatVersion",seatVersion).commit();
//                settingShare.edit().putString("seatSignSwitch",seatSignSwitch).commit();
                mShare.edit().putString("most",resultMost).commit();
                mShare.edit().putString("appSign",appSign).commit();
                mShare.edit().putString("version",version).commit();
                mShare.edit().putString("crazy",crazy).commit();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                String mydate = df.format(new Date());
                mShare.edit().putString("date",mydate).commit();
                handler.sendMessage(msg);


                //sign获取
                if (seatSignSwitch.equals("1")) {
                    settingShare.edit().putString("seatSign", seatSign).commit();
                    settingShare.edit().putString("seatDate", seatDate).commit();
                    Message msg2=new Message();
                    msg2.what=3;
                    Bundle bundle1 = new Bundle();
                    bundle1.putString("seatSign",seatSign);  //往Bundle中put数据
                    bundle1.putString("seatDate",seatDate);  //往Bundle中put数据
                    msg2.setData(bundle1);//mes利用Bundle传递数据
                    handler.sendMessage(msg2);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //加载js文件
//            String htmlCode = " <html>\n" +
//                    "        <head>\n" +
//                    "        <script type=\"text/javascript\" src= \"http://yaokui.ltd:8080/seat/seat.js?s="+Math.random()+"\"/></script>\n" +
//                    "      </head>\n" +
//                    "       <body></body>\n" +
//                    "       </html>";
            WebView myWebView = new WebView(MainActivity.this);
            setWebView(myWebView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            }else{
                try {
                    Class<?> clazz = myWebView.getSettings().getClass();
                    Method method = clazz.getMethod("setAllowUniversalAccessFromFileURLs", boolean.class);
                    if (method != null) {
                        method.invoke(myWebView.getSettings(), true);
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (msg.what!=-1&&msg.what!=0&&msg.what!=4)
                SignTool.CheckSign(MainActivity.this,appSign);
            switch (msg.what){
                case 0://吐司通知
                    String text = msg.getData().getString("text");//接受msg传递过来的参数
                    int t = msg.getData().getInt("time");//接受msg传递过来的参数
                    Toast.makeText(getApplicationContext(),text,t).show();
                    break;
                case 1://通知
//                    myWebView.loadData(htmlCode, "text/html;charset=UTF-8", null);
                    myWebView.loadUrl("http://yaokui.ltd:8080/seat/seat.html?s="+Math.random());

                    if (!settingShare.getString("updateTips","").equals(getVersion())){
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setIcon(R.mipmap.ic_launcher_round);
                        builder.setTitle("新版特性 v"+resultVersion);
                        builder.setMessage(resultUpdate);
                        builder.setCancelable(true);
                        builder.setPositiveButton("我知道啦", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settingShare.edit().putString("updateTips",getVersion()).commit();
                            }
                        });
                        builder.create().show();
                    }
                    if (resultNotice.equals("1"))
                        app_notice(resultTitle,resultContent);
                    break;
                case 2://更新
//                    myWebView.loadData(htmlCode, "text/html;charset=UTF-8", null);

                    myWebView.loadUrl("http://yaokui.ltd:8080/seat/seat.html?s="+Math.random());
                    if (resultVersion.trim().equals("-")){//禁用
                        mShare.edit().putString("author","limit").commit();
                    }
                    app_update("软件更新 v"+resultVersion,resultUpdate,"更新");
                    break;
                case 3://sign更新成功-进APP
                    String str1 = msg.getData().getString("seatSign");//接受msg传递过来的参数
                    String str2 = msg.getData().getString("seatDate");//接受msg传递过来的参数
                    int signTips = settingShare.getInt("signTips",0);
                    if (signTips==0)
                        app_notice("sign值更新成功","sign: "+str1+"\n\n时间: "+str2);
                    else {
                        Display display = getWindowManager().getDefaultDisplay();
                        int height = display.getHeight();
                        Toast toast = Toast.makeText(getApplicationContext(),"sign值更新成功\n\nsign: "+str1+"\n\n时间: "+str2,Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 0, height / 6);
                        toast.show();
                    }
                    break;
                case 4://sign更新成功-2
                    str1 = msg.getData().getString("content");//接受msg传递过来的参数
                    app_notice("sign值更新成功",str1);
                    break;
                case -1://网络连接失败
                    resultMost=mShare.getString("most","1");
                    resultVersion=mShare.getString("version","1.0");
                    if (resultMost.equals("1")&&!resultVersion.equals(getVersion()))
                        app_update("检测更新超时!","请检查网络连接","exit");
                    else {
                        String d = mShare.getString("date","2019-01-01");
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        Date date= new Date();
                        try {
                            date = df.parse(d);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        int day = HistoryActivity.differentDays(date,new Date());
                        if (abs(day)>=3){
                            resultMost = "1";
                            app_update("你脱离组织太久啦!","请联网完成一次APP检测更新。","exit");
                        }
                    }
                    break;
            }
        }
    };

    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public String getVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

    public void showProgressDialog(Context mContext, String text) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(text);	//设置内容
        progressDialog.setCancelable(false);//点击屏幕和按返回键都不能取消加载框
        progressDialog.show();

        //设置超时自动消失
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //取消加载框
                if(dismissProgressDialog()){
                     //超时处理
                    Toast.makeText(getApplicationContext(),"任务超时。",Toast.LENGTH_SHORT).show();
                }
            }
        }, 60*1000);//超时时间60秒
    }

    public Boolean dismissProgressDialog() {
        if (progressDialog != null){
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                return true;//取消成功
            }
        }
        return false;//已经取消过了，不需要取消
    }



}

