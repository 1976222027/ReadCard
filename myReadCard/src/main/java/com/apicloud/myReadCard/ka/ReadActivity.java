package com.apicloud.myReadCard.ka;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.apicloud.myReadCard.Constant;
import com.apicloud.myReadCard.utils.ByteUtil;
import com.apicloud.myReadCard.utils.LogUtil;
import com.uzmap.pkg.uzcore.UZResourcesIDFinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 读卡   add 写卡
 */
public class ReadActivity extends Activity {
    int block;
    int flag, type;
    String keyStr;
    String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_read);
        setContentView(UZResourcesIDFinder.getResLayoutID("activity_read"));

        data = getIntent().getStringExtra("data");
        type = getIntent().getIntExtra("type", 0);
        keyStr = getIntent().getStringExtra("key");
        flag = getIntent().getIntExtra("flag", 0);
        block = getIntent().getIntExtra("block", 10);//扇区
        initData();
    }

    @Override
    protected void onResume() {
        //开启前台调度系统
        super.onResume();
        if (adapter != null) {
            adapter.enableForegroundDispatch(this, mPendingIntent, intentFiltersArray, techListsArray);
        }
        overridePendingTransition(0,0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readAllData(type,keyStr, block, flag, intent);
//        String action = intent.getAction();
//        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)|| NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
//            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//            MifareClassic mifareClassic = MifareClassic.get(tag);
//            try {
//                mifareClassic.connect();
//                //获取扇区数量
//                int count = mifareClassic.getSectorCount();
//                Log.e("onNewIntent:" + type, "扇区数量 ===" + count);
//                final byte[] KEY_PASSWORD = ByteUtil.hexStr2Bytes(keyStr);
//                Log.e("mhykey", ByteUtil.byteHexToSting(KEY_PASSWORD));
//                // {(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11,(byte)0x11};
//                //用于判断时候有内容读取出来
//
//                for (int i = 0; i < count; i++) {//遍历扇区
//                    boolean isOpen = false;
//                    if (block == i) {//add 全部在指定扇区内进行
//                        //用 byte 密码打开此卡
//                        if (type == 2) {
//                            //开卡用默认key进入
//                            isOpen = mifareClassic.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT);
//                        } else {
//                            if (flag == 0) {
//                                //key校验扇区
//                                isOpen = mifareClassic.authenticateSectorWithKeyA(i, KEY_PASSWORD);
//                            } else if (flag == 1) {
//                                isOpen = mifareClassic.authenticateSectorWithKeyB(i, KEY_PASSWORD);
//                            }
//                        }
//                        //  boolean isOpen = mifareClassic.authenticateSectorWithKeyA(i, KEY_PASSWORD/*MifareClassic.KEY_DEFAULT*/);
//                        //读卡
//                        if (isOpen) {
//                            //获取扇区里面块的数量
//                            int bCount = mifareClassic.getBlockCountInSector(i);
//                            Log.e("onNewIntent:", "扇区里面块的数量 ===" + bCount);
//                            //获取扇区第一个块对应芯片存储器的位置
//                            //存储器的位置为第一扇区为0后面叠加4直到60为止
//                            int bIndex = mifareClassic.sectorToBlock(i);
//                            for (int j = 0; j < bCount; j++) {//块
//                                Log.e("onNewIntent:", "存储器的位置 ===" + bIndex + "当前块 === " + (bIndex + j));
//                                byte[] data = mifareClassic.readBlock(bIndex + j);//对块进行了读卡
//                                Log.e("第" + (bIndex + j) + "块hexStr",  ByteUtil.byteHexToSting(data));
////                                if ( j==1) {
//                                    Log.e("第" + (bIndex + j) + "块GBK数据"/*+ByteUtil.Byte2String(data).substring(0, 11)*/,  ByteUtil.Byte2String(data));
////                                }
//                                if (type == 2) {//开卡 将此扇区4块都改密
//                                    //修改KeyA和KeyB
//                                    if ((bIndex + j) == (4 * i + 3)) {//第 3 7 11 15.。。。是每扇区的最后一块 存放key和控制符
//                                        //将所有扇区的最后一个Block修改为111111111111ff078069111111111111
//                                        mifareClassic.writeBlock(bIndex + j, ByteUtil.byteMerger(ByteUtil.hexStr2Bytes(keyStr), new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11}));//new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11});
//                                        Log.e("onNewIntent:", (bIndex + j) + "块加密成功");
//                                    }
//                                }  if (type == 3) {//销卡 将此扇区4块都改密
//                                    //修改KeyA和KeyB
//                                    if ((bIndex + j) == (4 * i + 3)) {//第 3 7 11 15.。。。是每扇区的最后一块 存放key和控制符
//                                        //将所有扇区的最后一个Block修改为111111111111ff078069111111111111
//                                        mifareClassic.writeBlock(bIndex + j, ByteUtil.byteMerger(MifareClassic.KEY_DEFAULT, new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69}, MifareClassic.KEY_DEFAULT));//new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11});
//                                        Log.e("onNewIntent:", (bIndex + j) + "块还原成功");
//                                    }
//                                }
//                            }
//                            if (type == 1) { //写数据
//                                //指定扇区的块
//                                int blockIndex = 1;//此扇区的块1   0123
//                                byte[] datt = ByteUtil.String2Byte(data);//new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11};
//                                Log.e("写数据", ByteUtil.byteHexToSting(datt));
////                                //将所有扇区的最后一个Block修改为111111111111ff078069111111111111
//                                mifareClassic.writeBlock(blockIndex + bIndex, datt);
//                                Log.e("onNewIntent:", (bIndex + blockIndex) + "写数据成功");
//                            }
//                        } else {
//                            Log.e("失败 ", "第" + (i + 1) + "扇区" + "验证新卡密码失败\r\n");
//                        }
//
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    mifareClassic.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //关闭前台调度系统
        adapter.disableForegroundDispatch(this);
        overridePendingTransition(0,0);
    }

    /**
     * 吐司
     *
     * @param str
     */
    private void toToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                setResult(104);
                finish(); overridePendingTransition(0,0);
            } else if (msg.what == 2) {
                //读取数据ok
                Intent data = new Intent();
                data.putExtra("data0", msg.getData().getString("data0"));
                data.putExtra("data1", msg.getData().getString("data1"));
                data.putExtra("data2", msg.getData().getString("data2"));
                setResult(105, data);
                finish(); overridePendingTransition(0,0);
            } else if (msg.what == 3) {
                Intent data = new Intent();
                data.putExtra("data", msg.getData().getString("data"));
                setResult(106,data);//写卡 开卡 销卡
                finish(); overridePendingTransition(0,0);
            }
        }
    };

    /**
     * 读取全部数据
     */
    private void readAllData(int type, String keyAStr, int block, int flag, Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //读取卡全部数据
            NfcReadHelper.getInstence(intent)
                    .getAllData(type, data, keyAStr, block, new NfcReadHelper.NFCCallback() {
                        @Override
                        public void callBack(Map<String, List<String>> data) {
                            String text = "";
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
                            bundle.putString("data", text);
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

    /**
     * 初始化数据
     */
    private PendingIntent mPendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;
    NfcAdapter adapter;

    private void initData() {
        adapter = NfcAdapter.getDefaultAdapter(this);
        //读卡
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ReadActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED/*ACTION_TAG_DISCOVERED*/);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};
        techListsArray = new String[][]{new String[]{NfcA.class.getName()}};
        Log.d(" mTechLists", NfcF.class.getName() + techListsArray.length);
        if (null == adapter) {
            Toast.makeText(this, "不支持NFC功能", Toast.LENGTH_SHORT).show();
        } else if (!adapter.isEnabled()) {
            //打开NFC
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            // 根据包名打开对应的设置界面
            startActivity(intent);
        }
    }
}
