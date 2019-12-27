package com.apicloud.myReadCard.utils;

import android.nfc.tech.MifareClassic;
import android.text.TextUtils;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//字符串转hex byte
public class ByteUtil {
   public static byte[]bytes0= new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte)0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    //控制位7f078869 KeyA/B 均可读写块012  仅KeyB 能写块3均不可读
   public static byte[] commdB =new byte[]{(byte) 0x7f, 0x07, (byte) 0x88, (byte) 0x69};//hexStr2Bytes("7f078869");
    public static byte[] baseKey=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte) 0xff, 0x07, (byte) 0x80, (byte) 0x69,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};//hexStr2Bytes("ffffffffffffff078069ffffffffffff")
    public static String baseKeyB ="ffffffffffff";
    public static String commdKeyB ="7f078869";

    public static String bytes2HexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String temp;
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制

            temp = Integer.toHexString(0xFF & b);
            if (temp.length() == 1) {
                // 每个字节8位，转为16进制标志，2个16进制位
                sb.append("0");
            }
            sb.append(temp);
        }
        return sb.toString().toUpperCase();
    }

    //convert byte[] to HexString
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
    public static String bytes2HexStr_2(byte[] bytes) {
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    public static String byte2HexStr(byte b) {
        String temp = Integer.toHexString(0xFF & b);
        if (temp.length() == 1) {
            // 每个字节8为，转为16进制标志，2个16进制位
            temp = "0" + temp;
        }
        return temp;
    }

    //2位==1字符 ffff->0xff,0xff
    public static byte[] hexStr2Bytes(String hexStr) {
        if (TextUtils.isEmpty(hexStr)){
            return new byte[16];//字节
        }
        hexStr = hexStr.toLowerCase();
        int length = hexStr.length();
        LogUtil.e("字符长度", length + "");
        byte[] bytes = new byte[length >> 1];
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (index > hexStr.length() - 1) return bytes;
            byte highDit = (byte) (Character.digit(hexStr.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexStr.charAt(index + 1), 16) & 0xFF);
            bytes[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return bytes;
    }

    //str>hex byte
    public static byte hexStr2Byte(String hexStr) {
        return (byte) Integer.parseInt(hexStr, 16);
    }

    /*输入10进制数字字符串，输出hex字符串（2位,eg: f 则输出 0f）*/
    // 100->010000
    public String getHexString(String value) {
        int parseInt = Integer.parseInt(value, 10);
        String hexString = Integer.toHexString(parseInt);
        if (hexString.length() < 2) {
            hexString = '0' + hexString;
        }
        return hexString;
    }

    /**
     * 汉子或10进制数字 字符串 转换16进制byte[]
     */
    public static byte[] String2Byte(String data) {
//            byte[] bd = data.getBytes(Charset.forName("UTF-8"));
//            for (byte b : bd) {
//                System.out.println(b);
//            }
//            byte[] bdata = {-26, -83, -93};
//            System.out.println(" utf-8 :  " + new String(bdata));
        //utf-8 一字3位 GBK一字2位
        String hex = "";
        byte[] gbd = data.getBytes(Charset.forName("GBK"));
        for (byte b : gbd) {
//                System.out.println(b);//10进制
            hex += byteToHex(b);//转16进制
        }
        LogUtil.e("hex源"+hex.length(),hex);//一定是双数
        for (int i = hex.length(); i < 32; i += 2) {
            hex += "20";//20 是空 补充占位
        }

        if (hex.length() > 32) {
            hex = hex.substring(0, 32);
        }
//        hex32位  [16]byte
        return hexStr2Bytes(hex);
    }
    //hex byte ->GBK字符
    public static String Byte2String(byte[] gdata) {
//            byte[] gdata = {-43, -3};
//            System.out.println(" GBK :  " + new String(gdata, Charset.forName("GBK")));
//        byte[] gdata= hexStr2Bytes(hexString);
//       if (byteHexToSting(gdata).equals("00000000000000000000000000000000")){
//           gdata = String2Byte("00000000000000000000000000000000");
//        }
        return new String(gdata, Charset.forName("GBK"));
    }

    /**
     * 字节转十六进制
     *
     * @param b 需要进行转换的byte字节
     * @return 转换后的Hex字符串
     */
    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex;
    }

    // 使用两个 for 语句
//java 合并两个byte数组
    public static byte[] byteMerger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        int i = 0;
        for (byte bt : bt1) {
            bt3[i] = bt;
            i++;
        }
        for (byte bt : bt2) {
            bt3[i] = bt;
            i++;
        }
        return bt3;
    }

    public static byte[] byteMerger(byte[] bt1, byte[] bt2, byte[] bt3) {
        byte[] bt4 = new byte[bt1.length + bt2.length + bt3.length];
        int i = 0;
        for (byte bt : bt1) {
            bt4[i] = bt;
            i++;
        }
        for (byte bt : bt2) {
            bt4[i] = bt;
            i++;
        }
        for (byte bt : bt3) {
            bt4[i] = bt;
            i++;
        }
        return bt4;
    }
//hexStr转10进制字符 2位1字符
    public static String hexStr2Str(String hexStr) {
        String vi = "0123456789ABC DEF".trim();
        char[] array = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int temp;
        for (int i = 0; i < bytes.length; i++) {
            char c = array[2 * i];
            temp = vi.indexOf(c) * 16;
            c = array[2 * i + 1];
            temp += vi.indexOf(c);
            bytes[i] = (byte) (temp & 0xFF);
        }
        return new String(bytes);
    }

    public static String hexStr2AsciiStr(String hexStr) {
        String vi = "0123456789ABC DEF".trim();
        hexStr = hexStr.trim().replace(" ", "").toUpperCase(Locale.US);
        char[] array = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int temp = 0x00;
        for (int i = 0; i < bytes.length; i++) {
            char c = array[2 * i];
            temp = vi.indexOf(c) << 4;
            c = array[2 * i + 1];
            temp |= vi.indexOf(c);
            bytes[i] = (byte) (temp & 0xFF);
        }
        return new String(bytes);
    }

    /**
     * 将无符号short转换成int，大端模式(高位在前)
     */
    public static int unsignedShort2IntBE(byte[] src, int offset) {
        return (src[offset] & 0xff) << 8 | (src[offset + 1] & 0xff);
    }

    /**
     * 将无符号short转换成int，小端模式(低位在前)
     */
    public static int unsignedShort2IntLE(byte[] src, int offset) {
        return (src[offset] & 0xff) | (src[offset + 1] & 0xff) << 8;
    }

    /**
     * 将无符号byte转换成int
     */
    public static int unsignedByte2Int(byte[] src, int offset) {
        return src[offset] & 0xFF;
    }

    /**
     * 将字节数组转换成int,大端模式(高位在前)
     */
    public static int unsignedInt2IntBE(byte[] src, int offset) {
        int result = 0;
        for (int i = offset; i < offset + 4; i++) {
            result |= (src[i] & 0xff) << (offset + 3 - i) * 8;
        }
        return result;
    }

    /**
     * 将字节数组转换成int,小端模式(低位在前)
     */
    public static int unsignedInt2IntLE(byte[] src, int offset) {
        int value = 0;
        for (int i = offset; i < offset + 4; i++) {
            value |= (src[i] & 0xff) << (i - offset) * 8;
        }
        return value;
    }

    /**
     * 将int转换成byte数组，大端模式(高位在前)
     */
    public static byte[] int2BytesBE(int src) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (src >> (3 - i) * 8);
        }
        return result;
    }

    /**
     * 将int转换成byte数组，小端模式(低位在前)
     */
    public static byte[] int2BytesLE(int src) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (src >> i * 8);
        }
        return result;
    }

    /**
     * 将short转换成byte数组，大端模式(高位在前)
     */
    public static byte[] short2BytesBE(short src) {
        byte[] result = new byte[2];
        for (int i = 0; i < 2; i++) {
            result[i] = (byte) (src >> (1 - i) * 8);
        }
        return result;
    }

    /**
     * 将short转换成byte数组，小端模式(低位在前)
     */
    public static byte[] short2BytesLE(short src) {
        byte[] result = new byte[2];
        for (int i = 0; i < 2; i++) {
            result[i] = (byte) (src >> i * 8);
        }
        return result;
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    public static byte[] concatByteArrays(byte[]... list) {
        if (list == null || list.length == 0) {
            return new byte[0];
        }
        return concatByteArrays(Arrays.asList(list));
    }

    /**
     * 将字节数组列表合并成单个字节数组
     */
    public static byte[] concatByteArrays(List<byte[]> list) {
        if (list == null || list.isEmpty()) {
            return new byte[0];
        }
        int totalLen = 0;
        for (byte[] b : list) {
            if (b == null || b.length == 0) {
                continue;
            }
            totalLen += b.length;
        }
        byte[] result = new byte[totalLen];
        int index = 0;
        for (byte[] b : list) {
            if (b == null || b.length == 0) {
                continue;
            }
            System.arraycopy(b, 0, result, index, b.length);
            index += b.length;
        }
        return result;
    }

    /**
     * 时间戳转换成日期格式字符串
     * @param seconds 精确到秒的字符串
     * @return
     */
    public static String timeStamp2Date(String seconds) {
        if (TextUtils.isEmpty(seconds)){
            return seconds;
        }
        if(seconds == null || seconds.isEmpty() || seconds.equals("null")){
            return "";
        }
           String format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds/*+"000"*/)));
    }
}
