package ltd.yaokui.seat;

import android.webkit.JavascriptInterface;

//sign表
public class Sign {
    private int id;
    private String sign;
    private String date;
    private int id_;

    @JavascriptInterface
    public int getId() {
        return id;
    }
    @JavascriptInterface
    public void setId(int id) {
        this.id = id;
    }
    @JavascriptInterface
    public String getSign() {
        return sign;
    }
    @JavascriptInterface
    public void setSign(String sign) {
        this.sign = sign;
    }
    @JavascriptInterface
    public String getDate() {
        return date;
    }
    @JavascriptInterface
    public void setDate(String date) {
        this.date = date;
    }
    @JavascriptInterface
    public int getId_() {
        return id_;
    }
    @JavascriptInterface
    public void setId_(int id_) {
        this.id_ = id_;
    }
}