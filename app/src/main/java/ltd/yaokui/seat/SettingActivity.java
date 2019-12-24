package ltd.yaokui.seat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.leon.lib.settingview.LSettingItem;

import ltd.yaokui.seat.utils.AppUtils;


public class SettingActivity extends BaseActivity {


    SharedPreferences settingShare;
    boolean skipLoading,stateBr,skip,notice3,booksort,fresh,delBind,wipeHistory,hideCancel,todayHistory,autoSeat,autoBook,ShutupLauncher;
    int bookshow,sort,countID,sortHistory,signTips;
    String app_title;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingShare = this.getSharedPreferences("setting",MODE_PRIVATE);
        skipLoading = settingShare.getBoolean("skipLoading",false);
        stateBr = settingShare.getBoolean("stateBr",true);
        skip = settingShare.getBoolean("skip",false);
        notice3 = settingShare.getBoolean("notice3",false);
        booksort = settingShare.getBoolean("booksort",true);
        fresh = settingShare.getBoolean("fresh",true);
        delBind = settingShare.getBoolean("delBind",true);
        wipeHistory = settingShare.getBoolean("wipeHistory",true);
        hideCancel = settingShare.getBoolean("hideCancel",false);
        todayHistory = settingShare.getBoolean("todayHistory",true);
        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);
        ShutupLauncher = settingShare.getBoolean("ShutupLauncher",false);
        //账号排序方式：0.id 1.学号 2.备注 3.id降序 4.学号降序 5.备注降序
        sort = settingShare.getInt("sort",3);
        //座位预约显示账号信息：0.备注 1.学号 2.学号+备注
        bookshow = settingShare.getInt("bookshow",2);
        //账号ID显示：0.序号 1.真实id
        countID = settingShare.getInt("countID",0);
        //记录排序方式：0.ID 1.学号 2.时间 3.状态 4.id降序 5.学号降序 6.时间降序 7.状态降序
        sortHistory = settingShare.getInt("sortHistory",2);
        //sign更新成功提示方式:0.弹框 1.吐司
        signTips = settingShare.getInt("signTips",0);
        //标题文字
        app_title = settingShare.getString("title","Library seating reservation");

        setContentView(R.layout.activity_setting);
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("设置");

        //定时任务
        if (autoBook&&!AutoBookService.isServiceRunning()){
            Intent intentOne = new Intent(getApplicationContext(), AutoBookService.class);
            stopService(intentOne);
            startService(intentOne);
        }
        if (autoSeat&&!AutoSeatService.isServiceRunning()){
            Intent intentOne = new Intent(getApplicationContext(), AutoSeatService.class);
            stopService(intentOne);
            startService(intentOne);
        }

        //设置自动预约/入座开启状态文字
        LSettingItem auto_lSettingItem = (LSettingItem) findViewById(R.id.auto_seat);
        auto_lSettingItem.setRightText(autoSeat?"已开启":"未开启");
        auto_lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                Intent intent = new Intent(getApplicationContext(),AutoSeatActivity.class);
                startActivity(intent);
            }
        });
        auto_lSettingItem = (LSettingItem) findViewById(R.id.auto_book);
        auto_lSettingItem.setRightText(autoBook?"已开启":"未开启");

        auto_lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                Intent intent = new Intent(getApplicationContext(),AutoBookActivity.class);
                startActivity(intent);
            }
        });
        //加群
        LSettingItem qqun = (LSettingItem) findViewById(R.id.qqun);
        qqun.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                if(joinQQGroup("_NIFMLJnRWuPc1mPcHILKWrC33fXHJyc"))
                    Toast.makeText(getApplicationContext(),"加群口令：好哥哥。",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(),"跳转失败。",Toast.LENGTH_SHORT).show();
            }
        });

        LSettingItem mSettingItemOne = (LSettingItem) findViewById(R.id.skipLoading);
        if (skipLoading)
            mSettingItemOne.clickOn();
        mSettingItemOne.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("skipLoading",isChecked).commit();
            }
        });
        String s ="";

        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.stateBr);
        if (stateBr)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("stateBr",isChecked).commit();
            }
        });

        //跳过加载动画
        lSettingItem = (LSettingItem) findViewById(R.id.skip);
        if (skip)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("skip",isChecked).commit();
            }
        });
        //相同公告显示三次
        lSettingItem = (LSettingItem) findViewById(R.id.notice3);
        if (notice3)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("notice3",isChecked).commit();
            }
        });
        //预约界面显示排序按钮
        lSettingItem = (LSettingItem) findViewById(R.id.booksort);
        if (booksort)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("booksort",isChecked).commit();
            }
        });
        //页面下拉刷新
        lSettingItem = (LSettingItem) findViewById(R.id.fresh);
        if (fresh)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("fresh",isChecked).commit();
                Toast.makeText(getApplicationContext(),"重启APP生效",Toast.LENGTH_SHORT).show();
            }
        });
        //显示解绑选项
        lSettingItem = (LSettingItem) findViewById(R.id.delBind);
        if (delBind)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("delBind",isChecked).commit();
            }
        });
        //显示清空记录按钮
        lSettingItem = (LSettingItem) findViewById(R.id.wipeHistory);
        if (wipeHistory)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("wipeHistory",isChecked).commit();
            }
        });
        //不显示已取消记录
        lSettingItem = (LSettingItem) findViewById(R.id.hideCancel);
        if (hideCancel)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("hideCancel",isChecked).commit();
            }
        });
        //只显示今天以后记录
        lSettingItem = (LSettingItem) findViewById(R.id.todayHistory);
        if (todayHistory)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("todayHistory",isChecked).commit();
            }
        });
        //开机自启动
        lSettingItem = (LSettingItem) findViewById(R.id.ShutupLauncher);
        if (ShutupLauncher)
            lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                settingShare.edit().putBoolean("ShutupLauncher",isChecked).commit();
                if (isChecked){
                    Toast.makeText(getApplicationContext(),"也不知道有没有用，试试看呗",Toast.LENGTH_SHORT).show();
                }
            }
        });
        //用户改进计划
        lSettingItem = (LSettingItem) findViewById(R.id.UserImprove);
        lSettingItem.clickOn();
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.UserImprove);
                lSettingItem.clickOn();

                android.support.v7.app.AlertDialog.Builder builder1 = new android.support.v7.app.AlertDialog.Builder(SettingActivity.this);
                builder1.setIcon(R.mipmap.ic_launcher);
                builder1.setTitle("no no no");
                builder1.setMessage("霸权主义了解一下~");
                builder1.setCancelable(true);
                builder1.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                builder1.create().show();
            }
        });

        //标题显示
        lSettingItem = (LSettingItem) findViewById(R.id.app_title);
        s = app_title;
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                final EditText inputServer = new EditText(SettingActivity.this);
                inputServer.setText(app_title);
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(SettingActivity.this);
                builder.setTitle("主界面标题").setIcon(R.drawable.ic_title_black_24dp).setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        settingShare.edit().putString("title",inputServer.getText().toString()).commit();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.app_title);
                        lSettingItem.setRightText(inputServer.getText().toString());
                        Toast.makeText(getApplicationContext(),"重启APP生效",Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
            }
        });

        //sign成功更新提示
        lSettingItem = (LSettingItem) findViewById(R.id.signTips);
        s = "弹框提醒";
        if (signTips==1)
            s = "吐司通知";
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                final String[] items = new String[] {  "弹框提醒","吐司通知"};
                builder.setTitle("Sign更新成功提示方式");
                builder.setSingleChoiceItems(items, signTips, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signTips = which;
                        settingShare.edit().putInt("signTips",signTips).commit();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.signTips);
                        lSettingItem.setRightText(items[signTips]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        //id显示
        lSettingItem = (LSettingItem) findViewById(R.id.countID);
        s = "序号";
        if (countID==1)
            s = "真实ID";
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                final String[] items = new String[] {  "序号","真实ID"};
                builder.setTitle("账号ID显示");
                builder.setSingleChoiceItems(items, countID, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        countID = which;
                        settingShare.edit().putInt("countID",countID).commit();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.countID);
                        lSettingItem.setRightText(items[countID]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        //排序
        lSettingItem = (LSettingItem) findViewById(R.id.sort);
        s = "ID";
        switch (sort){
            case 1:
                s="学号";
                break;
            case 2:
                s="备注";
                break;
            case 3:
                s="预约状态";
                break;
            case 4:
                s="ID（降序）";
                break;
            case 5:
                s="学号（降序）";
                break;
            case 6:
                s="备注（降序）";
                break;
            case 7:
                s="预约状态（降序）";
                break;
        }
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                final String[] items = new String[] {  "ID","学号","备注","预约状态","ID（降序）","学号（降序）","备注（降序）","预约状态（降序）" };
                builder.setTitle("账号排序方式");
                builder.setSingleChoiceItems(items, sort, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sort = which;
                        settingShare.edit().putInt("sort",sort).commit();
                        if (sort==3||sort==7){
//                            Toast.makeText(getApplicationContext(),"仅适用于使用本APP进行座位预约产生的数据。",Toast.LENGTH_SHORT).show();
                        }
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.sort);
                        lSettingItem.setRightText(items[sort]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        //记录排序
        lSettingItem = (LSettingItem) findViewById(R.id.sortHistory);
        s = "ID";
        switch (sortHistory){
            case 1:
                s="学号";
                break;
            case 2:
                s="预约时间";
                break;
            case 3:
                s="预约状态";
                break;
            case 4:
                s="ID（降序）";
                break;
            case 5:
                s="学号（降序）";
                break;
            case 6:
                s="预约时间（降序）";
                break;
            case 7:
                s="预约状态（降序）";
                break;
        }
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                final String[] items = new String[] {  "ID","学号","预约时间","预约状态","ID（降序）","学号（降序）","预约时间（降序）","预约状态（降序）" };
                builder.setTitle("记录排序方式");
                builder.setSingleChoiceItems(items, sortHistory, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sortHistory = which;
                        settingShare.edit().putInt("sortHistory",sortHistory).commit();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.sortHistory);
                        lSettingItem.setRightText(items[sortHistory]);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });


        lSettingItem = (LSettingItem) findViewById(R.id.bookshow);
        s = "备注+学号";
        if (bookshow==0)
            s="备注";
        else if (bookshow==1)
            s = "学号";
        lSettingItem.setRightText(s);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                final String[] items = new String[] {  "备注","学号", "备注+学号" };
                builder.setTitle("预约账号显示");
                builder.setSingleChoiceItems(items, bookshow, new DialogInterface.OnClickListener() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bookshow = which;
                        String s = "备注+学号";
                        if (bookshow==0)
                            s="备注";
                        else if (bookshow==1)
                            s = "学号";
                        settingShare.edit().putInt("bookshow",bookshow).commit();
                        LSettingItem lSettingItem = (LSettingItem) findViewById(R.id.bookshow);
                        lSettingItem.setRightText(s);
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        lSettingItem = (LSettingItem) findViewById(R.id.version);
        lSettingItem.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                Intent intent = new Intent(getApplicationContext(),AboutActivity.class);
                startActivity(intent);
//                Toast.makeText(getApplicationContext(),"提示：“我的账号”里点击学号可以查看预约记录。\n\n当前版本：v"+AppUtils.getVersionName(getApplicationContext())+"\n\n\n【本软件仅供内部使用，禁止传播】。",Toast.LENGTH_LONG).show();
            }
        });
        lSettingItem.setRightText("v"+ AppUtils.getVersionName(getApplicationContext()));
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

    @Override
    public void onResume() {
        autoSeat = settingShare.getBoolean("autoSeat",false);
        autoBook = settingShare.getBoolean("autoBook",false);
        LSettingItem auto_lSettingItem = (LSettingItem) findViewById(R.id.auto_seat);
        auto_lSettingItem.setRightText(autoSeat?"已开启":"未开启");
        auto_lSettingItem = (LSettingItem) findViewById(R.id.auto_book);
        auto_lSettingItem.setRightText(autoBook?"已开启":"未开启");
        super.onResume();
    }
    /****************
     *
     * 发起添加群流程。群号：111(912646450) 的 key 为： _NIFMLJnRWuPc1mPcHILKWrC33fXHJyc
     * 调用 joinQQGroup(_NIFMLJnRWuPc1mPcHILKWrC33fXHJyc) 即可发起手Q客户端申请加群 111(912646450)
     *
     * @param key 由官网生成的key
     * @return 返回true表示呼起手Q成功，返回fals表示呼起失败
     ******************/
    public boolean joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
            return false;
        }
    }



}
