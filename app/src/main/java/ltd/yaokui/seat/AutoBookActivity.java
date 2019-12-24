package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.a520wcf.yllistview.YLListView;
import com.leon.lib.settingview.LSettingItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;


public class AutoBookActivity extends BaseActivity {

    Handler mHandler;
    SharedPreferences settingShare;
    boolean autoSeat,autoBook,auto_book_1minute,auto_book_efficient,auto_book_showToast;
    int auto_book_maxcount,auto_Try_Time;
    String auto_book_time,auto_book_seat,auto_book_room;


    private YLListView listView;
    List<AutoBook> dbList;
    static DemoAdapter demoAdapter;


    class DemoAdapter extends BaseAdapter {

        public void refresh() {
            dbList.clear();
            dbList.addAll(query());
            LinearLayout.LayoutParams Params = (LinearLayout.LayoutParams) listView.getLayoutParams();
            Params.height = dbList.size()>0?dbList.size()*50:50;// 当控件的高强制设成
            Params.height = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Params.height, getResources().getDisplayMetrics()));
            listView.setLayoutParams(Params);
            notifyDataSetChanged();
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
            View view = View.inflate(getApplicationContext(),R.layout.list_view_autobook,null);

            TextView textView = view.findViewById(R.id.text_id);
            textView.setText(position + 1 + "");
            textView = view.findViewById(R.id.text_room);
            String room = dbList.get(position).getRoom();
            switch (room){
                case "9":
                    room = "202";
                    break;
                case "10":
                    room = "203";
                    break;
                case "12":
                    room = "101";
                    break;
                default:
                    room = "error";
            }
            textView.setText(room+"自习室");
            textView = view.findViewById(R.id.text_seat);
            textView.setText(dbList.get(position).getSeat());
            Button button = view.findViewById(R.id.text_del);
            button.setOnClickListener(new View.OnClickListener() {
                int pos = position;
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AutoBookActivity.this);
                    builder.setTitle("温馨提示");
                    builder.setMessage("确定要删除该座位吗？");
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
                }
            });
            return view;
        }

    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);
        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);
        auto_book_1minute = settingShare.getBoolean("auto_book_1minute",false);
        //高效模式
        auto_book_efficient = settingShare.getBoolean("auto_book_efficient",false);
        auto_book_showToast = settingShare.getBoolean("auto_book_showToast",true);
        //自动预约最大座位数
        auto_book_maxcount = settingShare.getInt("auto_book_maxcount",2);
        //预约时间
        auto_book_time = settingShare.getString("auto_book_time","12:00:00");
        //自动重试时间
        auto_Try_Time = settingShare.getInt("auto_Try_Time",0);

        setContentView(R.layout.activity_autobook);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("自动预约");

        listView = (YLListView) findViewById(R.id.auto_book_listView);
        dbList = query();
        LinearLayout.LayoutParams Params = (LinearLayout.LayoutParams) listView.getLayoutParams();
        Params.height = dbList.size()>0?dbList.size()*50:50;// 当控件的高强制设成
        Params.height = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Params.height, getResources().getDisplayMetrics()));
        listView.setLayoutParams(Params);
        demoAdapter = new DemoAdapter();
        listView.setAdapter(demoAdapter);

        //YLListView默认有头和底  处理点击事件位置注意减去
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position=position-listView.getHeaderViewsCount();
            }
        });

        LSettingItem mSettingItemOne = (LSettingItem) findViewById(R.id.auto_book_switch);
        if (autoBook) {
            mSettingItemOne.clickOn();
        }
        else{
            View view = findViewById(R.id.auto_book_content);
            view.setVisibility(View.GONE);
        }
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("autoBook",isChecked).commit();
                View view = findViewById(R.id.auto_book_content);

                Intent intentOne = new Intent(getApplicationContext(), AutoBookService.class);
                if (isChecked){
                    view.setVisibility(View.VISIBLE);
                    startService(intentOne);
                }
                else {
                    view.setVisibility(View.GONE);
                    stopService(intentOne);
                }

            }
        });


        //高效模式
        mSettingItemOne = (LSettingItem) findViewById(R.id.auto_book_efficient);
        if (auto_book_efficient)
            mSettingItemOne.clickOn();
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("auto_book_efficient",isChecked).commit();
            }
        });
        //showToast
        mSettingItemOne = (LSettingItem) findViewById(R.id.auto_book_showToast);
        if (auto_book_showToast)
            mSettingItemOne.clickOn();
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("auto_book_showToast",isChecked).commit();
            }
        });

        String s ="";

        //一分钟重试
        mSettingItemOne = (LSettingItem) findViewById(R.id.auto_book_1minute);
        String[] items = new String[] {  "关闭","100毫秒","1秒","1分钟"};
        mSettingItemOne.setRightText(items[auto_Try_Time]);
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AutoBookActivity.this);
                final String[] items = new String[] {  "关闭","100毫秒","1秒","1分钟"};
                builder.setTitle("选择自动重试时间");
                builder.setSingleChoiceItems(items,auto_Try_Time, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_book_1minute);
                        lSettingItem.setRightText(items[which]);
                        auto_Try_Time = which;
                        settingShare.edit().putInt("auto_Try_Time",which).commit();
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        //预约时间
        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_book_time);
        s = auto_book_time;
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                final EditText inputServer = new EditText(AutoBookActivity.this);
                auto_book_time = settingShare.getString("auto_book_time","12:00:00");
                inputServer.setText(auto_book_time);
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AutoBookActivity.this);
                builder.setTitle("自动预约时间（格式：HH:mm:ss)").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String text = inputServer.getText().toString();
                        if (!isTime(text)){
                            Toast.makeText(getApplicationContext(),"时间输入不合法。",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        settingShare.edit().putString("auto_book_time",text).commit();
                        auto_book_time = text;
                        Intent intentOne = new Intent(getApplicationContext(), AutoBookService.class);
                        stopService(intentOne);
                        startService(intentOne);
//                        Toast.makeText(getApplicationContext(),"时间修改成功。",Toast.LENGTH_SHORT).show();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_book_time);
                        lSettingItem.setRightText(inputServer.getText().toString());
                    }
                });
                builder.show();
            }
        });
        //预约日志
        lSettingItem = (LSettingItem) findViewById(R.id.auto_book_logs);
        final String auto_book_logs = settingShare.getString("auto_book_logs","");
        final String auto_book_logsTime = settingShare.getString("auto_book_logsTime","无");
        lSettingItem.setRightText(auto_book_logsTime);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AutoBookActivity.this);
                builder.setIcon(R.mipmap.ic_launcher_round);
                builder.setTitle("预约日志 : "+auto_book_logsTime);
                builder.setMessage(auto_book_logs);
                builder.setCancelable(true);
                builder.setPositiveButton("我知道啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        });

        auto_book_seat = auto_book_room ="";
        //自习室
        lSettingItem = (LSettingItem) findViewById(R.id.auto_book_room);
        s = "请选择";
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AutoBookActivity.this);
                final String[] items = new String[] {  "101","202","203"};
                builder.setTitle("选择自习室");
                builder.setSingleChoiceItems(items,-1, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_book_room);
                        lSettingItem.setRightText(items[which]+"自习室");
                        lSettingItem = (LSettingItem) findViewById(R.id.auto_book_seat);
                        lSettingItem.setRightText("请选择");
                        auto_book_room = items[which];
                        auto_book_seat = "";
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        //座位
        lSettingItem = (LSettingItem) findViewById(R.id.auto_book_seat);
        s = "请选择";
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AutoBookActivity.this);
                String[] seat = new String[] {};
                switch (auto_book_room){
                    case "101":
                        seat =  new String[] {"1A","1B","1C","1D","2A","2B","2C","2D","3A","3B","3C","3D","4A","4B","4C","4D","5A","5B","5C","5D","6A","6B","6C","6D","7A","7B","7C","7D","8A","8B","8C","8D","9A","9B","9C","9D","10A","10B","10C","10D","11A","11B","11C","11D","12A","12B","12C","12D","13A","13B","13C","13D","14A","14B","14C","14D","15A","15B","15C","15D","16A","16B","16C","16D","17A","17B","17C","17D","18A","18B","18C","18D","19A","19B","19C","19D","20A","20B","20C","20D","21A","21B","21C","21D","22A","22B","22C","22D","23A","23B","23C","23D","24A","24B","24C","24D","25A","25B","25C","25D","26A","26B","26C","26D","27A","27B","27C","27D","28A","28B","28C","28D","29A","29B","29C","29D","30A","30B","30C","30D","31A","31B","31C","31D","32A","32B","32C","32D","33A","33B","33C","33D","34A","34B","34C","34D","35A","35B","35C","35D","36A","36B","36C","36D","37A","37B","37C","37D","38A","38B","38C","38D","39A","39B","39C","39D","40A","40B","40C","40D","41A","41B","41C","41D","42A","42B","42C","42D","43A","43B","43C","43D","44A","44B","44C","44D","45A","45B","45C","45D","46A","46B","46C","46D","47A","47B","47C","47D","48A","48B","48C","48D","49A","49B","49C","49D","50A","50B","50C","50D","54A","54B","54C","54D","51A","51B","51C","51D","52A","52B","52C","52D","53A","53B","53C","53D","55A","55B","55C","55D","56A","56B","56C","56D","57A","57B","57C","57D","58A","58B","58C","58D","59A","59B","59C","59D","60A","60B","60C","60D","61A","61B","61C","61D","63A","63B","63C","63D","64A","64B","64C","64D","65A","65B","65C","65D","66A","66B","66C","66D","67A","67B","67C","67D","68A","68B","68C","68D","69A","69B","69C","69D","70A","70B","70C","70D","71A","71B","71C","71D","72A","72B","72C","72D","73A","73B","73C","73D","74A","74B","74C","74D","75A","75B","75C","75D","76A","76B","76C","76D"};
                        break;
                    case "202":
                        seat =  new String[] {"1A","1B","1C","1D","1E","1F","2A","2B","2C","2D","2E","2F","3A","3B","3C","3D","3E","3F","4A","4B","4C","4D","4E","4F","5A","5B","5C","5D","5E","5F","6A","6B","6C","6D","6E","6F","7A","7B","7C","7D","7E","7F","8A","8B","8C","8D","8E","8F","9A","9B","9C","9D","9E","9F","10A","10B","10C","10D","10E","10F","11A","11B","11C","11D","11E","11F","12A","12B","12C","12D","13A","13B","13C","13D","14A","14B","14C","14D","15A","15B","15C","15D","16A","16B","16C","16D","17A","17B","17C","17D","18A","18B","18C","18D","19A","19B","19C","19D","20A","20B","20C","20D","21A","21B","21C","21D","22A","22B","22C","22D","23A","23B","23C","23D","24A","24B","24C","24D","25A","25B","25C","25D","26A","26B","26C","26D","27A","27B","27C","27D","28A","28B","28C","28D","29A","29B","29C","29D","30A","30B","30C","30D","31A","31B","31C","31D","32A","32B","32C","32D","33A","33B","33C","33D","34A","34B","34C","34D","34E","34F","35A","35B","35C","35D","35E","35F","36A","36B","36C","36D","36E","36F","37A","37B","37C","37D","37E","37F","38A","38B","38C","38D","38E","38F","39A","39B","39C","39D","39E","39F","40A","40B","40C","40D","40E","40F","41A","41B","41C","41D","41E","41F","42A","42B","42C","42D","42E","42F","43A","43B","43C","43D","43E","43F","44A","44B","44C","44D","44E","44F","45A","45B","45C","45D","45E","45F","46A","46B","46C","46D","46E","46F","47A","47B","47C","47D","47E","47F","48A","48B","48C","48D","48E","48F","49A","49B","49C","49D","49E","49F","50A","50B","50C","50D","50E","50F","51A","51B","51C","51D","51E","51F","52A","52B","52C","52D","52E","52F","53A","53B","53C","53D","54A","54B","54C","54D","55A","55B","55C","55D","56A","56B","56C","56D","57A","57B","57C","57D","58A","58B","58C","58D","59A","59B","59C","59D","60A","60B","60C","60D","61A","61B","61C","61D","62A","62B","62C","62D","63A","63B","63C","63D","64A","64B","64C","64D","65A","65B","65C","65D","66A","66B","66C","66D","67A","67B","67C","67D","68A","68B","68C","68D"};
                        break;
                    case "203":
                        seat =  new String[] {"1A","1B","1C","1D","1E","1F","2A","2B","2C","2D","2E","2F","3A","3B","3C","3D","3E","3F","4A","4B","4C","4D","4E","4F","5A","5B","5C","5D","5E","5F","6A","6B","6C","6D","6E","6F","7A","7B","7C","7D","7E","7F","8A","8B","8C","8D","8E","8F","9A","9B","9C","9D","9E","9F","10A","10B","10C","10D","10E","10F","11A","11B","11C","11D","11E","11F","12A","12B","12C","12D","12E","12F","13A","13B","13C","13D","13E","13F","14A","14B","14C","14D","14E","14F","15A","15B","15C","15D","15E","15F","16A","16B","16C","16D","16E","16F","17A","17B","17C","17D","17E","17F","18A","18B","18C","18D","18E","18F","19A","19B","19C","19D","19E","19F","20A","20B","20C","20D","20E","20F","21A","21B","21C","21D","21E","21F","22A","22B","22C","22D","22E","22F","23A","23B","23C","23D","23E","23F","24A","24B","24C","24D","24E","24F","25A","25B","25C","25D","25E","25F","26A","26B","26C","26D","26E","26F","27A","27B","27C","27D","27E","27F","28A","28B","28C","28D","28E","28F","29A","29B","29C","29D","29E","29F","30A","30B","30C","30D","30E","30F","31A","31B","31C","31D","31E","31F","32A","32B","32C","32D","32E","32F","33A","33B","33C","33D","33E","33F","34A","34B","34C","34D","34E","34F","35A","35B","35C","35D","35E","35F","36A","36B","36C","36D","36E","36F","37A","37B","37C","37D","37E","37F","38A","38B","38C","38D","38E","38F","39A","39B","39C","39D","39E","39F","40A","40B","40C","40D","40E","40F","41A","41B","41C","41D","41E","41F","42A","42B","42C","42D","42E","42F","43A","43B","43C","43D","43E","43F","44A","44B","44C","44D","44E","44F","45A","45B","45C","45D","45E","45F","46A","46B","46C","46D","46E","46F","47A","47B","47C","47D","47E","47F","48A","48B","48C","48D","48E","48F","49A","49B","49C","49D","49E","49F","50A","50B","50C","50D","50E","50F","51A","51B","51C","51D","51E","51F","52A","52B","52C","52D","52E","52F","53A","53B","53C","53D","53E","53F","54A","54B","54C","54D","54E","54F","55A","55B","55C","55D","55E","55F","56A","56B","56C","56D","56E","56F","57A","57B","57C","57D","57E","57F","58A","58B","58C","58D","58E","58F","60A","60B","60C","60D","60E","60F","61A","61B","61C","61D","61E","61F","62A","62B","62C","62D","62E","62F","63A","63B","63C","63D","63E","63F","64A","64B","64C","64D","64E","64F","65A","65B","65C","65D","65E","65F","66A","66B","66C","66D","66E","66F","67A","67B","67C","67D","67E","67F","68A","68B","68C","68D","68E","68F","69A","69B","69C","69D","69E","69F","70A","70B","70C","70D","70E","70F"};
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),"请先选择自习室。",Toast.LENGTH_SHORT).show();
                        return;
                }
                final String[] items = seat;
                builder.setTitle("选择座位["+auto_book_room+"]");
                builder.setSingleChoiceItems(items,-1, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_book_seat);
                        lSettingItem.setRightText(items[which]);
                        auto_book_seat = items[which];
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        //添加
        Button addbtn = findViewById(R.id.auto_book_add);
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (auto_book_maxcount<=dbList.size()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(AutoBookActivity.this);
                    builder.setTitle("抱歉！");
                    builder.setMessage("你的座位添加数已达限制："+auto_book_maxcount+"");
                    builder.setCancelable(true);
                    builder.setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.setNeutralButton("解锁更多", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final EditText inputServer = new EditText(AutoBookActivity.this);
                            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AutoBookActivity.this);
                            builder.setTitle("输入口令：").setView(inputServer)
                                    .setNegativeButton("取消", null);
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    Boolean pass = false;
                                    String text = inputServer.getText().toString();
                                    if (auto_book_maxcount<3&&text.trim().equals("好哥哥")){
                                        pass = true;
                                    }
                                    else if (auto_book_maxcount<6&&text.trim().equals("薄荷你玩")){
                                        pass = true;
                                    }
                                    else if (auto_book_maxcount<999&&text.trim().equals("yaokui.ltd")){
                                        pass = true;
                                    }
                                    if (pass) {
                                        auto_book_maxcount++;
                                        Toast.makeText(getApplicationContext(), "口令正确：可添加座位数+1。", Toast.LENGTH_SHORT).show();
                                        settingShare.edit().putInt("auto_book_maxcount",auto_book_maxcount).commit();
                                    }
                                    else
                                        Toast.makeText(getApplicationContext(), "口令不正确：多试几次，你可以的！", Toast.LENGTH_SHORT).show();

                                }
                            });
                            builder.show();
                            if (auto_book_maxcount<3){
                                Toast.makeText(getApplicationContext(),"口令提示：好哥哥。",Toast.LENGTH_SHORT).show();
                            }
                        }

                    });
                    builder.create().show();
                    return;
                }
                if (auto_book_seat==""){
                    Toast.makeText(getApplicationContext(),"请先选择座位。",Toast.LENGTH_SHORT).show();
                    return;
                }
                String room = "";
                switch (auto_book_room){
                    case "202":
                        room = "9";
                        break;
                    case "203":
                        room = "10";
                        break;
                    case "101":
                        room = "12";
                        break;
                }
                long num = insert(room,auto_book_seat,"");
                if (num>0){
                    demoAdapter.refresh();
                    Toast.makeText(getApplicationContext(),"座位添加成功。",Toast.LENGTH_SHORT).show();
                }
                else if (num==-1){
                    Toast.makeText(getApplicationContext(),"该座位已存在",Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(getApplicationContext(),"座位添加失败",Toast.LENGTH_SHORT).show();
            }
        });
        //立即执行
        addbtn = findViewById(R.id.auto_book_go);
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentOne = new Intent(getApplicationContext(), AutoBookService.class);
                stopService(intentOne);
                startService(intentOne);
                startService(intentOne);
            }
        });

    }
    //数据库操作
    public long insert(String room,String seat,String daytime){//插入
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
        //取得一个只读的数据库对象
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("auto_book", new String[]{"id","room","seat","daytime"}, "room=? and seat=?", new String[]{room,seat}, null, null, null, null);
        //利用游标遍历所有数据对象
        while(cursor.moveToNext()){
            return -1;
        }
        ContentValues values = new ContentValues();
        //像ContentValues中存放数据
        values.put("room",room);
        values.put("seat",seat);
        values.put("daytime", daytime);
        return db1.insert("auto_book",null,values);
    }
    public void update(String room,String seat,String daytime){//更新
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("room",room);
        values.put("seat",seat);
        values.put("daytime", daytime);
        db1.update("auto_book", values, "room=? and seat=?", new String[]{room,seat});
    }
    public List<AutoBook> query(){//查询
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("auto_book", new String[]{"id","room","seat","daytime"}, null, null, null, null, "id", null);
        //利用游标遍历所有数据对象
        List<AutoBook> list = new ArrayList<>();
        while(cursor.moveToNext()){
            AutoBook db = new AutoBook();
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setRoom(cursor.getString(cursor.getColumnIndex("room")));
            db.setSeat(cursor.getString(cursor.getColumnIndex("seat")));
            db.setDaytime(cursor.getString(cursor.getColumnIndex("daytime")));
            list.add(db);
        }
        return list;
    }
    public boolean delete(int id){//删除
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("auto_book", "id=?", new String[]{id+""})>0;
    }
    public boolean deleteAll(){//删除所有
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("auto_book",null,null)>0;
    }

    /**
     * actionBar监听
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //判断时间是否合法：HH:mm:ss
    public static boolean isTime(String time){
        Pattern p = Pattern.compile("((((0[0-9])|([1][0-9])|([2][0-4]))\\:([0-5][0-9])\\:([0-5][0-9])))$");
        return p.matcher(time).matches();
    }

}
