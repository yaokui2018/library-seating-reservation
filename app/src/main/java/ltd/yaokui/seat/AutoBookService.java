package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.carlt.networklibs.NetType;
import com.carlt.networklibs.NetworkManager;
import com.carlt.networklibs.annotation.NetWork;
import com.carlt.networklibs.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ltd.yaokui.seat.utils.HttpUtils;

public class AutoBookService extends Service{
    private volatile static AutoBookService service;
    String data="";//服务启动携带参数
    private Handler mHandler=null;

    //座位任务index
    int seat_i = 0;
    String seat_room="",seat_seat="",seat_roomId="";
    List<Db> seat_dbList;
    List<AutoBook> autoBookList;
    //sign值获取时间
    private long getSignTime = 0;
    String sign ="1";//= settingShare.getString("seatSign","99be17c86d7169e81f7ec6416398dadb.1631546119553");

    public static String ACTION_ALARM = "action_AutoBook";
    private Handler mHanler = new Handler(Looper.getMainLooper());
    static boolean flag_ = false;//标识，true为执行任务（第一次不执行-默认false）
    String channelId="SeatId1";//渠道id

    Context context;
    SharedPreferences settingShare;
    boolean autoSeat,autoBook,auto_book_efficient;
    int auto_Try_Time;
    String auto_book_time;
    String auto_book_logs,auto_book_logsTime;//日志
    NotificationManager manager;

    Boolean networkFail = false;//是否网络异常

    String toastText="";//提示文字
    String toastText2="";//提示文字
    String noticeSeat="";//座位
    String noticeSeat2="";//座位
    static int noticeId = 100;//通知id

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

        //预约时间
        auto_book_time = settingShare.getString("auto_book_time","12:00:00");
        mHanler.post(new Runnable() {
            @Override
            public void run() {
            if (flag_){//执行预约操作
                //高效模式
                auto_book_efficient = settingShare.getBoolean("auto_book_efficient",false);
                if (auto_book_efficient)
                    autobook_efficient();
                else
                    autobook();
            }
                flag_=true;
                //下一次任务
                if (auto_book_time.split(":").length<3){
                    auto_book_time += ":00";
                }
                Calendar instance = Calendar.getInstance();
                instance.set(Calendar.HOUR_OF_DAY, Integer.parseInt(auto_book_time.split(":")[0]));//小时
                instance.set(Calendar.MINUTE, Integer.parseInt(auto_book_time.split(":")[1]));//分钟
                instance.set(Calendar.SECOND, Integer.parseInt(auto_book_time.split(":")[2]));//秒

                String isToday = "今天";
                if (instance.getTimeInMillis()-System.currentTimeMillis()<=0){
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
                int second=instance.get(Calendar.SECOND);//秒
                boolean auto_book_showToast = settingShare.getBoolean("auto_book_showToast",true);
                if (auto_book_showToast)
                    Toast.makeText(context, "预约服务启动。\n任务执行时间："+year+"-"+month+"-"+day+"("+isToday+") "+hour+":"+(minute>9?minute:"0"+minute)+":"+(second>9?second:"0"+second), Toast.LENGTH_SHORT).show();
//                Toast.makeText(context, "服务启动。\n："+(instance.getTimeInMillis()-System.currentTimeMillis()), Toast.LENGTH_SHORT).show();
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(getApplicationContext(), AutoBookService.class);
                intent.setAction(AutoBookService.ACTION_ALARM);
                PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    if(Build.VERSION.SDK_INT < 19){
                        am.set(AlarmManager.RTC_WAKEUP, instance.getTimeInMillis(),pendingIntent);
                    }else{
                        am.setExact(AlarmManager.RTC_WAKEUP, instance.getTimeInMillis(), pendingIntent);
                    }
            }
        });
        network(NetworkUtils.getNetType());
//        getStartNotification();
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
        Intent intent = new Intent(getApplicationContext(), AutoBookService.class);
        intent.setAction(AutoBookService.ACTION_ALARM);
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
//            manager.cancel(0);
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
         notification.flags |= Notification.FLAG_NO_CLEAR;
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
        else
        {
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
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//安卓8.0
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

    //自动预约
    public void autobook(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //日志
                String t = settingShare.getString("auto_book_logsTime","");
                //自动重试时间
                auto_Try_Time = settingShare.getInt("auto_Try_Time",0);
                String time = getTime();
                if (t.length()>10&&time.startsWith(t.substring(0,10)))
                    auto_book_logs = "\n"+settingShare.getString("auto_book_logs","")+t+"\n----------\n\n";
                else
                    auto_book_logs = "";
                settingShare.edit().putString("auto_book_logsTime",time).commit();


//                String sign_ = getSeatSign();
//                settingShare.edit().putString("seatSign", sign_.getSign()).commit();
//                settingShare.edit().putString("seatDate", sign_.getDate()).commit();

                Message msg=new Message();
                msg.what=-2;
                List<AutoBook> autoBookList = query();
                if (autoBookList.size()==0){
                    toastText = "未添加座位任务。";
                    Bundle bundle = new Bundle();
                    bundle.putString("title",toastText);  //往Bundle中存放数据
                    bundle.putString("content","自动预约结束");  //往Bundle中put数据
                    msg.setData(bundle);//mes利用Bundle传递数据

                    auto_book_logs += toastText+"\n自动预约结束\n\n";
                    settingShare.edit().putString("auto_book_logs",auto_book_logs).commit();

                    handler.sendMessage(msg);
                    return;
                }
                List<Db> dbList = query2();
                msg.what=-3;
                Bundle bundle1 = new Bundle();
                bundle1.putString("content","自动预约：执行中...");  //往Bundle中put数据
                msg.setData(bundle1);//mes利用Bundle传递数据
                handler.sendMessage(msg);
                Calendar instance = Calendar.getInstance();
                instance.set(Calendar.DAY_OF_MONTH, instance.get(Calendar.DAY_OF_MONTH)+1);//日期
//                if (instance.get(Calendar.DAY_OF_MONTH)==1)
//                    instance.set(Calendar.MONTH, instance.get(Calendar.MONTH)+1);//月份
                int year = instance.get(Calendar.YEAR);//获取年份
                int month=instance.get(Calendar.MONTH);//获取月份
                int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
                String dateStr = year+"-"+(month+1)+"-"+day;
                boolean auto_book_1minute = settingShare.getBoolean("auto_book_1minute",false);
                for (int i = 0,j=0;i<dbList.size()&&j<autoBookList.size();) {
                    String room = autoBookList.get(j).getRoom();
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
                    noticeSeat = "座位：["+room+"] "+autoBookList.get(j).getSeat()+" "+"\n账号："+dbList.get(i).getNumber()+"("+dbList.get(i).getRemark()+")";

                    //参数
                    String str = "{\"intf_code\":\"UPD_PRE_SEAT\",\"params\":{\"seatNo\":\""+autoBookList.get(j).getSeat()+"\",\"roomId\":\""+autoBookList.get(j).getRoom()+"\",\"dateStr\":\""+dateStr+"\",\"startHour\":\"06:30\",\"endHour\":\"23:00\",\"userPhysicalCard\":\""+dbList.get(i).getNumber()+"\"}}";

                    //服务器请求路径
                    String strUrlPath = "http://211.70.171.14:9999/tsgintf/main/service";
                    HttpUtils.setSign(getSeatSign());
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
                        j++;
                        continue;
                    }

                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(strResult);
                        String result_code = "-999";
                        try {
                            result_code = jsonObject.getString("result_code");
                        }catch (Exception e){}
                        toastText = jsonObject.getString("result_desc");
                        if (result_code.equals("20014")||result_code.equals("20001")){//该座位正在被其他人操作，请稍后再试|此座位不存在
                            j++;
                        }
                        else if (result_code.equals("20004")&&(toastText.equals("抱歉，今日您已被取消预约权，明天解除！")||toastText.endsWith("不能再次预占"))){//今日被取消预约权
                            i++;
                            continue;
                        }
                        else if (result_code.equals("20013")){//现有预约记录冲突
                            i++;
                            continue;
                        }
                        else if (result_code.equals("-999")){//token错误
                            i++;
                        }else if (auto_Try_Time!=0&&result_code.equals("20004")&&toastText.equals("亲，未到预约时间，不可以预约！")){
                            long Trytime = -1;
                            switch (auto_Try_Time){
                                case 1:
                                    Trytime = 100;
                                    break;
                                case 2:
                                    Trytime = 1000;
                                    break;
                                case 3:
                                    Trytime = 1000*60;
                                    break;
                            }
                            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            Intent intent = new Intent(getApplicationContext(), AutoBookService.class);
                            intent.setAction(AutoBookService.ACTION_ALARM);
                            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            if(Build.VERSION.SDK_INT < 19){
                                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Trytime,pendingIntent);
                            }else{
                                am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Trytime, pendingIntent);
                            }
                            msg=new Message();
                            msg.what=-2;
                            Bundle bundle = new Bundle();
                            bundle.putString("title","未到预约时间");  //往Bundle中存放数据
                            String[] items = new String[] {  "关闭","100毫秒","1秒","1分钟"};
                            bundle.putString("content","任务暂停，"+items[auto_Try_Time]+"后继续。");  //往Bundle中put数据
                            msg.setData(bundle);//mes利用Bundle传递数据
                            handler.sendMessage(msg);
                            settingShare.edit().putString("auto_book_logs","未到预约时间\n任务暂停，1分钟后继续。").commit();
                            return;
                        }
                        else {
                            if (result_code.equals("0")){//成功
                                addHistory(room,autoBookList.get(j).getSeat(),dateStr,"06:30",dbList.get(i).getNumber()+"");
                            }
                            j++;
                            i++;
                        }
                        msg=new Message();
                        msg.what=-2;
                        Bundle bundle = new Bundle();
                        bundle.putString("title",toastText);  //往Bundle中存放数据
                        bundle.putString("content",noticeSeat);  //往Bundle中put数据
                        msg.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(msg);
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
                        j++;
                    }
                }

                msg=new Message();
                msg.what=-3;
                bundle1 = new Bundle();
                bundle1.putString("content","自动预约：结束。");  //往Bundle中put数据
                msg.setData(bundle1);//mes利用Bundle传递数据
                handler.sendMessage(msg);
            }
        }).start();
    }
    //自动预约-高效
    public void autobook_efficient(){
        if (mHandler!=null)
            //销毁线程
            mHandler.removeCallbacks(mBackgroundRunnable);
        seat_i=0;
        Message msg=new Message();
        msg.what=-2;
        if (autoBookList==null)
            autoBookList = query();
        if (seat_dbList==null)
            seat_dbList = query2();
        if (autoBookList.size()==0){
            toastText = "未添加座位任务。";
            Bundle bundle = new Bundle();
            bundle.putString("title",toastText);  //往Bundle中存放数据
            bundle.putString("content","自动预约结束");  //往Bundle中put数据
            msg.setData(bundle);//mes利用Bundle传递数据

            auto_book_logs += toastText+"\n自动预约结束\n\n";
            settingShare.edit().putString("auto_book_logs",auto_book_logs).commit();

            handler.sendMessage(msg);
            return;
        }
        msg.what=-3;
        Bundle bundle1 = new Bundle();
        bundle1.putString("content","自动预约：执行中...");  //往Bundle中put数据
        msg.setData(bundle1);//mes利用Bundle传递数据
        handler.sendMessage(msg);
        int iii=-99;
        HandlerThread thread = new HandlerThread("MyHandlerThread");
        thread.start();//创建一个HandlerThread并启动它
        for (int j=0;j<autoBookList.size();++j) {
            while(iii==seat_i);
            iii=seat_i;
            String room = autoBookList.get(j).getRoom();
            seat_roomId = room;
            switch (room) {
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
            seat_room = room;
            seat_seat = autoBookList.get(j).getSeat();
            Handler mHandler = new Handler(thread.getLooper());//使用HandlerThread的looper对象创建Handler，如果使用默认的构造方法，很有可能阻塞UI线程
            mHandler.post(mBackgroundRunnable);//将线程post到Handler中

        }
        msg=new Message();
        msg.what=-3;
        bundle1 = new Bundle();
        bundle1.putString("content","自动预约：结束。");  //往Bundle中put数据
        msg.setData(bundle1);//mes利用Bundle传递数据
        handler.sendMessage(msg);
    }
    Runnable mBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            String room = seat_room;
            String roomId = seat_roomId;
            String seat = seat_seat;
            int i =seat_i++;
            int f =i;
            //日志
            String t = settingShare.getString("auto_book_logsTime","");
            //自动重试时间
            auto_Try_Time = settingShare.getInt("auto_Try_Time",0);
            String time = getTime();
            if (t.length()>10&&time.startsWith(t.substring(0,10)))
                auto_book_logs = "\n"+settingShare.getString("auto_book_logs","")+t+"\n----------\n\n";
            else
                auto_book_logs = "";
            settingShare.edit().putString("auto_book_logsTime",time).commit();

            Message msg=new Message();
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.DAY_OF_MONTH, instance.get(Calendar.DAY_OF_MONTH)+1);//日期
            int year = instance.get(Calendar.YEAR);//获取年份
            int month=instance.get(Calendar.MONTH);//获取月份
            int day=instance.get(Calendar.DAY_OF_MONTH);//获取日
            String dateStr = year+"-"+(month+1)+"-"+day;
            boolean auto_book_1minute = settingShare.getBoolean("auto_book_1minute",false);
            for (int j=0;i<seat_dbList.size()&&j==0;) {
                noticeSeat = "座位：["+room+"] "+seat+" "+"\n账号："+seat_dbList.get(i).getNumber()+"("+seat_dbList.get(i).getRemark()+")";

                //参数
                String str = "{\"intf_code\":\"UPD_PRE_SEAT\",\"params\":{\"seatNo\":\""+seat+"\",\"roomId\":\""+roomId+"\",\"dateStr\":\""+dateStr+"\",\"startHour\":\"06:30\",\"endHour\":\"23:00\",\"userPhysicalCard\":\""+seat_dbList.get(i).getNumber()+"\"}}";

                //服务器请求路径
                String strUrlPath = "http://211.70.171.14:9999/tsgintf/main/service";
                HttpUtils.setSign(getSeatSign());
                HttpUtils.setSign2(querySign2().getSign2());
                HttpUtils.setAuthorization(seat_dbList.get(i).getToken());
                String strResult = HttpUtils.submitPostData(strUrlPath, str, "utf-8");

                if (strResult.equals("error")){
                    msg=new Message();
                    msg.what=-1;
                    Bundle bundle = new Bundle();
                    bundle.putString("title",toastText);  //往Bundle中存放数据
                    bundle.putString("content",noticeSeat);  //往Bundle中put数据
                    msg.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(msg);
                    j++;
                    continue;
                }

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(strResult);
                    String result_code = "-999";
                    try {
                        result_code = jsonObject.getString("result_code");
                    }catch (Exception e){}
                    toastText = jsonObject.getString("result_desc");
                    if (result_code.equals("20014")||result_code.equals("20001")){//该座位正在被其他人操作，请稍后再试|此座位不存在
                        j++;
                    }
                    else if (result_code.equals("20004")&&(toastText.equals("抱歉，今日您已被取消预约权，明天解除！")||toastText.endsWith("不能再次预占"))){//今日被取消预约权
                        i++;
                        continue;
                    }
                    else if (result_code.equals("20013")){//现有预约记录冲突
                        i++;
                        continue;
                    }
                    else if (result_code.equals("-999")){//token错误
                        i++;
                    }else if (f==0&&auto_Try_Time!=0&&result_code.equals("20004")&&toastText.equals("亲，未到预约时间，不可以预约！")){
                        long Trytime = -1;
                        switch (auto_Try_Time){
                            case 1:
                                Trytime = 100;
                                break;
                            case 2:
                                Trytime = 1000;
                                break;
                            case 3:
                                Trytime = 1000*60;
                                break;
                        }
                        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(getApplicationContext(), AutoBookService.class);
                        intent.setAction(AutoBookService.ACTION_ALARM);
                        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        if(Build.VERSION.SDK_INT < 19){
                            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Trytime,pendingIntent);
                        }else{
                            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Trytime, pendingIntent);
                        }
                        msg=new Message();
                        msg.what=-2;
                        Bundle bundle = new Bundle();
                        bundle.putString("title","未到预约时间");  //往Bundle中存放数据
                        String[] items = new String[] {  "关闭","100毫秒","1秒","1分钟"};
                        bundle.putString("content","任务暂停，"+items[auto_Try_Time]+"后继续。");  //往Bundle中put数据
                        msg.setData(bundle);//mes利用Bundle传递数据
                        handler.sendMessage(msg);
                        settingShare.edit().putString("auto_book_logs","未到预约时间\n任务暂停，1分钟后继续。").commit();
                        return;
                    }
                    else {
                        if (result_code.equals("0")){//成功
                            addHistory(room,seat,dateStr,"06:30",seat_dbList.get(i).getNumber()+"");
                        }
                        j++;
                        i++;
                    }
                    msg=new Message();
                    msg.what=-2;
                    Bundle bundle = new Bundle();
                    bundle.putString("title",toastText);  //往Bundle中存放数据
                    bundle.putString("content",noticeSeat);  //往Bundle中put数据
                    msg.setData(bundle);//mes利用Bundle传递数据
                    handler.sendMessage(msg);
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
                    j++;
                }

            }

            if (mHandler!=null)
//                销毁线程
                mHandler.removeCallbacks(this);

        }
    };

    //查询
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

    public List<Db> query2(){//查询
        String s ="status,daytime COLLATE LOCALIZED desc";
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

                    auto_book_logs += "网络连接超时,任务取消"+"\n"+content+"\n\n";
                    settingShare.edit().putString("auto_book_logs",auto_book_logs).commit();

                    sendNotification("网络连接超时,任务取消",content);
                    break;
                case -2://发送通知
                    String str1 = msg.getData().getString("title");//接受msg传递过来的参数
                    String str2 = msg.getData().getString("content");//接受msg传递过来的参数

                    auto_book_logs += str1+"\n"+str2+"\n\n";
                    settingShare.edit().putString("auto_book_logs",auto_book_logs).commit();

                    sendNotification(str1,str2);
                    break;
                case -3:
                    boolean auto_book_showToast = settingShare.getBoolean("auto_book_showToast",true);
                    if (auto_book_showToast) {
                        String str = msg.getData().getString("content");//接受msg传递过来的参数
                        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                    }
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
                break;
            default:
                if (!networkFail)
                    Toast.makeText(context, "网络异常：请保持校园网连接", Toast.LENGTH_SHORT).show();
                getNetworkNotification();
                break;
        }
    }

    //网络异常通知
    public void getNetworkNotification() {
        networkFail = true;
        String title = "";
        if (autoBook) {
        }
        else if (autoSeat){}
        else {
//            manager.cancel(1);
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

    //更新预约记录
    public void addHistory(String room,String canSeat,String day,String startTime,String SuserPhysicalCard){
        //创建一个DatabaseHelper对象
        DatabaseHelper dbHelper1 = new DatabaseHelper(getApplicationContext(), "seat.db",3);
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


}