package com.apicloud.myReadCard.pos;

import android.app.Activity;
import android.content.Intent;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.apicloud.myReadCard.Constant;
import com.apicloud.hanchao.MyApplication;
import com.apicloud.myReadCard.R;
import com.apicloud.myReadCard.utils.ByteUtil;
import com.apicloud.myReadCard.utils.LogUtil;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;

import java.util.Arrays;

import sunmi.paylib.SunmiPayKernel;

/**
 * POS读卡
 */
public class M1Activity extends Activity {
    private SunmiPayKernel mSMPayKernel;
    private boolean isDisConnectService = true;//未连接
    private int type;
    private int sector;//扇区
    private int keyType;    // 密钥类型，0表示KEY A、1表示 KEY B
    private byte[] keyBytes;
    String keyStr, data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m1);
        keyStr = getIntent().getStringExtra("key");
        data = getIntent().getStringExtra("data");
        type = getIntent().getIntExtra("type", 0);//0刷卡 1写卡 2开卡 3销卡
        sector = getIntent().getIntExtra("block", 10);
        connectPayService();
//        mHintDialog = new SwingCardHintDialog(this);
//        mHintDialog.setOwnerActivity(this);
        checkCard();
    }



    private void connectPayService() {
        mSMPayKernel = SunmiPayKernel.getInstance();
        mSMPayKernel.initPaySDK(this, mConnectCallback);
    }

    private void checkCard() {
        try {
//            showHintDialog();
            MyApplication.mReadCardOptV2.checkCard(AidlConstantsV2.CardType.MIFARE.getValue(), mCheckCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            readM1();
        }

        @Override
        public void onError(int code, String message) throws RemoteException {
            checkCard();
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    @Override
    protected void onPause() {
        overridePendingTransition(0, 0);
        super.onPause();
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
                keyType=1;//KeyB验证
                result = MyApplication.mReadCardOptV2.mifareAuth(keyType, startBlockNo, MifareClassic.KEY_DEFAULT) == 0;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if(type == 3) {
            keyType=1;//KeyB验证
            result = m1Auth(keyType, startBlockNo, keyBytes);
        }else {
            keyType=0;//KeyA验证  读卡写卡
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
                m1WriteBlock(startBlockNo + 1,  ByteUtil.String2Byte(System.currentTimeMillis()+""));//字节
                Intent data = new Intent();
                data.putExtra("data0", s0);
                data.putExtra("data1", s1);
                data.putExtra("data2", s2);
                setResult(601, data);
            } else if (type == 1) {//写卡
                outData = ByteUtil.String2Byte(data)/*new byte[32]*/;
                res = m1WriteBlock(startBlockNo, outData);//块0 123
                Log.e("多少字节", res + "=" + res * 2 + "位");
                if (res >= 0 && res <= 16) {
                    //读数据
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "0read outData:" + hexStr);
                    s0 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "0outData:" + s0);
                }
                Intent idata = new Intent();
                idata.putExtra("data", "写卡成功:"+data);
                setResult(603, idata);
            } else if (type == 2) {//开卡 将块3 key换掉
                outData = ByteUtil.byteMerger(ByteUtil.hexStr2Bytes(keyStr),ByteUtil.commdB,ByteUtil.hexStr2Bytes(keyStr)/*new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69,(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11}*/);
                res = m1WriteBlock(startBlockNo + 3, outData);//字节
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "3开卡:" + hexStr);
                    s3 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "3开卡:" + s3);
                }
//
                m1WriteBlock(startBlockNo + 2,  ByteUtil.String2Byte(System.currentTimeMillis()+""));//字节
                Intent idata = new Intent();
                idata.putExtra("data", "开卡成功");
                setResult(603, idata);
            } else if (type == 3) {//销卡 key还原
//                outData = ByteUtil.byteMerger(MifareClassic.KEY_DEFAULT, new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69}, MifareClassic.KEY_DEFAULT);
                outData = ByteUtil.baseKey;
                m1WriteBlock(startBlockNo, ByteUtil.bytes0);//字节
                m1WriteBlock(startBlockNo + 1,  ByteUtil.bytes0);//字节
                m1WriteBlock(startBlockNo + 2,  ByteUtil.bytes0);//字节
                res = m1WriteBlock(startBlockNo + 3, outData);//字节
                if (res >= 0 && res <= 16) {
                    String hexStr = ByteUtil.bytes2HexStr(Arrays.copyOf(outData, res));
                    LogUtil.e(Constant.TAG, "3销卡:" + hexStr);
                    s3 = ByteUtil.hexStr2Str(hexStr);
                    LogUtil.e(Constant.TAG, "3销卡:" + s3);
                }
                Intent idata = new Intent();
                idata.putExtra("data", "销卡成功");
                setResult(603, idata);
            }
            finish();
            overridePendingTransition(0, 0);
        } else {
            setResult(602);
            finish();
            overridePendingTransition(0, 0);
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
     *
     * @param block
     * @param blockData
     * @return
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
//            showToast(R.string.card_auth_fail);
            checkCard();
            return false;
        }
    }

    public void showToast(int redId) {
        showToastOnUI(getString(redId));
    }

    private void showToastOnUI(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(M1Activity.this, "" + msg, Toast.LENGTH_SHORT).show();
            }
        });

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnectPaySDK() {
            LogUtil.e(Constant.TAG, "onDisconnectPaySDK");
            isDisConnectService = true;
//            showToast(R.string.connect_fail);
        }

    };
}
