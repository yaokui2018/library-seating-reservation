package ltd.yaokui.seat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.carlt.networklibs.NetworkManager;
import com.gyf.barlibrary.ImmersionBar;

public class BaseActivity extends AppCompatActivity {

     private ImmersionBar mImmersionBar;
     @Override
     protected void onCreate(@Nullable Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
//       mImmersionBar = ImmersionBar.with(this);
//       mImmersionBar.init();   //所有子类都将继承这些相同的属性
        
     }
 
     @Override
     protected void onDestroy() {
         super.onDestroy();
         if (mImmersionBar != null)
            mImmersionBar.destroy();  //必须调用该方法，防止内存泄漏，不调用该方法，如果界面bar发生改变，在不关闭app的情况下，退出此界面再进入将记忆最后一次bar改变的状态
     }
 }