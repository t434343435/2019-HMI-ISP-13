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
            res |= ((int) (bytes[i] & 0xff)) << (8 * i);
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

    public static byte[] BytesConcat(byte[] bytes1, byte[] bytes2){
        byte[] res = new byte[bytes1.length + bytes2.length];
        for(int i = 0; i < bytes1.length; i++)
            res[i] = bytes1[i];
        for(int i = bytes1.length; i < res.length; i++)
            res[i] = bytes2[i - bytes1.length];
        return res;
    }

    public static double[] DoublesConcat(double[] doubles1, double doubles2){
        double[] res = new double[doubles1.length + 1];
        for(int i = 0; i < doubles1.length; i++)
            res[i] = doubles1[i];
        res[doubles1.length] = doubles2;
        return res;
    }

    public static long[] LongsConcat(long[] longs1, long longs2){
        long[] res = new long[longs1.length + 1];
        for(int i = 0; i < longs1.length; i++)
            res[i] = longs1[i];
        res[longs1.length] = longs2;
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
