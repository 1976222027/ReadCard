package com.apicloud.hanchao;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.webkit.ConsoleMessage;

import com.apicloud.myReadCard.Constant;
import com.apicloud.myReadCard.NFCInstans;
import com.apicloud.myReadCard.utils.LogUtil;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.uzmap.pkg.openapi.IncPackage;
import com.uzmap.pkg.openapi.WebViewProvider;
import com.uzmap.pkg.uzcore.UZAppActivity;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONException;
import org.json.JSONObject;

import sunmi.paylib.SunmiPayKernel;

public final class HomeActivity extends UZAppActivity {
    static String a = "Decompile Is A Stupid Behavior";
    public static HomeActivity instans;

    public HomeActivity() {
    }

    // Intent 接口
    private NFCInstans nfcInstans;
    public void getNFCTagListener(NFCInstans getTagListener) {
        this.nfcInstans = getTagListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instans = this;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nfcInstans.getDestory();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        nfcInstans.getIntent(intent);//接口
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

