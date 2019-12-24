package ltd.yaokui.seat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.a520wcf.yllistview.YLListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SignActivity extends AppCompatActivity {

    private YLListView listView;
    List<Sign> dbList;
    DemoAdapter demoAdapter;
    private int dbVersion = 1;
    SharedPreferences settingShare;


    View bottomView;

    class DemoAdapter extends BaseAdapter {
        int pos;

        public void refresh() {
            dbList.clear();
            dbList.addAll(query());
            setTitle("Sign List : "+dbList.size());
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
            if (position==0){

            }
            View view = View.inflate(getApplicationContext(),R.layout.list_view_sign,null);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
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
            TextView textView = view.findViewById(R.id.sign);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
                    builder.setMessage("ID: "+dbList.get(position).getId()+"\n\nSign: "+dbList.get(position).getSign()+"\n\n时间: "+dbList.get(position).getDate());
                    builder.setCancelable(true);
                    builder.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                        }
                    });
                    builder.show();
                }
            });
            textView = view.findViewById(R.id.date);
            textView.setText(dbList.get(position).getDate());
            return view;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);

        settingShare.edit().putInt("signId", 0).commit();

        boolean autoDeleteSign = settingShare.getBoolean("autoDeleteSign", true);

        int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
        if(autoDeleteSign)
            Toast.makeText(getApplicationContext(),"已自动删除"+reGetSignTimeAdd+"秒以前数据。",Toast.LENGTH_SHORT).show();

        dbList = query();

        listView = (YLListView) findViewById(R.id.listView);
        // 不添加也有默认的头和底
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
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

        if (dbList.size()<3){
            bottomView.setVisibility(View.GONE);
        }

        listView.addFooterView(bottomView);

        // 顶部和底部也可以固定最终的高度 不固定就使用布局本身的高度
        listView.setFinalBottomHeight(200);
        listView.setFinalTopHeight(100);
        demoAdapter = new DemoAdapter();
        listView.setAdapter(demoAdapter);


        View topView=View.inflate(this,R.layout.top,null);
        TextView textView = topView.findViewById(R.id.bhnw);
        textView.setText("Sign List : "+dbList.size());
        listView.addHeaderView(topView);

        //YLListView默认有头和底  处理点击事件位置注意减去
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                position=position-listView.getHeaderViewsCount();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 添加菜单
        menu.add(0,0,0,"清空记录");
        boolean autoDeleteSign = settingShare.getBoolean("autoDeleteSign", true);
        String s ="开";
        if (!autoDeleteSign)
            s = "关";

        int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
        menu.add(0,1,1,"自动删除"+reGetSignTimeAdd+"秒前记录："+s);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        // 响应每个菜单项(通过菜单项的ID)
        switch (item.getItemId()) {
            case 0:
                AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
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
                break;
            case 1:
                boolean autoDeleteSign = settingShare.getBoolean("autoDeleteSign", true);
                if (autoDeleteSign) {
                    Toast.makeText(getApplicationContext(), "功能关闭成功，下一次访问本界面生效。", Toast.LENGTH_SHORT).show();
                    settingShare.edit().putBoolean("autoDeleteSign", false).commit();
                }
                else {
                    Toast.makeText(getApplicationContext(), "功能开启成功，下一次访问本界面生效。", Toast.LENGTH_SHORT).show();
                    settingShare.edit().putBoolean("autoDeleteSign", true).commit();
                }
                break;
        }
        return true;
    }

    public List<Sign> query(){//查询
        int reGetSignTimeAdd = settingShare.getInt("reGetSignTimeAdd",6000);
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        boolean autoDeleteSign = settingShare.getBoolean("autoDeleteSign", true);
        if (autoDeleteSign)
            db1.delete("sign","date < datetime('now','-"+reGetSignTimeAdd+" seconds','localtime')",null);
        //创建游标对象
        Cursor cursor = db1.query("sign", new String[]{"id","sign","date","id_"}, null, null, null, null, "date", null);
        //利用游标遍历所有数据对象
        List<Sign> list = new ArrayList<>();
        while(cursor.moveToNext()){
            Sign db = new Sign();
            db.setId(cursor.getInt(cursor.getColumnIndex("id")));
            db.setSign(cursor.getString(cursor.getColumnIndex("sign")));
            db.setDate(cursor.getString(cursor.getColumnIndex("date")));
            db.setId_(cursor.getInt(cursor.getColumnIndex("id_")));
            list.add(db);
        }
        return list;
    }
    public boolean deleteAll(){//删除所有
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign",null,null)>0;
    }
    public boolean delete(int id){//删除
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",dbVersion);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign","id=?",new String[]{id+""})>0;
    }

}
