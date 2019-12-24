package ltd.yaokui.seat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ltd.yaokui.seat.utils.AppUtils;


public class AboutActivity extends Activity {
	//反馈信息
	private String versionName = null;
	private String deviceModel = null;
	private String versionRelease = null;
	private String feedback = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_us);
		
		deviceModel=android.os.Build.MODEL;
		versionRelease=android.os.Build.VERSION.RELEASE;
		TextView update = findViewById(R.id.update);
		update.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(),UpdateAboutActivity.class));
			}
		});


		// set real version
		ComponentName comp = new ComponentName(this, getClass());
		PackageInfo pinfo = null;
		try {
			pinfo = getPackageManager()
					.getPackageInfo(comp.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TextView version = (TextView) findViewById(R.id.version);
		
		version.setText(String.format("v %s", AppUtils.getVersionName(getApplicationContext())));
		

		// bind button click event
		Button okBtn = (Button) findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
				
		Button feedbackBtn = (Button) findViewById(R.id.feedback_btn);
		feedbackBtn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				String qqUrl = "mqqwpa://im/chat?chat_type=wpa&uin=" + "1361453981" + "&version=1";
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(qqUrl));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent); //跳转到制定QQ的聊天界面
			}
		});
	}

	

}
