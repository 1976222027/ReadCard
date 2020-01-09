package com.uzmap.pkg;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import com.uzmap.pkg.openapi.IncPackage;
import com.uzmap.pkg.openapi.WebViewProvider;
import com.uzmap.pkg.uzcore.UZAppActivity;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

public final class EntranceActivit extends UZAppActivity {
    static String a = "Decompile Is A Stupid Behavior";
    public static EntranceActivit instans;
    private NFCInstans nfcInstans;

    public EntranceActivit() {
    }

    public void getActivityListener(NFCInstans getTagListener) {
        this.nfcInstans = getTagListener;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.nfcInstans.getIntent(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        this.nfcInstans.getKeyEvent(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instans = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.nfcInstans.getDestory();
    }

    @Override
    protected final boolean isFromNativeSDK() {
        return false;
    }

    @Override
    protected final void onProgressChanged(WebViewProvider provider, int newProgress) {
    }

    @Override
    protected final void onPageStarted(WebViewProvider provider, String url, Bitmap favicon) {
    }

    @Override
    protected final void onPageFinished(WebViewProvider provider, String url) {
    }

    @Override
    protected final boolean shouldOverrideUrlLoading(WebViewProvider provider, String url) {
        return false;
    }

    @Override
    protected final void onReceivedTitle(WebViewProvider provider, String title) {
    }

    @Override
    protected boolean shouldForbiddenAccess(String host, String module, String api) {
        return false;
    }

    @Override
    protected boolean onHtml5AccessRequest(WebViewProvider provider, UZModuleContext moduleContext) {
        return false;
    }

    @Override
    protected boolean onSmartUpdateFinish(IncPackage iPackage) {
        return false;
    }

    @Override
    public void onConsoleMessage(ConsoleMessage console) {
    }
}

