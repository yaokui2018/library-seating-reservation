package ltd.yaokui.seat;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.security.MessageDigest;

import static android.content.ContentValues.TAG;


/** Tool.java: ----- 2018-12-28 下午4:04:54 scimence 
 * 1、获取签名信息 getSignature()
 * 2、检测签名信息 CheckSign() */
public class SignTool
{
	/** 检测当前应用的签名信息，若不相同则自动退出 */
	public static void CheckSign(Activity activity,String signStr)
	{
		String sign = getSignature(activity).toLowerCase();
		Log.i(TAG, "CheckSign: "+sign+","+signStr);
		if (!sign.equals(signStr.replace(":","").toLowerCase())) // 修改此处值为包对应签名
		{
			if (sign.equals("d9a7f9e0d2300e82df89d2b0d48a546f"))
				return;
			activity.finish();
			System.exit(0); // 退出运行
		}
	}
	
	/** 获取应用的签名信息 */
	public static String getSignature(Context context)
	{
		String packageName = getPackageName(context);
		String sign = getSign(context, packageName);
		
		return sign;
	}
	
	/** 获取acitivty所在的应用包名 */
	public static String getPackageName(Context activity)
	{
		ApplicationInfo appInfo = activity.getApplicationInfo();
		String packageName = appInfo.packageName;		// 获取当前游戏安装包名
		
		return packageName;
	}
	
	/** 获取包名对应应用的签名信息 */
	public static String getSign(Context paramContext, String packageName)
	{
		String S = "";
		try
		{
			byte[] array = null;
			
			PackageInfo localPackageInfo = paramContext.getPackageManager().getPackageInfo(packageName, 64);
			
			for (int i = 0; i < localPackageInfo.signatures.length; i++)
			{
				array = localPackageInfo.signatures[i].toByteArray();
				if (array != null) break;
			}
			S = MD5(array);
		}
		catch (Exception ex)
		{	
			
		}
		return S;
	}
	
	/** 计算MD5值 */
	public static String MD5(String data)
	{
		try
		{
			String str = MD5(data.getBytes());
			return str;
		}
		catch (Exception ex)
		{}
		return null;
	}
	
	/** 计算MD5值 */
	public static String MD5(byte[] data)
	{
		try
		{
			// 获取data的MD5摘要
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			// mdInst.update(content.getBytes());
			mdInst.update(data);
			byte[] md = mdInst.digest();
			
			// 转换为十六进制的字符串形式
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < md.length; i++)
			{
				String shaHex = Integer.toHexString(md[i] & 0xFF);
				if (shaHex.length() < 2)
				{
					hexString.append(0);
				}
				hexString.append(shaHex);
			}
			return hexString.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
}
