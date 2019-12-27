package com.apicloud.myReadCard.pos;

import android.app.Activity;
import android.os.Bundle;

import com.apicloud.myReadCard.Constant;
import com.apicloud.hanchao.MyApplication;
import com.apicloud.myReadCard.R;
import com.apicloud.myReadCard.utils.LogUtil;

import sunmi.paylib.SunmiPayKernel;
//POS 初始化
public class InitPosActivity extends Activity {

    private SunmiPayKernel mSMPayKernel;
    private boolean isDisConnectService = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initpos);
        connectPayService();
    }
//初始化时 联机一下pay服务
    private void connectPayService() {
        mSMPayKernel = SunmiPayKernel.getInstance();
        mSMPayKernel.initPaySDK(this, mConnectCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSMPayKernel != null) {
            mSMPayKernel.destroyPaySDK();
        }
    }

    /**
     * 连接状态回调
     */
    private SunmiPayKernel.ConnectCallback mConnectCallback = new SunmiPayKernel.ConnectCallback() {

        @Override
        public void onConnectPaySDK() {
            LogUtil.e(Constant.TAG, "onConnectPaySDK");
            try {
                MyApplication.mEMVOptV2 = mSMPayKernel.mEMVOptV2;
                MyApplication.mBasicOptV2 = mSMPayKernel.mBasicOptV2;
                MyApplication.mPinPadOptV2 = mSMPayKernel.mPinPadOptV2;
                MyApplication.mReadCardOptV2 = mSMPayKernel.mReadCardOptV2;
                MyApplication.mSecurityOptV2 = mSMPayKernel.mSecurityOptV2;
                isDisConnectService = false;
//                showToast(R.string.connect_success);
                setResult(501);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnectPaySDK() {
            LogUtil.e(Constant.TAG, "onDisconnectPaySDK");
            isDisConnectService = true;
            setResult(502);
            finish();
//            showToast(R.string.connect_fail);
        }

    };
}
