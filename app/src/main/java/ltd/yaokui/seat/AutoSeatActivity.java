package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import java.util.List;
import java.util.regex.Pattern;


public class AutoSeatActivity extends BaseActivity {


    SharedPreferences settingShare;
    boolean autoSeat,autoBook;
    String auto_seat_time;


    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingShare = this.getSharedPreferences("setting", MODE_PRIVATE);
        autoSeat = settingShare.getBoolean("autoSeat", false);
        autoBook = settingShare.getBoolean("autoBook", false);
        //入座时间
        auto_seat_time = settingShare.getString("auto_seat_time", "06:30");

        setContentView(R.layout.activity_autoseat);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("自动入座");


        LSettingItem mSettingItemOne = (LSettingItem) findViewById(R.id.auto_seat_switch);
        if (autoSeat) {
            mSettingItemOne.clickOn();
        } else {
            View view = findViewById(R.id.auto_seat_content);
            view.setVisibility(View.GONE);
        }
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("autoSeat", isChecked).commit();
                View view = findViewById(R.id.auto_seat_content);

                Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
                if (isChecked) {
                    view.setVisibility(View.VISIBLE);
                    startService(intentOne);
                } else {
                    view.setVisibility(View.GONE);
                    stopService(intentOne);
                }

            }
        });
        String s = "";

        //入座时间
        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_seat_time);
        s = auto_seat_time;
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                final EditText inputServer = new EditText(AutoSeatActivity.this);
                auto_seat_time = settingShare.getString("auto_seat_time", "06:30");
                inputServer.setText(auto_seat_time);
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AutoSeatActivity.this);
                builder.setTitle("自动入座时间（格式：HH:mm)").setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String text = inputServer.getText().toString();
                        if (!isTime(text)) {
                            Toast.makeText(getApplicationContext(), "时间输入不合法。", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        settingShare.edit().putString("auto_seat_time", text).commit();
                        auto_seat_time = text;
                        Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
                        stopService(intentOne);
                        startService(intentOne);
//                        Toast.makeText(getApplicationContext(),"时间修改成功。",Toast.LENGTH_SHORT).show();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.auto_seat_time);
                        lSettingItem.setRightText(inputServer.getText().toString());
                    }
                });
                builder.show();
            }
        });
        //入座日志
        lSettingItem = (LSettingItem) findViewById(R.id.auto_seat_logs);
        final String auto_seat_logs = settingShare.getString("auto_seat_logs","");
        final String auto_seat_logsTime = settingShare.getString("auto_seat_logsTime","无");
        lSettingItem.setRightText(auto_seat_logsTime);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AutoSeatActivity.this);
                builder.setIcon(R.mipmap.ic_launcher_round);
                builder.setTitle("入座日志 : "+auto_seat_logsTime);
                builder.setMessage(auto_seat_logs);
                builder.setCancelable(true);
                builder.setPositiveButton("我知道啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        });

        //立即执行
        Button addbtn = findViewById(R.id.auto_seat_go);
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
                stopService(intentOne);
                startService(intentOne);
                startService(intentOne);
            }

        });
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
    //判断时间是否合法：HH:mm
    public static boolean isTime(String time){
        Pattern p = Pattern.compile("((((0[0-9])|([1][0-9])|([2][0-4]))\\:([0-5][0-9])))$");
        return p.matcher(time).matches();
    }

}
