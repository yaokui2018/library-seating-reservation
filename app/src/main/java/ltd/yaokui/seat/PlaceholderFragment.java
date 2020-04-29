package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bravin.btoast.BToast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ltd.yaokui.seat.utils.SignUtil;

import static android.content.Context.MODE_PRIVATE;
import static java.lang.Integer.parseInt;

/**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {
    private static final int REQUEST_CODE_SCANHtml = 98;//记录页
    private int dbVersion = 1;
    WebView WEBVIEW;
    //sign值获取时间
//    private long getSignTime = 0;

    SharedPreferences mShare;

    SwipeRefreshLayout refreshLayout;

    SharedPreferences settingShare;

    boolean skipLoading,autoSeat,autoBook;
    int bookshow,sort,countID;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        public PlaceholderFragment(){

        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            settingShare = getActivity().getSharedPreferences("setting", MODE_PRIVATE);
            mShare = getActivity().getSharedPreferences("author", MODE_PRIVATE);

            View rootView = inflater.inflate(R.layout.fragment_crazy, container, false);
            int i = getArguments().getInt(ARG_SECTION_NUMBER);
            final WebView webView =  rootView.findViewById(R.id.crazy_webview);

            refreshLayout = rootView.findViewById(R.id.swipe_fresh);

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

            if (webView.getUrl()==null) {
                setWebView(webView);
                if (i == 3) {
                    webView.loadUrl("file:///android_asset/allStuId.html");
                    WEBVIEW = webView;
                }
                else if (i == 2)
                    webView.loadUrl("file:///android_asset/crazyMode22.html");
                else
                    webView.loadUrl("file:///android_asset/crazyMode2.html");
            }
            return rootView;
        }


    /**
     * 与js进行交互
     */
    public class JsInterface {
        List<Db> list = new ArrayList();

        @JavascriptInterface
        public String getSeatNum() {
            return settingShare.getString("seatNum","");
        }
        @JavascriptInterface
        public String getSeatNumRemark() {
            return settingShare.getString("seatNumRemark","");
        }
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(getContext(), toast, Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void showToast2(String toast,int time) {
            Toast.makeText(getContext(), toast, time).show();
        }
        //自定义吐司
        @JavascriptInterface
        public void showMyToast(String toast,int flag) {//flag:0.error 1.success 2.warn 3.info
            switch (flag){
                case 2:
                    BToast.warning(getContext())
                            .text(toast)
                            .show();
                    break;
                case 0:
                    BToast.error(getContext())
                            .text(toast)
                            .show();
                    break;
                case 1:
                    BToast.success(getContext())
                            .text(toast)
                            .show();
                    break;
                case 3:
                    BToast.info(getContext())
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
            Toast.makeText(getContext(), toast, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), toast, Toast.LENGTH_SHORT).show();
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
        //疯狂抢座
        @JavascriptInterface
        public void toCrazy(String seatNum,String seatNumRemark){
                Toast.makeText(getActivity(),"嗯哼~",Toast.LENGTH_SHORT).show();
                return;
        }
        @JavascriptInterface
        public void setListNumToken(int index,String token) {
            list.get(index).setToken(token);
        }
        @JavascriptInterface
        public String getNumToken(int index) {
            return list.get(index).getToken();
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
                Toast.makeText(getContext(),"已切换至兼容模式",Toast.LENGTH_SHORT).show();
            }
            else {
                settingShare.edit().putBoolean("AllListMode", true).commit();
                Toast.makeText(getContext(),"已切换至急速模式",Toast.LENGTH_SHORT).show();
            }
        }
        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(text);
//            Toast.makeText(MainActivity.this, "内容已复制至剪贴板。", Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void copyCount(int number) {
            Db db = selectByNum(number);
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(db.getNumber()+","+db.getPassword()+","+db.getRemark());
            Toast.makeText(getContext(), "账号信息已复制至剪贴板。", Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public String getSeatVersion() {//获取官方软件版本号
            return settingShare.getString("seatVersion","2.4.0");
        }
        //获取官方软件sign
        @JavascriptInterface
        public String getSeatSign() {
            return SignUtil.getSeatSign();
        }
        @JavascriptInterface
        public String getSeatDate() {
            return settingShare.getString("seatDate","-");
        }
        @JavascriptInterface
        public void setSeatSign(String sign,String date) {
//            settingShare.edit().putString("seatSign", sign).commit();
//            settingShare.edit().putString("seatDate", date).commit();
//            insertSign("0",sign,date);
        }
        //离开
        @JavascriptInterface
        public void leave(final String i,final String number,final String id,final String seatId,final String userId) {
            AlertDialog.Builder builder11 = new AlertDialog.Builder(getActivity());
            builder11.setIcon(R.mipmap.ic_launcher);
            builder11.setTitle("离开");
            builder11.setMessage("请选择你要进行的操作。");
            builder11.setCancelable(true);
            builder11.setNeutralButton("长离", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    Toast.makeText(getApplicationContext(),"操作中，请稍等",Toast.LENGTH_SHORT).show();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WEBVIEW.loadUrl("javascript:Leave("+id+","+i+","+userId+",2);");
                        }
                    });
                }
            });
            builder11.setPositiveButton("暂离", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WEBVIEW.loadUrl("javascript:Leave("+id+","+i+","+userId+",1);");
                        }
                    });
                }
            });
            builder11.setNegativeButton("回座", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WEBVIEW.loadUrl("javascript:backSeat("+id+","+i+","+number+","+seatId+");");
                        }
                    });
                }

            });
            builder11.create().show();
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
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WEBVIEW.reload();
                        }
                    });
                    dialog.dismiss();
                }
            });
            builder.show();
        }

        //获取官方软件sign2
        @JavascriptInterface
        public String getSeatSign2() {
            return SignUtil.getSeatSign2();
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

        //预约记录
        @JavascriptInterface
        public void addHistory(String room,String canSeat,String day,String startTime,String SuserPhysicalCard){
            //创建一个DatabaseHelper对象
            DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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
            DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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
            DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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

        //座位地图
        @JavascriptInterface
        public void roomMap(String room){
            Intent mapintent = new Intent(getContext(),SeatMapActivity.class);
            mapintent.putExtra("room", room);
            startActivity(mapintent);
        }
        //记录-同官方
        @JavascriptInterface
        public void record(String number,String pass,int i){
            Intent mapintent = new Intent(getContext(),RecordActivity.class);
            mapintent.putExtra("number", number);
            mapintent.putExtra("pass", pass);
            mapintent.putExtra("i", i);
            startActivityForResult(mapintent,REQUEST_CODE_SCANHtml);
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
        public void exit() {
            System.exit(0);
        }

    }

    //数据库操作
    public long insert(int number,String password,String remark){//插入
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("number",number);
        values.put("password",password);
        values.put("remark", remark);
        db1.update("account", values, "id=?", new String[]{id+""});
    }
    public void updateToken(String token,String number){//更新token
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("token", token);
        db1.update("account", values, "number=?", new String[]{number});
    }
    public Db selectByNum(int number){//通过学号查询
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("account", "id=?", new String[]{id+""})>0;
    }
    public boolean deleteAll(){//删除所有
        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("account",null,null)>0;
    }
//    //sign插入
//    public long insertSign(String id_,String sign,String date){
//        if (sign.length()!=46)//格式不对
//            return -2;
//        //创建一个DatabaseHelper对象
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        //取得一个只读的数据库对象
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        //创建游标对象
//        Cursor cursor = db1.query("sign", new String[]{"id","sign","date","id_"}, "date=?", new String[]{date+""}, null, null, null, null);
//        //利用游标遍历所有数据对象
//        while(cursor.moveToNext()){
//            return -1;
//        }
//        ContentValues values = new ContentValues();
//        //像ContentValues中存放数据
//        values.put("sign",sign);
//        values.put("date",date);
//        values.put("id_", id_);
//        return db1.insert("sign",null,values);
//    }
//    //sign删除
//    public boolean deleteSign(int id){
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        return db1.delete("sign", "id=?", new String[]{id+""})>0;
//    }
//    //sign查询
//    public Sign querySign(){
//        int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
//        int reGetSignTimeSub = settingShare.getInt("reGetSignTimeSub",0);
//        int reGetSignTimeSwtich = settingShare.getInt("reGetSignTimeSwtich",0);
//        String reGetSignTimeS="";
//        if (reGetSignTimeSwtich==1){
//            Calendar instance = Calendar.getInstance();
//            int year = instance.get(Calendar.YEAR);//获取年份
//            int month=instance.get(Calendar.MONTH)+1;//获取月份
//            int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
//            int hour=instance.get(Calendar.HOUR_OF_DAY);//小时
//            reGetSignTimeS = "date >= '"+year+"-"+(month<10?"0":"")+month+"-"+(day<10?"0":"")+day+" "+(hour<10?"0":"")+hour+":00:00' and";
//        }
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        //创建游标对象
//        Cursor cursor = db1.query("sign", new String[]{"id","sign","date","id_"}, reGetSignTimeS+" date > datetime('now','-"+reGetSignTimeAdd+" seconds','localtime') and date < datetime('now','"+reGetSignTimeSub+" seconds','localtime')", null, null, null, "date", "1");
//        //利用游标遍历所有数据对象
//        Sign db = new Sign();
//        if(cursor.moveToNext()){
//            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
//            db.setSign(cursor.getString(cursor.getColumnIndex("sign")));
//            db.setDate(cursor.getString(cursor.getColumnIndex("date")));
//            db.setId_(cursor.getInt(cursor.getColumnIndex("id_")));
//        }
//        return db;
//    }
//
//
//    //sign2删除
//    public boolean deleteSign2(int id){
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        return db1.delete("sign2", "id=?", new String[]{id+""})>0;
//    }
//    //sign2查询
//    public Sign2 querySign2(){
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        //创建游标对象
//        Cursor cursor = db1.query("sign2", new String[]{"id","sign2"}, null, null, null, null, "id", "1");
//        //利用游标遍历所有数据对象
//        Sign2 db = new Sign2();
//        if(cursor.moveToNext()){
//            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
//            db.setSign2(cursor.getString(cursor.getColumnIndex("sign2")));
//
//            deleteSign2(cursor.getInt(cursor.getColumnIndex("id")));
//        }
//        return db;
//    }
//    //sign2查询数量
//    public int querySign2Size(){
//        DatabaseHelper dbHelper1 = new DatabaseHelper(getContext(), "seat.db",dbVersion);
//        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
//        //创建游标对象
//        Cursor cursor = db1.rawQuery("select count(id) from sign2 ",null);
//        cursor.moveToFirst();
//        int count = cursor.getInt(0);
//        cursor.close();
//        return count;
//    }






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

        String cacheDirPath = getContext().getFilesDir().getAbsolutePath() + "cache/";
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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webview,true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            webview.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }else{
            try {
                Class<?> clazz = webview.getSettings().getClass();
                Method method = clazz.getMethod("setAllowUniversalAccessFromFileURLs", boolean.class);
                if (method != null) {
                    method.invoke(webview.getSettings(), true);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setAppCacheMaxSize(1024*1024*8);
        String appCachePath = getContext().getCacheDir().getAbsolutePath();
        webview.getSettings().setAppCachePath(appCachePath);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAppCacheEnabled(true);

        webview.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }
}

    
    