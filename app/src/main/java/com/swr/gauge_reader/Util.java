package com.swr.gauge_reader;

import android.graphics.RectF;

public class Util {
    public static byte[] Double2Bytes(double data){
        long value = Double.doubleToRawLongBits(data);
        byte[] byteRet = new byte[8];
        for (int i = 0; i < 8; i++) {
            byteRet[i] = (byte) ((value >> 8 * i) & 0xff);
        }
        return byteRet;
    }
    public static byte[] Int2Bytes(int data){
        byte[] byteRet = new byte[4];
        for (int i = 0; i < 4; i++) {
            byteRet[i] = (byte) ((data >> 8 * i) & 0xff);
        }
        return byteRet;
    }
    public static byte[] Long2Bytes(long data){
        byte[] byteRet = new byte[4];
        for (int i = 0; i < 4; i++) {
            byteRet[i] = (byte) ((data >> 8 * i) & 0xff);
        }
        return byteRet;
    }
    public static double Bytes2Double(byte[] bytes){
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res |= ((long) (bytes[i] & 0xff)) << (8 * i);
        }
        return Double.longBitsToDouble(res);
    }
    public static int Bytes2Int(byte[] bytes){
        int res = 0;
        for (int i = 0; i < 4; i++) {
            res = (res << 8) + (bytes[i]&0xFF);
        }
        return res;
    }
    public static long Bytes2Long(byte[] bytes){
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res |= ((long) (bytes[i] & 0xff)) << (8 * i);
        }
        return res;
    }

    public static byte[] BytesConcact(byte[] bytes1, byte[] bytes2){
        byte[] res = new byte[bytes1.length + bytes2.length];
        for(int i = 0; i < bytes1.length; i++)
            res[i] = bytes1[i];
        for(int i = bytes1.length; i < res.length; i++)
            res[i] = bytes2[i - bytes1.length];
        return res;
    }

    public static int BytesFind(byte[] bytes, byte[] bytestofind){
        for (int i = 0; i < bytes.length; i++) {
            int j = 0;
            while(bytes[i] == bytestofind[j]){
                i++;
                j++;
                if(i >= bytes.length || j >= bytestofind.length)break;
            }
            if(j == bytestofind.length){
                return i - j;
            }
        }
        return -1;
    }

    public static byte[] BytesSub(byte[] bytes, int start_index, int end_index){
        byte[] res = new byte[end_index - start_index];
        for(int i = 0; i < res.length; i++)
            res[i] = bytes[i + start_index];
        return  res;
    }

}
