package ltd.yaokui.seat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import static java.lang.Math.abs;

public class RecordActivity extends AppCompatActivity {
    WebView webView;
    String number,pass;
    private int writeflag;
    private int REQUEST_CODE_SCAN=1;
    private int REQUEST_CODE_SCANHtml=98;//界面返回码
    private int status=-1;//预约状态
    private int SeaStatus=0;//入座状态
    private int i;
    SharedPreferences settingShare;
    long getSignTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_map);
        webView = findViewById(R.id.seatmap);
        setWebView(webView);

        settingShare = getApplication().getSharedPreferences("setting",MODE_PRIVATE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }else{
            writeflag = 1;
        }

        if(writeflag == 0){
            Toast.makeText(RecordActivity.this, "没有存储权限！\n请接收权限申请或前往设置添加权限！", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(RecordActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }else{
            //进行相机权限的检测，如果没有授权，申请权限。
            if(ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(RecordActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }


        String ua = webView.getSettings().getUserAgentString();//原来获取的UA
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");

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

        Intent intent = getIntent();
        number = intent.getStringExtra("number");
        pass = intent.getStringExtra("pass");
        i = intent.getIntExtra("i",-1);
        webView.loadUrl("file:///android_asset/www/record.html");

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }

    /**
     * 与js进行交互
     */
    public class JsInterface {
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(RecordActivity.this, toast, Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void debug_(String data) {
            System.out.println(data);
        }
        @JavascriptInterface
        public void close() {
            Intent intent = new Intent();
            // 获取用户计算后的结果
            intent.putExtra("status", status); //将计算的值回传回去
            intent.putExtra("SeatStatus", SeaStatus); //1.取消 2.入座
            intent.putExtra("number", number); //将计算的值回传回去
            intent.putExtra("i", i); //将计算的值回传回去
            //通过intent对象返回结果，必须要调用一个setResult方法，
            //setResult(resultCode, data);第一个参数表示结果返回码，一般只要大于1就可以，但是
            setResult(REQUEST_CODE_SCANHtml, intent);
            finish();
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
        @JavascriptInterface
        public String getNum() {
            return number;
        }
        @JavascriptInterface
        public String getPass() {
            return pass;
        }
        @JavascriptInterface
        public void setStatus(int mystatus,int seatSta) {
            status = mystatus;//0.未预约 1.已预约
            SeaStatus = seatSta;//1.取消 2.入座 0.离席
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
        public String getNumTokenByNum(int number) {
            return selectByNum(number).getToken();
        }
        //离开
        @JavascriptInterface
        public void leave(final String i,final String number,final String id,final String seatId,final String userId) {
            AlertDialog.Builder builder11 = new AlertDialog.Builder(RecordActivity.this);
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
        @JavascriptInterface
        public void scan() {
            Intent intent = new Intent(RecordActivity.this, CaptureActivity.class);
            /*ZxingConfig是配置类
             *可以设置是否显示底部布局，闪光灯，相册，
             * 是否播放提示音  震动
             * 设置扫描框颜色等
             * 也可以不传这个参数
             * */
            ZxingConfig config = new ZxingConfig();
            config.setShowbottomLayout(false);//底部布局（包括闪光灯和相册）
            config.setPlayBeep(false);//是否播放扫描声音 默认为true
            config.setShake(true);//是否震动  默认为true
            config.setDecodeBarCode(true);//是否扫描条形码 默认为true
            config.setReactColor(R.color.red);//设置扫描框四个角的颜色 默认为白色
//            config.setFrameLineColor(R.color.red);//设置扫描框边框颜色 默认无色
            config.setScanLineColor(R.color.red);//设置扫描线的颜色 默认白色
            config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
            intent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        }

    }

    public Db selectByNum(int number){//通过学号查询
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",4);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
//                Toast.makeText(this,"扫描结果为：" + content,Toast.LENGTH_SHORT).show();
                webView.loadUrl("javascript:onmarked('"+content+"')");
            }
        }
    }


    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
                //弹出提示，可以有多种方式
                if (!webView.getUrl().equals("file:///android_asset/www/record.html")){
                    Toast.makeText(getApplicationContext(), "back", Toast.LENGTH_SHORT).show();
                    webView.goBack();
                }
                else {
//                    if ((System.currentTimeMillis() - exitTime) > 2000) {
//                            Toast.makeText(getApplicationContext(), "再按一次关闭页面", Toast.LENGTH_SHORT).show();
//                            exitTime = System.currentTimeMillis();
//                    } else {
                        Intent intent = new Intent();
                        // 获取用户计算后的结果
                        intent.putExtra("status", status); //将计算的值回传回去
                        intent.putExtra("SeatStatus", SeaStatus); //1.取消 2.入座
                        intent.putExtra("number", number); //将计算的值回传回去
                        intent.putExtra("i", i); //将计算的值回传回去
                        //通过intent对象返回结果，必须要调用一个setResult方法，
                        //setResult(resultCode, data);第一个参数表示结果返回码，一般只要大于1就可以，但是
                        setResult(REQUEST_CODE_SCANHtml, intent);
                        finish();
//                    }
                }
            return true;
        }

        return super.onKeyDown(keyCode, event);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",5);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",5);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign2", "id=?", new String[]{id+""})>0;
    }
    //sign2查询
    public Sign2 querySign2(){
        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",5);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",5);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.rawQuery("select count(id) from sign2 ",null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

}
