package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.a520wcf.yllistview.YLListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private YLListView listView;
    List<History> dbList;
    DemoAdapter demoAdapter;
    private int dbVersion = 1;
    SharedPreferences settingShare;


    View bottomView;

    class DemoAdapter extends BaseAdapter {
        int pos;

        public void refresh() {
            dbList.clear();
            dbList.addAll(query());
            notifyDataSetChanged();;
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
            if (settingShare.getBoolean("wipeHistory",true)&&dbList.size()<3){
                bottomView.setVisibility(View.GONE);
            }
            if (dbList.size()==0){
                View view = View.inflate(getApplicationContext(),R.layout.no_data,null);
                return view;
            }
            if (position==0){

            }
            View view = View.inflate(getApplicationContext(),R.layout.list_view_history,null);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
                    builder.setIcon(android.R.drawable.ic_delete);
                    builder.setTitle("提示");
                    builder.setMessage("确定要删除该记录吗？");
                    builder.setCancelable(true);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //删除
                            if(delete(dbList.get(position).getId())){
                                Toast.makeText(getApplicationContext(),"删除成功。",Toast.LENGTH_SHORT).show();
                                refresh();
                            }
                            else
                                Toast.makeText(getApplicationContext(),"删除失败。",Toast.LENGTH_SHORT).show();

                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {}

                    });
                    builder.create().show();
                    return true;
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(),"长按可删除该记录。",Toast.LENGTH_SHORT).show();
                }
            });
            TextView textView = view.findViewById(R.id.his_id);
            textView.setText(dbList.get(position).getId()+"");
            textView = view.findViewById(R.id.his_day);
            textView.setText(dbList.get(position).getDay());
            String d1 = dbList.get(position).getDay();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date date= new Date();
            try {
                date = df.parse(d1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            int day = differentDays(new Date(),date);
            if (day==0) {
                textView.setText(d1.substring(5)+" (今天)");
                textView.setTextColor(getApplicationContext().getResources().getColor(R.color.textColor_alert_button_cancel));
            }
            else if (day==1)
                textView.setText(d1.substring(5)+" (明天)");

            else if (day==-1)
                textView.setText(d1.substring(5)+" (昨天)");

            textView = view.findViewById(R.id.his_num);
            textView.setText(dbList.get(position).getNumber());
            textView = view.findViewById(R.id.remark);
            String s1 = getRemark(dbList.get(position).getNumber());
            s1 = s1.split("_")[0];
            s1 = s1.length()>3?s1.substring(0,3):s1;
            textView.setText(s1);
//            textView.setVisibility(View.GONE);
            textView = view.findViewById(R.id.his_seat);
            textView.setText(dbList.get(position).getRoom()+" → "+dbList.get(position).getSeat());
            textView = view.findViewById(R.id.his_time);
            textView.setText(dbList.get(position).getTime());
            textView = view.findViewById(R.id.his_status);
            String s = "";
            switch (dbList.get(position).getStatus()){
                case 0:
                    s = "待入座";
                    textView.setTextColor(getApplicationContext().getResources().getColor(R.color.unseat));
                    String d = dbList.get(position).getDay()+" "+dbList.get(position).getTime();
                    df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    try {
                        date = df.parse(d);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (date.before(new Date())){
                        s = "已逾期";
                        textView.setTextColor(getApplicationContext().getResources().getColor(R.color.canceled));
                    }
                    break;
                case 1:
                    s = "已取消";
                    textView.setTextColor(getApplicationContext().getResources().getColor(R.color.canceled));
                    break;
                case 2:
                    s = "已入座";
                    textView.setTextColor(getApplicationContext().getResources().getColor(R.color.seated));
                    break;
            }
            textView.setText(s);

            return view;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);

        listView = (YLListView) findViewById(R.id.listView);
        // 不添加也有默认的头和底
        View topView=View.inflate(this,R.layout.top,null);
        TextView textView = topView.findViewById(R.id.bhnw);
        textView.setText("Reservation record");
        listView.addHeaderView(topView);
        boolean wipeHistory = settingShare.getBoolean("wipeHistory",true);
        if (wipeHistory) {
            bottomView = View.inflate(this, R.layout.bottom, null);
            bottomView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "长按可清空记录。", Toast.LENGTH_SHORT).show();
                }
            });
            bottomView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
                    builder.setIcon(android.R.drawable.ic_delete);
                    builder.setTitle("清空记录");
                    builder.setMessage("确定要清空所有记录吗？");
                    builder.setCancelable(true);
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //删除
                            if (deleteAll()) {
                                Toast.makeText(getApplicationContext(), "清空成功。", Toast.LENGTH_SHORT).show();
                                demoAdapter.refresh();
                            } else
                                Toast.makeText(getApplicationContext(), "清空失败。", Toast.LENGTH_SHORT).show();

                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }

                    });
                    builder.create().show();
                    return true;
                }
            });
        }
        else
            bottomView = new View(getApplicationContext());
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
    }

    public List<History> query(){//查询
        boolean hideCancel = settingShare.getBoolean("hideCancel",false);
        boolean todayHistory = settingShare.getBoolean("todayHistory",true);
        int sortHistory = settingShare.getInt("sortHistory",2);
        String s = "id";
        switch (sortHistory){
            case 1:
                s="number";
                break;
            case 2:
                s="day,time COLLATE LOCALIZED";
                break;
            case 3:
                s="status";
                break;
            case 4:
                s="id desc";
                break;
            case 5:
                s="number desc";
                break;
            case 6:
                s="day desc,time COLLATE LOCALIZED desc";
                break;
            case 7:
                s="status desc";
                break;
        }
        int status = 100;
        if (hideCancel)
            status = 1;
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("history", new String[]{"id","room","seat","day","time","number","status"}, "status!=?", new String[]{status+""}, null, null, s, null);
        //利用游标遍历所有数据对象
        List<History> list = new ArrayList<>();
        while(cursor.moveToNext()){
            if (todayHistory){
                String d1 = cursor.getString(cursor.getColumnIndex("day"));
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date date= new Date();
                try {
                    date = df.parse(d1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (differentDays(new Date(),date)<0){
                    continue;
                }
            }
            History db = new History();
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setNumber(cursor.getString(cursor.getColumnIndex("number")));
            db.setDay(cursor.getString(cursor.getColumnIndex("day")));
            db.setRoom(cursor.getString(cursor.getColumnIndex("room")));
            db.setSeat(cursor.getString(cursor.getColumnIndex("seat")));
            db.setTime(cursor.getString(cursor.getColumnIndex("time")));
            db.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            list.add(db);
            //日志打印输出
            Log.i("查询结果：","query-->"+","+db.getSeat());
        }
        return list;
    }
    public String getRemark(String number){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.query("account", new String[]{"remark"}, "number=?", new String[]{number}, null, null, null, null);
        //利用游标遍历所有数据对象
        if(cursor.moveToNext()){
            return cursor.getString(cursor.getColumnIndex("remark"));
        }
        return "";
    }
    public boolean deleteAll(){//删除所有
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("history",null,null)>0;
    }
    public boolean delete(int id){//删除
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("history","id=?",new String[]{id+""})>0;
    }


    /**
     * date2比date1多的天数
     * @param date1
     * @param date2
     * @return
     */
    public static int differentDays(Date date1,Date date2)
    {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2)   //同一年
        {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++)
            {
                if(i%4==0 && i%100!=0 || i%400==0)    //闰年
                {
                    timeDistance += 366;
                }
                else    //不是闰年
                {
                    timeDistance += 365;
                }
            }

            return timeDistance + (day2-day1) ;
        }
        else    //不同年
        {
            System.out.println("判断day2 - day1 : " + (day2-day1));
            return day2-day1;
        }
    }
}
