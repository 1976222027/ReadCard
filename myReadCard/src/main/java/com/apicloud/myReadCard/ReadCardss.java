package com.apicloud.myReadCard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.apicloud.myReadCard.utils.ByteUtil;
import com.apicloud.myReadCard.utils.Constant;
import com.apicloud.myReadCard.utils.LogUtil;
import com.apicloud.myReadCard.utils.MyApplication;
import com.apicloud.myReadCard.utils.NfcReadHelper;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.uzmap.pkg.EntranceActivit;
import com.uzmap.pkg.EntranceActivity;
import com.uzmap.pkg.NFCInstans;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sunmi.paylib.SunmiPayKernel;

/**
 * NFC 读卡
 */

public class ReadCardss extends UZModule implements NFCInstans {

    public ReadCardss(UZWebView webView) {
        super(webView);
        //注册监听 new intent
        LogUtil.e("webview", "pohene");
    }

    /**
     * 公共数据
     */
    private int type = 0, sector = 0;//扇区
    private String keyStr = ByteUtil.baseKeyB, datas = "";//默认密码
    private int keyType;    // 密钥类型，0表示KEY A、1表示 KEY B
    private byte[] keyBytes;
    /**
     * 初始化POS数据
     */
    private SunmiPayKernel mSMPayKernel;

    /**
     * 使用手机初始化
     */
    private PendingIntent mPendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;
    private NfcAdapter adapter;

    public void phone() {
        //这里才初始化 resume 当时还没有adapter so 初始化后还要
        adapter = NfcAdapter.getDefaultAdapter(getContext());
        //读卡
        mPendingIntent = PendingIntent.getActivity(getContext(), 0,
                new Intent(getContext(), getContext().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);//intent nfc类型
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};
        techListsArray = new String[][]{new String[]{NfcA.class.getName()}};
        Log.d(" mTechLists", NfcF.class.getName() + techListsArray.length);
        if (null == adapter) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "设备不支持nfc");
                initContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //  Toast.makeText(getContext(), "不支持NFC功能", Toast.LENGTH_SHORT).show();
        } else if (!adapter.isEnabled()) {
            //打开NFC开关
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            // 根据包名打开对应的设置界面
            startActivityForResult(intent, 102);
        } else if (adapter.isEnabled()) {
            //开启前台调度系统
            adapter.enableForegroundDispatch(getContext(), mPendingIntent, intentFiltersArray, techListsArray);
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", "初始化完成");
                initContext.success(ret, false);
                isInitPhone = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 初始化
     *
     * @param moduleContext
     */
    UZModuleContext initContext;
    private boolean isPos = false;
    private boolean isInitPhone = false;
    private boolean isInitPos = false;

    public void jsmethod_init(final UZModuleContext moduleContext) {
//        //判断终端是手机还是POS机
        String model = SystemProperties.get("ro.product.model");
        Log.e("wang", "终端名字为" + model);
        if (model.equals("P1N") || model.equals("P1_4G") || model.equals("P2 Pro") || model.equals("P2 Lite")) {
            isPos = true;
        } else {
            isPos = false;
            //初始化 监听newIntent
            EntranceActivit.instans.getActivityListener(this);
        }
//        isInitPhone = false;//        isInitPos = false;


        initContext = moduleContext;
        andContext = moduleContext;
        if (!isPos) {
            phone();
        } else {
            initPos();//初始化操作
        }
    }

    UZModuleContext andContext;//读卡后 回调用
    UZModuleContext prepareContext;

    /**
     * 0读卡数据
     *
     * @param moduleContext
     */
    public void jsmethod_readCard(UZModuleContext moduleContext) {
        type = 0;
        readAndWrite(moduleContext);
    }

    /**
     * 1写卡
     *
     * @param moduleContext
     */
    public void jsmethod_writeCard(UZModuleContext moduleContext) {
        type = 1;
        readAndWrite(moduleContext);
    }

    //2开卡更换秘钥FCF->KCK
    public void jsmethod_createCard(UZModuleContext moduleContext) {
        type = 2;
        readAndWrite(moduleContext);
    }

    //检测 pos
    public void jsmethod_checkPos(UZModuleContext moduleContext) {
        if (isPos) {
            checkCard();
        }
    }

    /**
     * 控制位7f078869 控制：
     *
     * @1数据块块012均可用KeyA/B读写
     * @2密码块3均不可读，仅KeyB能写 综合代码 type 3销卡 2开卡 1写卡 0读卡
     * 初始化、读、写卡 用keyA认证
     * 销卡、开卡用keyB认证  涉及块3 密码区
     */
    public void readAndWrite(UZModuleContext moduleContext) {
        prepareContext = moduleContext;
        if (!isPos) {
            if (isInitPhone && adapter.isEnabled()) {
                //type 3销卡 2开卡 1写卡 0读卡
                readCard(type, moduleContext);
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未初始化");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                prepareContext.success(ret, false);
            }
        } else {
            if (isInitPos) {
                //POS
                checkCard();//开启监测卡
                readPosCard(type, moduleContext);
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未初始化");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                prepareContext.success(ret, false);
            }
        }
    }

    //3销卡
    public void jsmethod_cleanCard(UZModuleContext moduleContext) {
        type = 3;
        readAndWrite(moduleContext);
    }

    /**
     * 使用手机读卡
     */
    private void readCard(int type, UZModuleContext moduleContext) {
        andContext = moduleContext;
        String keyAStr = moduleContext.optString("key", "");
        String block = moduleContext.optString("sector", "0");//扇区
        if (type == 1) {//写卡//写卡数据
            String data = moduleContext.optString("data", "");
            if (!TextUtils.isEmpty(data)) {
                datas = data;
            }
        }
        if (keyAStr.isEmpty()) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入秘钥");
                moduleContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        if (!keyAStr.isEmpty()) {
            keyStr = ByteUtil.psw2HexStr(keyAStr);//6->12
        }
        sector = Integer.parseInt(block);
        if (sector > 15 || sector < 0) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入正确的扇区");
                moduleContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
//        Intent intent = new Intent(getContext(), ReadActivity.class);
//        intent.putExtra("block", sector);
//        intent.putExtra("key", keyStr);
//        intent.putExtra("type", type);
//        intent.putExtra("data", datas);
//        startActivityForResult(intent, 100);
    }

    /**
     * Pos初始化 //初始化时 联机一下pay服务
     * connectPayService()
     */
    public void initPos() {
        if (isInitPos) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", "Pos机初始化成功");
                initContext.success(ret, false);
                isInitPos = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mSMPayKernel = SunmiPayKernel.getInstance();
            mSMPayKernel.initPaySDK(getContext(), mConnectCallback);
//            checkCard();//开启监测卡
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
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", "Pos机初始化成功");
                    initContext.success(ret, false);
                    isInitPos = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                showToast(R.string.connect_success);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnectPaySDK() {
            LogUtil.e(Constant.TAG, "onDisconnectPaySDK");
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "Pos机初始化失败");
                initContext.success(ret, false);
                isInitPos = false;
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            showToast(R.string.connect_fail);
        }

    };

    /**
     * 使用pos读卡
     */
    public void readPosCard(int type, UZModuleContext moduleContext) {
        andContext = moduleContext;
        String keyAStr = moduleContext.optString("key", "");
        String block = moduleContext.optString("sector");
        if (type == 1) {
            datas = moduleContext.optString("data", "");
        }//写卡数据
        if (keyAStr.isEmpty()) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入秘钥");
                moduleContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        if (!keyAStr.isEmpty()) {
            keyStr = ByteUtil.psw2HexStr(keyAStr);//6->12hex
        }
        sector = Integer.parseInt(block);
        if (sector > 15 || sector < 0) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "请输入正确的扇区");
                moduleContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

    }


    //验证key 读卡
    private void readAllSector(int type) {
        String s0 = "";
        String s1 = "";
        String s2 = "";
        String s3 = "";
        boolean result = false;
        int startBlockNo = sector * 4;//sector扇区起始块0
        if (type == 2) {//开卡
            try {
                keyType = 1;//KeyB验证
                result = MyApplication.mReadCardOptV2.mifareAuth(keyType, startBlockNo, MifareClassic.KEY_DEFAULT) == 0;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (type == 3) {
            keyType = 1;//KeyB验证
            result = m1Auth(keyType, startBlockNo, keyBytes);
        } else {
            keyType = 0;//KeyA验证  读卡写卡
            result = m1Auth(keyType, startBlockNo, keyBytes);
        }
        if (result) {
            int res = -1;
            byte[] outData = new byte[128];//1扇区总4*16=64字节 128位
            if (type == 0) {//读卡
                // outData = new byte[128];
                res = m1ReadBlock(startBlockNo, outData);///sector扇区块0
                if (res >= 0 && res <= 16) {
                    //读数据
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "0read outData:" + hexStr);
                    s0 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "0outData:" + s0);
                }
//                outData = new byte[128];
                res = m1ReadBlock(startBlockNo + 1, outData);////sector扇区块1
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "1read outData:" + hexStr);
                    s1 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "1outData:" + s1);
                }
//                outData = new byte[128];
                //读卡
                res = m1ReadBlock(startBlockNo + 2, outData);////sector扇区块2
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "2read outData:" + hexStr);
                    s2 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "2outData:" + s2);
                }
                //记录刷卡时间
                m1WriteBlock(startBlockNo + 1, ByteUtil.String2Byte(System.currentTimeMillis() + ""));//字节

                String data0 = s0.trim();
                String data1 = ByteUtil.timeStamp2Date(s1.trim());
                String data2 = ByteUtil.timeStamp2Date(s2.trim());
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("data0", data0.trim());
                    ret.put("data1", "上次刷卡:" + data1);
                    ret.put("data2", "开卡时间:" + data2);
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (type == 1) {//写卡
                outData = ByteUtil.String2Byte(datas)/*new byte[32]*/;
                res = m1WriteBlock(startBlockNo, outData);//块0 123
                Log.e("多少字节", res + "=" + res * 2 + "位");
                if (res >= 0 && res <= 16) {
                    //读数据
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "0read outData:" + hexStr);
                    s0 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "0outData:" + s0);
                }
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", "写卡成功:" + datas);
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (type == 2) {//开卡 将块3 key换掉
                outData = ByteUtil.hexStr2Bytes(keyStr + ByteUtil.commdKeyB + keyStr);/*new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69,(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11}*/
                res = m1WriteBlock(startBlockNo + 3, outData);//字节
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "3开卡:" + hexStr);
                    s3 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "3开卡:" + s3);
                }
                m1WriteBlock(startBlockNo + 2, ByteUtil.String2Byte(System.currentTimeMillis() + ""));//字节
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", "开卡成功");
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (type == 3) {//销卡 key还原
//                outData = ByteUtil.byteMerger(MifareClassic.KEY_DEFAULT, new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69}, MifareClassic.KEY_DEFAULT);
                outData = ByteUtil.baseKey;
                m1WriteBlock(startBlockNo, ByteUtil.bytes0);//字节
                m1WriteBlock(startBlockNo + 1, ByteUtil.bytes0);//字节
                m1WriteBlock(startBlockNo + 2, ByteUtil.bytes0);//字节
                res = m1WriteBlock(startBlockNo + 3, outData);//字节
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "3销卡:" + hexStr);
                    s3 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "3销卡:" + s3);
                }
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", "销卡成功");
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "卡片认证失败");
                andContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        cancelCheckCard();//结束见卡
//        checkCard();//结束再检测卡
    }

    private void checkCard() {
        try {
//            showHintDialog();
            MyApplication.mReadCardOptV2.checkCard(AidlConstantsV2.CardType.MIFARE.getValue(), mCheckCardCallback, 60);//延时范围1-120s
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //监测卡
    private CheckCardCallbackV2 mCheckCardCallback = new CheckCardCallbackV2.Stub() {
        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {

        }

        @Override
        public void findICCard(String atr) throws RemoteException {

        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            LogUtil.e(Constant.TAG, "findRFCard:" + uuid);
//            dismissHintDialog();
            //找到卡去读
            readM1();
        }

        @Override
        public void onError(int code, String message) throws RemoteException {
//            checkCard();
        }

    };

    //destory结束
    void jsmethod_closeScan() {
        if (mSMPayKernel != null) {
            mSMPayKernel.destroyPaySDK();
        }
        cancelCheckCard();
    }

    private void cancelCheckCard() {
        try {
            MyApplication.mReadCardOptV2.cardOff(AidlConstantsV2.CardType.MIFARE.getValue());
            MyApplication.mReadCardOptV2.cancelCheckCard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readM1() {
        boolean check = checkParams();
        if (check) {
            readAllSector(type);
        }
    }

    /**
     * 验证key空
     *
     * @return
     */
    private boolean checkParams() {
        keyBytes = ByteUtil.hexStr2Bytes(keyStr);
        if (keyBytes == null) {
            showToast(R.string.card_key_hint);
            return false;
        }
        return true;
    }

    /**
     * m1 card auth 卡认证
     */
    private boolean m1Auth(int keyType, int block, byte[] keyData) {
        boolean val = false;
        try {
            String hexStr = ByteUtil.bytes2HexStr(keyData);//-> byte[]
            LogUtil.e(Constant.TAG, "扇区block:" + block + " keyType:" + keyType + " keyBytes:" + hexStr);
            int result = MyApplication.mReadCardOptV2.mifareAuth(keyType, block, keyData);
            LogUtil.e(Constant.TAG, "m1Auth result:" + result);
            val = result == 0;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (val) {
            return true;
        } else {
//            showToast(R.string.card_auth_fail);//认证失败
            checkCard();
            return false;
        }
    }

    /**
     * m1 read block data
     */
    private int m1ReadBlock(int block, byte[] blockData) {
        try {
            int result = MyApplication.mReadCardOptV2.mifareReadBlock(block, blockData);
            LogUtil.e(Constant.TAG, "m1ReadBlock result:" + result);
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -123;
    }

    /**
     * 写卡
     */
    private int m1WriteBlock(int block, byte[] blockData) {
        try {
            int result = MyApplication.mReadCardOptV2.mifareWriteBlock(block, blockData);
            LogUtil.e(Constant.TAG, "m1WriteBlock result:" + result);
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -123;
    }

    public void showToast(int redId) {
        showToastOnUI(getContext().getString(redId));
    }

    private void showToastOnUI(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "" + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == 104) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "读取失败");
                prepareContext.success(ret, false);
                isInitPhone = false;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 100 && resultCode == 105) {
            String data0 = data.getStringExtra("data0");
            String data1 = data.getStringExtra("data1");
            String data2 = data.getStringExtra("data2");
            data0 = ByteUtil.hexStr2Str(data0);
            data1 = ByteUtil.hexStr2Str(data1);
            data2 = ByteUtil.hexStr2Str(data2);
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("data0", data0);
                ret.put("data1", data1);
                ret.put("data2", data2);
                prepareContext.success(ret, false);
//                isInitPhone = false;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 100 && resultCode == 106) {
           String datas = data.getStringExtra("data");
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", true);
                ret.put("msg", datas);
                prepareContext.success(ret, false);
//                isInitPhone = false;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 102 && resultCode == 0) {
            if (adapter.isEnabled()) {
                adapter.enableForegroundDispatch(getContext(), mPendingIntent, intentFiltersArray, techListsArray);
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", "nfc已开启");
                    andContext.success(ret, false);
                    isInitPhone = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "请打开nfc功能");
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    //监听 页面可见
    public void jsmethod_resumeJS(UZModuleContext moduleContext) {

        //开启前台调度系统
        if (adapter != null && isInitPhone) {
            adapter.enableForegroundDispatch(getContext(), mPendingIntent, intentFiltersArray, techListsArray);
        }
    }

    //监听页面 不可见
    public void jsmethod_pauseJS(UZModuleContext moduleContext) {
        //关闭前台调度系统
        if (adapter != null && isInitPhone) {
            adapter.disableForegroundDispatch(getContext());
        }
        if (isInitPos) {

        }
    }

    public void jsmethod_intentJS(UZModuleContext moduleContext) {
        JSONObject jsonObject =moduleContext.optJSONObject("param");
        Iterator<String> it = jsonObject.keys();
        while(it.hasNext()){
            String key = it.next();
            if (key.equals(NfcAdapter.EXTRA_TAG)){
        Intent intent = new Intent(getContext(), ReadActivity.class);
        intent.putExtra("block", sector);
        intent.putExtra("key", keyStr);
        intent.putExtra("type", type);
        intent.putExtra("data", datas);
        startActivityForResult(intent, 100);
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                //读取卡数据失败
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "验证卡片失败");
                    andContext.success(ret, false);
//                    isInitPhone = false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (msg.what == 2) {
                //读取卡数据成功
                String data0 = msg.getData().getString("data0");
                String data1 = msg.getData().getString("data1");
                String data2 = msg.getData().getString("data2");
                data1 = ByteUtil.timeStamp2Date(data1.trim());
                data2 = ByteUtil.timeStamp2Date(data2.trim());
//            data1 = ByteUtil.hexStr2Str(data1);
                JSONObject ret = new JSONObject();
                try {
                    if (sector == 0) {
                        ret.put("status", false);
                        ret.put("data0", "初始状态");
                    } else {
                        ret.put("status", true);
                        ret.put("data0", data0.trim());
                    }
                    ret.put("data1", "上次刷卡:" + data1);
                    ret.put("data2", "开卡时间:" + data2);
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (msg.what == 3) {
                //写卡 开卡 销卡
                String datas = msg.getData().getString("data");
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", true);
                    ret.put("msg", datas);
                    andContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //activity 有 newintent时 回调到此
    @Override
    public void getIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //读取卡全部数据
            NfcReadHelper.getInstence(intent)
                    .getAllData(type, datas, keyStr, sector, new NfcReadHelper.NFCCallback() {
                        @Override
                        public void callBack(Map<String, List<String>> data) {
//                            String text = "";
                            String text0 = "";
                            String text1 = "";
                            String text2 = "";
                            for (String key : data.keySet()) {
                                List list = data.get(key);
//                            for (int i = 0; i < list.size(); i++) {
//                                String str = "第" + key + "扇区" + "第" + i + "块内容：\n" + list.get(i);
//                                text += str + "\n";
//                            }
                                //块3是key 不是数据
                                text0 = list.get(0) + "";
                                text1 = list.get(1) + "";
                                text2 = list.get(2) + "";
                            }
                            Bundle bundle = new Bundle();
                            bundle.putString("data0", text0);
                            bundle.putString("data1", text1);
                            bundle.putString("data2", text2);
//                            bundle.putString("data", text);
                            Message message = new Message();
                            message.setData(bundle);
                            message.what = 2;
                            handler.sendMessage(message);
                        }

                        @Override
                        public void callBack(String data) {
                            Bundle bundle = new Bundle();
                            bundle.putString("data", data);
                            Message message = new Message();
                            message.setData(bundle);
                            message.what = 3;
                            handler.sendMessage(message);
                        }

                        @Override
                        public void error() {
                            Bundle bundle = new Bundle();
                            bundle.putString("data", "验证卡密码失败");
                            Message message = new Message();
                            message.setData(bundle);
                            message.what = 1;
                            handler.sendMessage(message);
                        }
                    });

        }
    }

    @Override
    public void getDestory() {
        if (mSMPayKernel != null) {
            mSMPayKernel.destroyPaySDK();
        }
        cancelCheckCard();
    }



}