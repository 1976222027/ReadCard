package com.apicloud.myReadCard.utils;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.sunmi.pay.hardware.aidlv2.emv.EMVOptV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadOptV2;
import com.sunmi.pay.hardware.aidlv2.readcard.ReadCardOptV2;
import com.sunmi.pay.hardware.aidlv2.security.SecurityOptV2;
import com.sunmi.pay.hardware.aidlv2.system.BasicOptV2;

import java.util.Locale;

public class MyApplication extends Application {

    public static Context context;
    public static BasicOptV2 mBasicOptV2;       // 获取基础操作模块
    public static ReadCardOptV2 mReadCardOptV2; // 获取读卡模块
    public static PinPadOptV2 mPinPadOptV2;     // 获取PinPad操作模块
    public static SecurityOptV2 mSecurityOptV2; // 获取安全操作模块
    public static EMVOptV2 mEMVOptV2;           // 获取EMV操作模块

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        initLocaleLanguage();
    }

    public static void initLocaleLanguage() {
        Resources resources = MyApplication.getContext().getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        int showLanguage = CacheHelper.getCurrentLanguage();
        if (showLanguage == Constant.LANGUAGE_AUTO) {
            config.locale = Resources.getSystem().getConfiguration().locale;
            LogUtil.e(Constant.TAG, config.locale.getCountry() + "---这是系统语言");
        } else if (showLanguage == Constant.LANGUAGE_CH_CN) {
            config.locale = Locale.SIMPLIFIED_CHINESE;
            LogUtil.e(Constant.TAG, "这是中文");
        } else {
            LogUtil.e(Constant.TAG, "这是英文");
            config.locale = Locale.ENGLISH;
        }
        resources.updateConfiguration(config, dm);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogUtil.e(Constant.TAG, "onConfigurationChanged");
    }

    public static Context getContext() {
        return context;
    }


}
