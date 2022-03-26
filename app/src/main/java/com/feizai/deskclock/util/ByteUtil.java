package com.feizai.deskclock.util;

/**
 * Author: chenhao
 * Date: 2022/2/11-0011 下午 02:13:04
 * Describe:
 */
public class ByteUtil {

    /**
     * +
     * byte转二进制字符串
     *
     * @param b
     * @return
     */
    public static String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
    }

    public static byte bitToByte(String bitStr){
        byte result=0;
        for(int i=bitStr.length()-1,j=0;i>=0;i--,j++){
            result+=(Byte.parseByte(bitStr.charAt(i)+"")*Math.pow(2, j));
        }
        return result;
    }

    public static byte intToByte(int value) {
        return (byte) (value & 0XFF);
    }
}
