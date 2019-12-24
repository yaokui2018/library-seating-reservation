package ltd.yaokui.seat;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportActivity extends BaseActivity {

    int REQUESTCODE_FROM_ACTIVITY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Button btn= (Button) findViewById(R.id.findFile);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LFilePicker()
                        .withActivity(ImportActivity.this)
                        .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                        .withStartPath("/mnt/sdcard/seat/out")//指定初始显示路径
                        .withFileFilter(new String[]{".txt"})
                        .withNotFoundBooks("请选择需要导入的文本文件")
                        .withMutilyMode(false)
                        .start();

            }
        });
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUESTCODE_FROM_ACTIVITY) {
                //如果是文件选择模式，需要获取选择的所有文件的路径集合
                //List<String> list = data.getStringArrayListExtra(Constant.RESULT_INFO);//Constant.RESULT_INFO == "paths"
                List<String> list = data.getStringArrayListExtra("paths");
                Toast.makeText(getApplicationContext(), "正在读取文件，请稍后...", Toast.LENGTH_SHORT).show();
                List<Db> dbList = Txt(list.get(0));
                int ok = insert(dbList);
                Toast.makeText(getApplicationContext(), "成功导入"+ok+"条账号信息。", Toast.LENGTH_SHORT).show();
                MainActivity.demoAdapter.refresh();
            }
        }
    }

    public int insert(List<Db> list){//插入
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db");
        //取得一个只读的数据库对象
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        int ok=0;
        for (Db db : list) {
            try {
                //创建游标对象
                Cursor cursor = db1.query("account", new String[]{"id", "number", "password", "remark"}, "number=?", new String[]{db.getNumber() + ""}, null, null, null, null);
                int flag = 0;
                //利用游标遍历所有数据对象
                while (cursor.moveToNext()) {
                    flag = 1;
                    break;
                }
                if (flag==1)
                    continue;
                ContentValues values = new ContentValues();
                //像ContentValues中存放数据
                values.put("number", db.getNumber());
                values.put("password", db.getPassword());
                values.put("remark", db.getRemark());
                db1.insert("account", null, values);
                ok++;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return ok;
    }

    public String convertCodeAndGetText(String str_filepath) {// 转码

        File file = new File(str_filepath);
        BufferedReader reader;
        String text = "";
        try {
            // FileReader f_reader = new FileReader(file);
            // BufferedReader reader = new BufferedReader(f_reader);
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fis);
            in.mark(4);
            byte[] first3bytes = new byte[3];
            in.read(first3bytes);//找到文档的前三个字节并自动判断文档类型。
            in.reset();
            if (first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB
                    && first3bytes[2] == (byte) 0xBF) {// utf-8

                reader = new BufferedReader(new InputStreamReader(in, "utf-8"));

            } else if (first3bytes[0] == (byte) 0xFF
                    && first3bytes[1] == (byte) 0xFE) {

                reader = new BufferedReader(
                        new InputStreamReader(in, "unicode"));
            } else if (first3bytes[0] == (byte) 0xFE
                    && first3bytes[1] == (byte) 0xFF) {

                reader = new BufferedReader(new InputStreamReader(in,
                        "utf-16be"));
            } else if (first3bytes[0] == (byte) 0xFF
                    && first3bytes[1] == (byte) 0xFF) {

                reader = new BufferedReader(new InputStreamReader(in,
                        "utf-16le"));
            } else {

                reader = new BufferedReader(new InputStreamReader(in, "GBK"));
            }
            String str = reader.readLine();

            while (str != null) {
                text = text + str + "/n";
                str = reader.readLine();

            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    /**
     * 根据行读取内容
     * @return
     */
    public List<Db> Txt(String path) {
        //将读出来的一行行数据使用List存储
        String filePath = path;
        BufferedReader reader;
        List<Db> newList=new ArrayList<Db>();
        try {
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {//文件存在
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream in = new BufferedInputStream(fis);
                in.mark(4);
                byte[] first3bytes = new byte[100];
                in.read(first3bytes);//找到文档的前三个字节并自动判断文档类型。
                in.reset();
                String DEFAULT_ENCODING = "UTF-8";
                org.mozilla.universalchardet.UniversalDetector detector =
                        new org.mozilla.universalchardet.UniversalDetector(null);
                detector.handleData(first3bytes, 0, first3bytes.length);
                detector.dataEnd();
                String encoding = detector.getDetectedCharset();
                detector.reset();
                if (encoding == null) {
                    encoding = DEFAULT_ENCODING;
                }
                reader = new BufferedReader(new InputStreamReader(in, encoding));

                String lineTxt = null;
                while ((lineTxt = reader.readLine()) != null) {
                    Db db = new Db();
                    if (!"".equals(lineTxt)) {
                        db.setNumber(Integer.parseInt(lineTxt.split(",")[0].trim()));
                        db.setPassword(lineTxt.split(",")[1].trim());
                        String mark = lineTxt.split(",")[2].trim();
                        if (mark.equals(""))
                            mark=lineTxt.split(",")[0].trim();
                        db.setRemark(mark);
                        newList.add(db);
                    }
                }
                in.close();
                reader.close();
            }else {
                Toast.makeText(getApplicationContext(),"文件不存在。",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"文件读取失败，请检查格式。",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return newList;
    }

}
