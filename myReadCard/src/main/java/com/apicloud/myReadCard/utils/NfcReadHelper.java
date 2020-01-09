package com.apicloud.myReadCard.utils;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zx
 * @date 2018/4/23 14:31
 * email 1058083107@qq.com
 * description nfc读取工具类
 */
public class NfcReadHelper {
    private Tag tag;
    private NFCCallback callback;
    private static NfcReadHelper helper;
    /**
     * 默认密码
     */
    private byte[] bytes = MifareClassic.KEY_DEFAULT;//{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    public NfcReadHelper(Intent intent) {
        this.tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        LogUtil.e("wang", "进入构造函数");
    }

    /**
     * 单例初始化
     *
     * @param intent
     * @return
     */
    public static NfcReadHelper getInstence(Intent intent) {
        if (helper == null) {
            helper = new NfcReadHelper(intent);
        }
        return helper;
    }
    //************************************/
    public static NfcReadHelper getInstence(Tag intent) {
        if (helper == null) {
            helper = new NfcReadHelper(intent);
        }
        return helper;
    }
    public NfcReadHelper(Tag intent) {
        this.tag = intent;
        LogUtil.e("wang", "进入构造函数");
    }
    //************************************/
    /**
     * 设置NFC卡的密码
     *
     * @param str
     * @return
     */
    public NfcReadHelper setPassword(String str) {
        if (null != str && (str.length() <= 6)) {
            for (int i = 0; i < str.length(); i++) {
                bytes[i] = (byte) str.charAt(i);
            }
        }
        return helper;
    }

    /**
     * 读取NFC卡的全部信息
     * @param type     0读卡  1写卡 2开卡 3销卡
     * @param datas    写卡数据
     * @param passWord
     * @param sector
     * @param callback
     */
    public void getAllData(final int type, final String datas, final String passWord, final int sector,final NFCCallback callback) {
        final byte[] keyByte = ByteUtil.hexStr2Bytes(passWord);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> map = new HashMap<>();
                MifareClassic mfc = MifareClassic.get(tag);
                if (null != mfc) {
                    try {
                        //链接NFC
                        mfc.connect();
                        //获取扇区数量
                        int count = mfc.getSectorCount();
                        LogUtil.e("读卡" + type, "扇区数量 ===" + count);
                        //用于判断时候有内容读取出来
                        boolean flag = false;
                        int i = sector;//扇区
                        List<String> list = new ArrayList<>();
                        //验证扇区密码，否则会报错（链接失败错误）
                        boolean isOpen = false;
                        String result = "";
                        if (type == 2){//开卡用默认keyB验证
                            isOpen = mfc.authenticateSectorWithKeyB(i, MifareClassic.KEY_DEFAULT);
                        }else if (type==3) {//开卡销卡用B验证
                            // 销卡用keystr keyB验证
                            isOpen = mfc.authenticateSectorWithKeyB(i, keyByte);
                        } else {//读卡 写卡 不涉及块3 用A
                            //通过密码验证通过》
                                //keyA校验扇区
                            isOpen = mfc.authenticateSectorWithKeyA(i, keyByte);
                        }

                        if (isOpen) {
                            //获取扇区里面块的数量
                            int bCount = mfc.getBlockCountInSector(i);
                            LogUtil.e("读卡", "扇区里面块的数量" + bCount);
                            //获取扇区第一个块对应芯片存储器的位置
                            //存储器的位置为第一扇区为0后面叠加4直到60为止
                            int bIndex = mfc.sectorToBlock(i);//扇区0-63 索引=0-15扇区X4

                            if (type == 0) { //读卡
                                for (int j = 0; j < bCount; j++) {//块
                                    LogUtil.e("读卡:", j + "当前块" + (bIndex + j));
                                    byte[] data = mfc.readBlock(bIndex + j);//对块进行了读卡
                                    String hexStr = ByteUtil.bytes2HexStr(data);
                                    LogUtil.e("第" + (bIndex + j) + "块hexStr", hexStr + "-" +ByteUtil.hexStr2Str(hexStr));
                                    LogUtil.e("第" + (bIndex + j) + "块GBK数据",  ByteUtil.Byte2String(data));/*+ByteUtil.Byte2String(data).substring(0, 11)*/
                                    list.add(ByteUtil.Byte2String(data)/*byteToString(data)*//*ByteUtil.hexStr2Str(hexStr)*/);
                                }
                                map.put(i + "", list);
                                //先读后写
                                //记录刷卡时间
                                mfc.writeBlock(bIndex + 1, ByteUtil.String2Byte(System.currentTimeMillis()+""));
                            }
                            if (type == 1) { //写数据
                                //指定扇区的块
                                int blockIndex = 0;//此扇区的块1   0123
                                byte[] datt = ByteUtil.String2Byte(datas);//new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11};
                                LogUtil.e("写数据", ByteUtil.bytes2HexStr(datt));
//                                //将所有扇区的最后一个Block修改为111111111111ff078069111111111111
                                mfc.writeBlock(blockIndex + bIndex, datt);
                                LogUtil.e("写卡:", (bIndex + blockIndex) + "写数据成功");
                                result = "写卡成功:" + datas/*+"/"+ByteUtil.bytes2HexStr(datt)*/;
                            }
                            if (type == 2) {//开卡 将此扇区4块都改密
                                //修改KeyA和KeyB
                                //每扇区的最后一块Block 存放key和控制符修改为111111111111ff078069111111111111
                                //记录开卡时间
                                mfc.writeBlock(bIndex + 2, ByteUtil.String2Byte(System.currentTimeMillis()+""));
                                //new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11});
                                mfc.writeBlock(bIndex + 3, ByteUtil.hexStr2Bytes(passWord+ByteUtil.commdKeyB+passWord));
                                 /*new byte[]{(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11}*/
                                //new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11});
                                LogUtil.e("开卡:", (bIndex + 3) + "开卡块加密成功");
                                result = "开卡成功";
                            }
                            if (type == 3) {//销卡 将此扇区4块
                                mfc.writeBlock(bIndex + 0, ByteUtil.bytes0);
                                mfc.writeBlock(bIndex + 1, ByteUtil.bytes0);
                                mfc.writeBlock(bIndex + 2, ByteUtil.bytes0);
                                mfc.writeBlock(bIndex + 3, ByteUtil.baseKey);
                                LogUtil.e("销卡:", (bIndex + 3) + "销卡块加密成功");
                                result = "销卡成功";
                            }
                            flag = true;
                        } else {
                            LogUtil.e("失败 ", "第" + (i + 1) + "扇区" + "验证卡密码失败\r\n");
                        }
                        if (flag) {
                            if (type == 0) {//读卡
                                callback.callBack(map);
                            }
                            if (type == 1 || type == 2 || type == 3) {
                                callback.callBack(result);
                            }
                        } else {
                            callback.error();
                        }
                    } catch (Exception e) {
                        callback.error();
                        e.printStackTrace();
                    } finally {
                        try {
                            mfc.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 添加数据可
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param callback
     */
    public void getData(final int a, final int b, int c, int d, int e, final NFCCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> map = new HashMap<>();
                MifareClassic mfc = MifareClassic.get(tag);
                if (null != null) {
                    try {
                        mfc.connect();
                        int count = mfc.getSectorCount();
                        if (a < 0 || a > count - 1) {
                            callback.error();
                            return;
                        }
                        int bCount = mfc.getBlockCountInSector(a);
                        if (b < 0 || b > bCount - 1) {
                            callback.error();
                            return;
                        }
                        boolean isOpen = mfc.authenticateSectorWithKeyA(a, bytes);
                        if (isOpen) {
                            int bIndex = mfc.sectorToBlock(a);
                            byte[] data = mfc.readBlock(bIndex + b);
                            callback.callBack(byteToString(data));//十六进制转化为字符串
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 读取NFC卡的特定扇区信息
     *
     * @param a        扇区
     * @param b        块
     * @param callback
     */
    public void getData(final int a, final int b, final NFCCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> map = new HashMap<>();
                MifareClassic mfc = MifareClassic.get(tag);
                if (null != mfc) {
                    try {
                        mfc.connect();
                        int count = mfc.getSectorCount();
                        if (a < 0 || a > count - 1) {
                            callback.error();
                            return;
                        }
                        int bCount = mfc.getBlockCountInSector(a);
                        if (b < 0 || b > bCount - 1) {
                            callback.error();
                            return;
                        }
                        boolean isOpen = mfc.authenticateSectorWithKeyA(a, bytes);
                        if (isOpen) {
                            int bIndex = mfc.sectorToBlock(a);
                            byte[] data = mfc.readBlock(bIndex + b);
                            callback.callBack(byteToString(data));
                        } else {
                            callback.error();
                        }
                    } catch (Exception e) {
                        callback.error();
                        e.printStackTrace();
                    } finally {
                        try {
                            mfc.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 返回监听类
     */
    public interface NFCCallback {
        /**
         * 返回读取nfc卡的全部信息
         *
         * @param data 前面代表扇区 四个块的数据用#号隔开
         */
        void callBack(Map<String, List<String>> data);

        void callBack(String data);

        void error();
    }

    /**
     * 将byte数组转化为字符串
     *
     * @param src
     * @return hexStr
     */
    public static String byteToString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
}
