package ltd.yaokui.seat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper{
    private static final int VERSION = 6;
    private static final String SWORD="SWORD";
    //三个不同参数的构造函数
    //带全部参数的构造函数，此构造函数必不可少
    public DatabaseHelper(Context context, String name, CursorFactory factory,
                          int version) {
//        super(context, name, factory, version);
        super(context, name, factory, VERSION);

    }
    //带两个参数的构造函数，调用的其实是带三个参数的构造函数
    public DatabaseHelper(Context context,String name){
        this(context,name,VERSION);
    }
    //带三个参数的构造函数，调用的是带所有参数的构造函数
    public DatabaseHelper(Context context,String name,int version){
        this(context, name,null,VERSION);
    }
    //创建数据库
    public void onCreate(SQLiteDatabase db) {
        Log.i(SWORD,"create a Database");
        //创建数据库sql语句
        String sql = "create table account(id Integer primary key autoincrement,number int,password varchar(32),remark varchar(20))";
        //执行创建数据库操作
        db.execSQL(sql);
        sql = "create table history(id Integer primary key autoincrement,room varchar(32),seat varchar(32),day varchar(32),time varchar(32),number varchar(32))";
        db.execSQL(sql);
        onUpgrade(db,1,VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion){
            case 1://版本2 预约记录增加预约状态字段，账号表添加时间、状态
                //0.待入座 1.已取消 2.已入座
                String sql = "ALTER TABLE history ADD status int";
                db.execSQL(sql);
                sql = "ALTER TABLE account ADD daytime varchar(50)";
                db.execSQL(sql);
                //0.未预约 1.已预约
                sql = "ALTER TABLE account ADD status int";
                db.execSQL(sql);
            case 2://版本3 添加自动预约座位数据数据库
                //创建数据库sql语句
                String sql2 = "create table auto_book(id Integer primary key autoincrement,room varchar(10),seat varchar(10),daytime varchar(50))";
                //执行创建数据库操作
                db.execSQL(sql2);
            case 3://版本4 账号表增加token字段
                sql = "ALTER TABLE account ADD token varchar(50)";
                db.execSQL(sql);
            case 4://版本5 增加sign表
                sql =  "create table sign(id Integer primary key autoincrement,sign varchar(50),date datetime,id_ Integer unique)";
                db.execSQL(sql);
            case 5://版本6 增加sign2表
                sql =  "create table sign2(id Integer primary key autoincrement,sign2 varchar(60))";
                db.execSQL(sql);
        }
        //创建成功，日志输出提示
        Log.i(SWORD,"update a Database");
    }

}
