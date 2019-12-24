package ltd.yaokui.seat;

import android.webkit.JavascriptInterface;

//sign表
public class Sign2 {
    private int id;
    private String sign2;

    @JavascriptInterface
    public int getId() {
        return id;
    }

    @JavascriptInterface
    public void setId(int id) {
        this.id = id;
    }
    @JavascriptInterface
    public String getSign2() {
        return sign2;
    }
    @JavascriptInterface
    public void setSign2(String sign2) {
        this.sign2 = sign2;
    }
}