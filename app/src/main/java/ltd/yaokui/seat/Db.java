package ltd.yaokui.seat;

import android.webkit.JavascriptInterface;
//账号表
public class Db {
    private int id;
    private int number;
    private String  password;
    private String remark;
    private int status;//0.未预约 1.已预约
    private String daytime;
    private String token;

    @JavascriptInterface
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @JavascriptInterface
    public int getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @JavascriptInterface
    public int getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @JavascriptInterface
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JavascriptInterface
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    @JavascriptInterface
    public String getDaytime() {
        return daytime;
    }

    public void setDaytime(String daytime) {
        this.daytime = daytime;
    }

    @JavascriptInterface
    public String getToken() {
        return token;
    }
    @JavascriptInterface
    public void setToken(String token) {
        this.token = token;
    }
}
