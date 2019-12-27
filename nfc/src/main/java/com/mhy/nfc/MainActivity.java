package com.mhy.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends BaseNfcActivity {
    StringBuffer msgBuffer = new StringBuffer();
    IntentFilter mFilters[];
    String mTechLists[][];
    NfcAdapter mAdapter;
    //数据的接收f
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
//                Bundle bundle = msg.getData();
//                String date = bundle.getString("msg");
                Log.e("消息", msgBuffer.toString());
            }
        }
    };
    private Button mButton;
    private Button mButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = findViewById(R.id.button);
        mButton2 = findViewById(R.id.button2);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NdefActivity.class));
            }
        });
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,MifareClassicActivity.class));
            }
        });
/**初始化**/
        msgBuffer = new StringBuffer();
        mAdapter = NfcAdapter.getDefaultAdapter(this);
/*****************************读卡****************************/
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{{IsoDep.class.getName()}, {NfcA.class.getName()},};
        Log.d(" mTechLists", NfcF.class.getName() + mTechLists.length);
/****************************************/
        if (mAdapter == null) {
            Toast.makeText(this, "设备不支持NFC！", Toast.LENGTH_LONG).show();
            msgBuffer.append("\r\n").append("设备不支持NFC！");
            handler.sendEmptyMessage(0);
            return;
        } else {
            msgBuffer.append("\r\n").append("设备支持NFC！");
            handler.sendEmptyMessage(0);
        }
        if (!mAdapter.isEnabled()) {
            Toast.makeText(this, "请在系统设置中先启用NFC功能！", Toast.LENGTH_LONG).show();
            return;
        }
    }

    /**
     * 重写onNewIntent这个方法。因为刷卡后会实例化一个新的Intent
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mifareClassic = MifareClassic.get(tag);
            try {
                mifareClassic.connect();
                //获取扇区数量
                int count = mifareClassic.getSectorCount();
                Log.e("onNewIntent:", "扇区数量 ===" + count);
                 final byte[] KEY_PASSWORD =
                        {(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11};
                //用于判断时候有内容读取出来
                for (int i = 0; i < count; i++) {
                    //用 byte 密码打开此卡
                    boolean isOpen = mifareClassic.authenticateSectorWithKeyA(i, /*KEY_PASSWORD*/MifareClassic.KEY_DEFAULT);
                    if (isOpen) {
                        //获取扇区里面块的数量
                        int bCount = mifareClassic.getBlockCountInSector(i);
                        Log.e("onNewIntent:", "扇区里面块的数量 ===" + bCount);
                        //获取扇区第一个块对应芯片存储器的位置
                        //存储器的位置为第一扇区为0后面叠加4直到60为止
                        int bIndex = mifareClassic.sectorToBlock(i);
                        for (int j = 0; j < bCount; j++) {
                            Log.e("onNewIntent:", "存储器的位置 ===" + bIndex + "当前块 === " + (bIndex + j));
                            byte[] data = mifareClassic.readBlock(bIndex + j);//进行了读卡
                            msgBuffer.append("块" + (bIndex + j) + "数据:").append(byteHexToSting(data)).append("\r\n");
                            handler.sendEmptyMessage(0);
                            Log.e("数据", "第" + (bIndex + j) + "块" + byteHexToSting(data));
                            //修改KeyA和KeyB
//                            if ((bIndex + j) == (4 * i + 3)) {//第 3 7 11 15.。。。是每扇区的最后一块 存放key和控制符
//                                //将所有扇区的最后一个Block修改为111111111111ff078069111111111111
//                                mifareClassic.writeBlock(bIndex + j, new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11});
//                                Log.e("onNewIntent:", (bIndex + j) + "块加密成功");
//                            }
                        }
                    } else {
                        msgBuffer.append("第" + (i + 1) + "扇区" + "验证新卡密码失败\r\n");
                        handler.sendEmptyMessage(0);
                        Log.e("失败 ", "验证密码");

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //开启前台调度系统
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        //关闭前台调度系统
        mAdapter.disableForegroundDispatch(this);
    }

    /**
     * convert byte[] to HexString
     *
     * @param bArray
     * @return
     */

    public static String byteHexToSting(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

}



