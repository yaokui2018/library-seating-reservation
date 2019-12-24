package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


import com.carlt.networklibs.NetType;
import com.carlt.networklibs.NetworkManager;
import com.carlt.networklibs.annotation.NetWork;
import com.carlt.networklibs.utils.Constants;
import com.carlt.networklibs.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ltd.yaokui.seat.utils.HttpUtils;

public class AutoSeatService extends Service{
    private volatile static AutoSeatService service;
    String data="";//服务启动携带参数

    public static String ACTION_ALARM = "action_AutoSeat";
    private Handler mHanler = new Handler(Looper.getMainLooper());
    static boolean flag_ = false;//标识，true为执行任务（第一次不执行-默认false）
    String channelId="SeatId1";//渠道id

    Context context;
    SharedPreferences settingShare;
    boolean autoSeat,autoBook;
    String auto_seat_time;
    String auto_seat_logs,auto_seat_logsTime;//日志
    NotificationManager manager;

    String toastText="";//提示文字
    String toastText2="";//提示文字
    String noticeSeat="";//座位
    String noticeSeat2="";//座位
    static int noticeId = 100;//通知id

    boolean networkFail  = false;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);
        //得到NotificationManager的对象，用来实现发送Notification
        manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        service = this;
        /**
         * 通知栏通知初始化·安卓8.0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //创建通知渠道
            CharSequence name = "Seat";
            String description = "渠道描述1";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;//重要性级别
            NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);
            mChannel.setDescription(description);//渠道描述
            mChannel.enableLights(true);//是否显示通知指示灯
            mChannel.enableVibration(true);//是否振动

            manager.createNotificationChannel(mChannel);//创建通知渠道
        }

        //注册网络监听
        NetworkManager.getInstance().registerObserver(this);

    }

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
        data=intent.getStringExtra("data");
        data = data==null?"":data;
        if (data.equals("exit")){
            onDestroy();
            return super.onStartCommand(intent,flags, startId);
        }
        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);
        //入座时间
        auto_seat_time = settingShare.getString("auto_seat_time","06:30");
        mHanler.post(new Runnable() {
            @Override
            public void run() {
            if (flag_){//执行预约操作
                autoseat();
            }
                flag_=true;
                //下一次任务
                Calendar instance = Calendar.getInstance();
                instance.set(Calendar.HOUR_OF_DAY, Integer.parseInt(auto_seat_time.split(":")[0]));//小时
                instance.set(Calendar.MINUTE, Integer.parseInt(auto_seat_time.split(":")[1]));//分钟
                instance.set(Calendar.SECOND, 0);//秒

                String isToday = "今天";
                if (instance.getTimeInMillis()-System.currentTimeMillis()<=0){//任务改成明天
                    isToday = "明天";
                    instance.set(Calendar.DAY_OF_MONTH, instance.get(Calendar.DAY_OF_MONTH)+1);//日期
//                    if (instance.get(Calendar.DAY_OF_MONTH)==1)
//                        instance.set(Calendar.MONTH, instance.get(Calendar.MONTH)+1);//月份
                }
                int year = instance.get(Calendar.YEAR);//获取年份
                int month=instance.get(Calendar.MONTH)+1;//获取月份
                int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
                int hour=instance.get(Calendar.HOUR_OF_DAY);//小时
                int minute=instance.get(Calendar.MINUTE);//分
                Toast.makeText(context, "入座服务启动。\n任务执行时间："+year+"-"+month+"-"+day+"("+isToday+") "+hour+":"+(minute>9?minute:"0"+minute), Toast.LENGTH_SHORT).show();
//                Toast.makeText(context, "服务启动。\n："+(instance.getTimeInMillis()-System.currentTimeMillis()), Toast.LENGTH_SHORT).show();
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(getApplicationContext(), AutoSeatService.class);
                intent.setAction(AutoSeatService.ACTION_ALARM);
                PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    if(Build.VERSION.SDK_INT < 19){
                        am.set(AlarmManager.RTC_WAKEUP, instance.getTimeInMillis(),pendingIntent);
                    }else{
                        am.setExact(AlarmManager.RTC_WAKEUP, instance.getTimeInMillis(), pendingIntent);
                    }
            }
        });
        network(NetworkUtils.getNetType());
        return super.onStartCommand(intent,flags, startId);
    }


    @Override
    public void onDestroy() {
        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);
        if (data.equals("exit")){
            autoSeat = autoBook = false;
        }
        getStartNotification();
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), AutoSeatService.class);
        intent.setAction(AutoSeatService.ACTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        try {
            am.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        flag_=false;
        super.onDestroy();
        service = null;
        data ="";

        //注销网络监听
        NetworkManager.getInstance().unRegisterObserver(this);
        //注销所有
        //NetworkManager.getInstance().unRegisterAllObserver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //服务启动通知
     public void getStartNotification() {
         networkFail = false;
        String title = "";
        if (autoBook) {
            title += "自动预约";
            if (autoSeat)
                title+=" | 自动入座";
        }
        else if (autoSeat)
            title+="自动入座";
        else {
//            manager.cancel(1);
            stopForeground(true);
            return;
        }

        //得到Notification对象
        Notification notification = new Notification();
        //设置消息来时显示的消息
        notification.tickerText = "服务已启动";
        //设置消息来时显示图标
        notification.icon = R.mipmap.ic_launcher;
        //设置是否会消失
         notification.flags |= Notification.FLAG_ONGOING_EVENT;
//        notification.flags =Notification.FLAG_AUTO_CANCEL;
        //设置消息来时震动
         notification.defaults =Notification.DEFAULT_VIBRATE;
        //设置当前时间
        long when = System.currentTimeMillis();
        notification.when = when;
        //通知的跳转事件
        Intent intent = new Intent(getApplication(), SettingActivity.class);
        /**Intent一般是用作Activity、Sercvice、BroadcastReceiver之间传递数据，
                 而Pendingintent，一般用在 Notification上，
                 可以理解为延迟执行的intent，PendingIntent是对Intent一个包装。*/
        //参数：1、上下文 2、请求码 3、用于启动的intent 4、新开启的Activity的启动模式
        @SuppressLint("WrongConstant")
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        //参数1、上下文 2、下拉通知栏 显示的标题 3、内容  4、PendingIntent对象
//        notification.setLatestEventInfo(this, "贾震", "外号叫大炮", pendingIntent);
        //取消通知
        //manager.cancelAll();
          if (Build.VERSION.SDK_INT <16) {
             Class clazz = notification.getClass();
             try {
                 Method m2 = clazz.getDeclaredMethod("setLatestEventInfo", Context.class,CharSequence.class,CharSequence.class,PendingIntent.class);
                    m2.invoke(notification, context, title,"服务已启动", pendingIntent);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
          else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//安卓8.0
              Notification.Builder mBuilder = new Notification.Builder(this);
              RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);

              mBuilder.setSmallIcon(R.mipmap.ic_launcher);
              mBuilder.setContent(remoteViews);
//            if (progress == 1) {
                mBuilder.setDefaults(Notification.DEFAULT_SOUND);
//            }
              Calendar calendar = Calendar.getInstance();
              String t = "上午";
              int m = calendar.get(Calendar.HOUR_OF_DAY);
              if (m>=12) {
                  t = "下午";
                  if (m!=12)
                      m-=12;
              }
              int n = calendar.get(Calendar.MINUTE);
              remoteViews.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
              mBuilder.setTicker("服务已启动");//手机状态栏通知
              remoteViews.setTextViewText(R.id.notificationTextView_title, title);
              remoteViews.setTextViewText(R.id.notificationTextView_content, "服务已启动");
              remoteViews.setTextViewText(R.id.notificationTextView_time, t+" "+(m>9?m:"0"+m)+":"+(n>9?n:"0"+n));
              mBuilder.setContentIntent(pendingIntent).setChannelId(channelId);

              Notification notification2 = mBuilder.getNotification();
              notification2.flags |= Notification.FLAG_ONGOING_EVENT;
              notification = mBuilder.build();
          }
          else {
              notification = new Notification.Builder(context)
                      .setTicker("服务已启动")//在状态栏显示的标题
                      .setContentTitle(title)
                      .setContentText("服务已启动")
                      .setContentIntent(pendingIntent)
                      .setAutoCancel(false)
                      .setOngoing(true)
                      .setSmallIcon(R.mipmap.ic_launcher)
                      .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置显示的大图标
                      .setWhen(System.currentTimeMillis())
                      .build();
          }
         //启动Notification
         manager.notify(1, notification);
         //前台服务
         startForeground(1, notification);

     }
    //网络异常通知
     public void getNetworkNotification() {
         networkFail = true;
        String title = "";
        if (autoBook) {
        }
        else if (autoSeat){}
        else {
            manager.cancel(1);
            stopForeground(true);
            return;
        }
        title = "网络异常";
        String content = "请保持校园网连接。";

        //得到Notification对象
        Notification notification = new Notification();
        //设置消息来时显示的消息
        notification.tickerText = "网络异常";
        //设置消息来时显示图标
        notification.icon = R.mipmap.ic_launcher;
        //设置是否会消失
         notification.flags |= Notification.FLAG_ONGOING_EVENT;
//        notification.flags =Notification.FLAG_AUTO_CANCEL;
        //设置消息来时震动
         notification.defaults =Notification.DEFAULT_VIBRATE;
        //设置当前时间
        long when = System.currentTimeMillis();
        notification.when = when;
        //通知的跳转事件
        Intent intent = new Intent(getApplication(), SettingActivity.class);
        /**Intent一般是用作Activity、Sercvice、BroadcastReceiver之间传递数据，
                 而Pendingintent，一般用在 Notification上，
                 可以理解为延迟执行的intent，PendingIntent是对Intent一个包装。*/
        //参数：1、上下文 2、请求码 3、用于启动的intent 4、新开启的Activity的启动模式
        @SuppressLint("WrongConstant")
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
          if (Build.VERSION.SDK_INT <16) {
             Class clazz = notification.getClass();
             try {
                 Method m2 = clazz.getDeclaredMethod("setLatestEventInfo", Context.class,CharSequence.class,CharSequence.class,PendingIntent.class);
                    m2.invoke(notification, context, title,content, pendingIntent);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
          else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//安卓8.0
              Notification.Builder mBuilder = new Notification.Builder(this);
              RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);

              mBuilder.setSmallIcon(R.mipmap.ic_launcher);
              mBuilder.setContent(remoteViews);
//            if (progress == 1) {
                mBuilder.setDefaults(Notification.DEFAULT_SOUND);
//            }
              Calendar calendar = Calendar.getInstance();
              String t = "上午";
              int m = calendar.get(Calendar.HOUR_OF_DAY);
              if (m>=12) {
                  t = "下午";
                  if (m!=12)
                      m-=12;
              }
              int n = calendar.get(Calendar.MINUTE);
              remoteViews.setImageViewResource(R.id.image, R.mipmap.ic_launcher);
              mBuilder.setTicker("网络异常");//手机状态栏通知
              remoteViews.setTextViewText(R.id.notificationTextView_title, title);
              remoteViews.setTextViewText(R.id.notificationTextView_content, content);
              remoteViews.setTextViewText(R.id.notificationTextView_time, t+" "+(m>9?m:"0"+m)+":"+(n>9?n:"0"+n));
              mBuilder.setContentIntent(pendingIntent).setChannelId(channelId);

              Notification notification2 = mBuilder.getNotification();
              notification2.flags |= Notification.FLAG_ONGOING_EVENT;
              notification = mBuilder.build();
          }
          else {
              notification = new Notification.Builder(context)
                      .setTicker("网络异常")//在状态栏显示的标题
                      .setContentTitle(title)
                      .setContentText(content)
                      .setContentIntent(pendingIntent)
                      .setAutoCancel(false)
                      .setOngoing(true)
                      .setSmallIcon(R.mipmap.ic_launcher)
                      .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置显示的大图标
                      .setWhen(System.currentTimeMillis())
                      .build();
          }
         //启动Notification
         manager.notify(1, notification);
         //前台服务
         startForeground(1, notification);
    }
    //发送通知
     public void sendNotification(String title,String content) {
        //得到Notification对象
        Notification notification = new Notification();
        //设置消息来时显示的消息
        notification.tickerText = "服务通知";
        //设置消息来时显示图标
        notification.icon = R.mipmap.ic_launcher;
        //设置是否会消失
        notification.flags =Notification.FLAG_AUTO_CANCEL;
        //设置消息来时震动
         notification.defaults =Notification.DEFAULT_VIBRATE;
        //设置当前时间
        long when = System.currentTimeMillis();
        notification.when = when;
        //通知的跳转事件
        Intent intent = new Intent(getApplication(), SettingActivity.class);
        /**Intent一般是用作Activity、Sercvice、BroadcastReceiver之间传递数据，
                 而Pendingintent，一般用在 Notification上，
                 可以理解为延迟执行的intent，PendingIntent是对Intent一个包装。*/
        //参数：1、上下文 2、请求码 3、用于启动的intent 4、新开启的Activity的启动模式
        @SuppressLint("WrongConstant")
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        //参数1、上下文 2、下拉通知栏 显示的标题 3、内容  4、PendingIntent对象
//        notification.setLatestEventInfo(this, "贾震", "外号叫大炮", pendingIntent);
        //取消通知
        //manager.cancelAll();
          if (Build.VERSION.SDK_INT <16) {
             Class clazz = notification.getClass();
             try {
                 Method m2 = clazz.getDeclaredMethod("setLatestEventInfo", Context.class,CharSequence.class,CharSequence.class,PendingIntent.class);
                    m2.invoke(notification, context, title,content, pendingIntent);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//安卓8.0
              NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
              style.bigText(content);
              style.setBigContentTitle(title);
              //SummaryText没什么用 可以不设置
//            style.setSummaryText("末尾只一行的文字内容");
              notification = new NotificationCompat.Builder(context)
                      .setTicker("服务通知")//在状态栏显示的标题
                      .setContentTitle(title)
                      .setContentText(content)
                      .setAutoCancel(true)
                      .setOngoing(false)
                      .setSmallIcon(R.mipmap.ic_launcher)
                      .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置显示的大图标
                      .setWhen(System.currentTimeMillis())
                      .setStyle(style)
                      .setChannelId(channelId)
                      .build();
        }
        else
        {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.bigText(content);
            style.setBigContentTitle(title);
            //SummaryText没什么用 可以不设置
//            style.setSummaryText("末尾只一行的文字内容");
            notification = new NotificationCompat.Builder(context)
            .setTicker("服务通知")//在状态栏显示的标题
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setOngoing(false)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置显示的大图标
            .setWhen(System.currentTimeMillis())
            .setStyle(style)
            .build();
        }
         //启动Notification
         manager.notify(noticeId++, notification);
    }
    //获取当前时间
    public String getTime(){
        Time time  =  new Time();
        time.setToNow();
        String str_time2 = time.format("%Y-%m-%d %H:%M:%S");
        return str_time2;
    }

    //自动入座
    public void autoseat(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //日志
                String t = settingShare.getString("auto_seat_logsTime","");
                String time = getTime();
                if (t.length()>10&&time.startsWith(t.substring(0,10)))
                    auto_seat_logs = "\n"+settingShare.getString("auto_seat_logs","")+t+"\n----------\n\n";
                else
                    auto_seat_logs = "";
                settingShare.edit().putString("auto_seat_logsTime",time).commit();


                Sign sign_ = querySign();
                settingShare.edit().putString("seatSign", sign_.getSign()).commit();
                settingShare.edit().putString("seatDate", sign_.getDate()).commit();

                Message msg=new Message();
                msg.what=-2;
                List<Db> dbList = query2();
                msg.what=-3;
                toastText2 = "自动入座：执行中...";
                handler.sendMessage(msg);
                for (int i = 0;i<dbList.size();i++) {
                    noticeSeat = "账号："+dbList.get(i).getNumber()+"("+dbList.get(i).getRemark()+")";
                    //参数
                    String str = "{\"intf_code\":\"QRY_RECORD\",\"params\":{\"userPhysicalCard\":"+dbList.get(i).getNumber()+",\"flag\":\"2\",\"status\":1}}";
                    //服务器请求路径
                    String strUrlPath = "http://211.70.171.14:9999/tsgintf/main/service";
                    String sign = settingShare.getString("seatSign","99be17c86d7169e81f7ec6416398dadb.1631546119553");
                    HttpUtils.setSign(sign);
                    HttpUtils.setSign2(querySign2().getSign2());
                    HttpUtils.setAuthorization(dbList.get(i).getToken());
                    String strResult = HttpUtils.submitPostData(strUrlPath, str, "utf-8");

                    if (strResult.equals("error")){
                        msg=new Message();
                        msg.what=-1;
                        Bundle bundle = new Bundle();
                        bundle.putString("title",toastText);  //往Bundle中存放数据
                        bundle.putString("content",noticeSeat);  //往Bundle中put数据
                        msg.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(msg);
                        continue;
                    }

                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(strResult);
                        String result_code = jsonObject.getString("result_code");
//                        toastText = jsonObject.getString("result_desc");
                        if (result_code.equals("0")){//成功
                            JSONArray data = jsonObject.getJSONObject("result_data").getJSONArray("rows");
                            int a = data.length();
                            if ( data.length() > 0){
                                JSONObject d = data.getJSONObject(0);
                                int seatId = d.getInt("seatId");
                                int id = d.getInt("id");
                                String appversion = settingShare.getString("seatVersion","2.4.0");
                                //入座
                                str = "{\"intf_code\":\"UPD_SCAN_SEAT\",\"params\":{\"deviceType\":\"mix\",\"operType\":\"click\",\"reback\":\"\",\"seatId\":"+seatId+",\"bssid\":\"70:ba:ef:af:eb:22,70:ba:ef:af:b3:42,70:ba:ef:af:ea:62,70:ba:ef:af:e5:62,70:ba:ef:af:b3:52,70:ba:ef:af:1f:42,70:ba:ef:af:b0:02,70:ba:ef:af:ea:42,70:ba:ef:af:e5:72,70:ba:ef:af:ae:e2,70:ba:ef:af:e8:52,70:ba:ef:af:ae:f2,70:ba:ef:af:b0:12,70:ba:ef:af:ea:72,70:ba:ef:af:af:72,70:ba:ef:af:b3:82,70:ba:ef:af:d4:82,70:ba:ef:af:eb:32,70:ba:ef:af:af:62,70:ba:ef:af:e8:42,70:ba:ef:af:b3:92,70:ba:ef:af:1f:52,70:ba:ef:af:b2:92,70:ba:ef:af:ea:52,70:ba:ef:af:ea:f2,70:ba:ef:af:b3:43,70:ba:ef:af:b3:40,70:ba:ef:af:b3:41,70:ba:ef:af:eb:23,70:ba:ef:af:eb:20,70:ba:ef:af:eb:21,70:ba:ef:af:ea:41,70:ba:ef:af:ea:43,70:ba:ef:af:eb:31,70:ba:ef:af:eb:33,70:ba:ef:af:eb:30,70:ba:ef:af:b3:51,70:ba:ef:af:ae:e0,70:ba:ef:af:ae:e1,70:ba:ef:af:ae:e3,70:ba:ef:af:e8:53,70:ba:ef:af:af:73,70:ba:ef:af:b3:53,70:ba:ef:af:e8:51,70:ba:ef:af:b3:50,70:ba:ef:af:ea:70,70:ba:ef:af:ea:73,70:ba:ef:af:b0:13,70:ba:ef:af:b0:10,70:ba:ef:af:af:70,70:ba:ef:af:e8:50,70:ba:ef:af:b0:11,70:ba:ef:af:ea:40,70:ba:ef:af:e5:63,70:ba:ef:af:e5:60,70:ba:ef:af:af:71,70:ba:ef:af:ae:f3,70:ba:ef:af:ea:71,70:ba:ef:af:b0:00,70:ba:ef:af:b0:01,70:ba:ef:af:e5:70,70:ba:ef:af:1f:41,70:ba:ef:af:1f:43,70:ba:ef:af:1f:40,70:ba:ef:af:ea:61,70:ba:ef:af:ea:63,70:ba:ef:af:ea:60,70:ba:ef:af:e5:73,70:ba:ef:af:ea:f0,70:ba:ef:af:ae:f0,70:ba:ef:af:b0:03,70:ba:ef:af:e5:61,70:ba:ef:af:ae:f1,70:ba:ef:af:e8:40,70:ba:ef:af:ea:f1,70:ba:ef:af:b3:80,70:ba:ef:af:b3:83,70:ba:ef:af:b3:81,70:ba:ef:af:e5:71,70:ba:ef:af:e8:43,70:ba:ef:af:e8:41\",\"phoneSystem\":\"android\",\"mixBssid\":\"\",\"appVersion\":1,\"version\":\""+appversion+"\",\"userPhysicalCard\":\""+dbList.get(i).getNumber()+"\",\"reservedId\":\""+id+"\"}}";

                                strResult = HttpUtils.submitPostData(strUrlPath, str, "utf-8");

                                if (strResult.equals("error")){
                                    msg=new Message();
                                    msg.what=-1;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("title",toastText);  //往Bundle中存放数据
                                    bundle.putString("content",noticeSeat);  //往Bundle中put数据
                                    msg.setData(bundle);//mes利用Bundle传递数据
                                    handler.sendMessage(msg);
                                    continue;
                                }
                                jsonObject = new JSONObject(strResult);
                                result_code = jsonObject.getString("result_code");
                                toastText = jsonObject.getString("result_desc");
                                if (result_code.equals("0")) {//成功
                                    toastText = "入座成功";
                                    CancelOrSeat(dbList.get(i).getNumber()+"");
                                }
                                    msg=new Message();
                                    msg.what=-2;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("title",toastText);  //往Bundle中存放数据
                                    bundle.putString("content",noticeSeat);  //往Bundle中put数据
                                    msg.setData(bundle);//mes利用Bundle传递数据
                                    handler.sendMessage(msg);
//                                }
                                continue;

                            }
                            else {
                                continue;
                            }
                        }
                        else
                            continue;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        msg=new Message();
                        msg.what=-2;
                        toastText = "出错了";
                        Bundle bundle = new Bundle();
                        bundle.putString("title",toastText);  //往Bundle中存放数据
                        bundle.putString("content",noticeSeat);  //往Bundle中put数据
                        handler.sendMessage(msg);
                    }
                }
                settingShare.edit().putString("auto_seat_logs",auto_seat_logs).commit();
                msg=new Message();
                msg.what=-3;
                toastText2 = "自动入座：结束。";
                handler.sendMessage(msg);
            }
        }).start();
    }


    public List<Db> query2(){//查询
        String s ="status desc,daytime COLLATE LOCALIZED";
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",3);
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


    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case 1:
                    Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
                    break;
                case 2://更新

                    break;
                case -1://网络连接失败
                    String content = msg.getData().getString("content");//接受msg传递过来的参数
                    Toast.makeText(context, "网络连接超时", Toast.LENGTH_SHORT).show();

                    auto_seat_logs += "网络连接超时,任务取消"+"\n"+content+"\n\n";

                    sendNotification("网络连接超时,任务取消",content);
                    break;
                case -2://发送通知
                    String str1 = msg.getData().getString("title");//接受msg传递过来的参数
                    String str2 = msg.getData().getString("content");//接受msg传递过来的参数
                    auto_seat_logs += str1+"\n"+str2+"\n\n";
                    sendNotification(str1,str2);
                    break;
                case -3:
                    Toast.makeText(context, toastText2, Toast.LENGTH_SHORT).show();
                    break;
                case -99:

                    break;
            }
        }
    };


    /**
     * 服务是否存活
     *
     * @return true: 存活
     */
    public static boolean isServiceRunning() {
        return service != null;
    }

//    public void test(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String strUrlPath = "http://211.70.171.14:9999/tsgintf/main/service";
//                String str = "{\"intf_code\":\"QRY_RECORD\",\"params\":{\"userPhysicalCard\":,\"flag\":\"2\",\"status\":1}}";
//                String strResult = HttpUtils.submitPostData(strUrlPath, str, "utf-8");
//                if (strResult.equals("error")) {
//                    getNetworkNotification();
//                } else
//                    getStartNotification();
//            }
//        });
//    }

    /**
     * 网络状态监听
     * @param netType
     */
    @NetWork(netType = NetType.AUTO)
    public void network(NetType netType) {
        switch (netType) {
            case WIFI:
                autoSeat = settingShare.getBoolean("autoSeat",false);
                autoBook = settingShare.getBoolean("autoBook",false);
                getStartNotification();
//                Log.e(Constants.LOG_TAG, "wifi");
                break;
//            case CMNET:
//            case CMWAP:
//                Log.e(Constants.LOG_TAG, "4G");
//                break;
//            case AUTO:
//                break;
//            case NONE:
//                Log.e(Constants.LOG_TAG, "无网络");
//                break;
            default:
                if (!networkFail)
                    Toast.makeText(context, "网络异常：请保持校园网连接", Toast.LENGTH_SHORT).show();
                getNetworkNotification();
                break;
        }
    }

    public void CancelOrSeat(String number){//flag : 1.取消 2.入座
        int flag = 2;
        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",3);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("status",2);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",5);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        return db1.delete("sign2", "id=?", new String[]{id+""})>0;
    }
    //sign2查询
    public Sign2 querySign2(){
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",5);
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
        DatabaseHelper dbHelper1 = new DatabaseHelper(this, "seat.db",5);
        SQLiteDatabase db1 = dbHelper1.getReadableDatabase();
        //创建游标对象
        Cursor cursor = db1.rawQuery("select count(id) from sign2 ",null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }


}