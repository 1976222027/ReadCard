package com.apicloud.myReadCard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Toast;

import com.apicloud.myReadCard.utils.NfcReadHelper;
import com.uzmap.pkg.uzcore.UZResourcesIDFinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class ReadActivity extends Activity {
    int block,type;
    String keyStr ,datas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_read);
        setContentView(UZResourcesIDFinder.getResLayoutID("activity_read"));
        initData();
        keyStr = getIntent().getStringExtra("key");
        type = getIntent().getIntExtra("type",0);
        datas = getIntent().getStringExtra("data");
        block = getIntent().getIntExtra("block", 10);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.enableForegroundDispatch(this, mPendingIntent, intentFiltersArray, techListsArray);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        //关闭前台调度系统
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readAllData(intent);
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                setResult(104);
                finish();
            } else if (msg.what == 2) {
                Intent data = new Intent();
                data.putExtra("data0", msg.getData().getString("data0"));
                data.putExtra("data1", msg.getData().getString("data1"));
                data.putExtra("data2", msg.getData().getString("data2"));
                setResult(105, data);
                finish();
            }else if (msg.what == 3) {
                //写卡 开卡 销卡
                String datas = msg.getData().getString("data");
                Intent data = new Intent();
                data.putExtra("data", datas);
                setResult(106, data);
                finish();
            }
        }
    };

    /**
     * 读取全部数据
     */
    private void readAllData(Intent intent) {
        NfcReadHelper.getInstence(intent)
                .getAllData(type,datas,keyStr, block,  new NfcReadHelper.NFCCallback() {
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
                        bundle.putString("data", "读取失败");
                        Message message = new Message();
                        message.setData(bundle);
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                });
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
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ReadActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[]{ndef,};
        techListsArray = new String[][]{new String[]{NfcA.class.getName()}};
        if (null == adapter) {
            Toast.makeText(this, "不支持NFC功能", Toast.LENGTH_SHORT).show();
        } else if (!adapter.isEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            // 根据包名打开对应的设置界面
            startActivity(intent);
        }
    }
}
