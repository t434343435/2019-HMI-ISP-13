package com.swr.gauge_reader;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class InternetUDPService {
    public int mState;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final int MESSAGE_INVALIDATE = 0;
    public static final int MESSAGE_IMAGE = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_SET_TO_CONNECT = 3;
    public static final int MESSAGE_SET_CONNECTING =4;
    public static final int MESSAGE_SET_CONNECTED = 5;

    private final String SERVER_HOST_IP = "192.168.43.72";  //"192.168.43.72";  // "106.54.219.89";
    private final int SERVER_HOST_PORT = 7000;
    private final byte[] AA = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x80};
    private final byte[] FIRST_BYTE = DataTransfer.BytesConcact("GgRd:".getBytes(),AA);
    public Handler mHandler;
    public InternetUDPService.ConnectedThread mConnectedThread;

    private InternetUDPService.OnInternetUDPServiceInteractionListener mListener;
    public InternetUDPService(final Context context){
        if (context instanceof InternetUDPService.OnInternetUDPServiceInteractionListener) {
            mListener = (InternetUDPService.OnInternetUDPServiceInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInternetUDPServiceInteractionListener");
        }
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (mListener != null) {
                    mListener.onInternetUDPServiceInteraction(msg);
                }
                return true;
            }
        });
    }
    public void connect(){
        // If there are paired devices, add each one to the ArrayAdapter
        try {
            DatagramSocket socket = new DatagramSocket(SERVER_HOST_PORT);
            socket.setBroadcast(true);
            new Thread(new InternetUDPService.ConnectedThread(socket)).start();
        } catch ( SocketException e) {
            e.printStackTrace();
        }
    }

    public void cancel(){
        mConnectedThread.cancel();
    }

    public class ConnectedThread extends Thread {
        private final DatagramSocket mDatagramSocket;
        DatagramPacket mDatagramPacket;

        private int state;
        private byte[] bytes_buffer;
        private byte[] data_buffer;
        private int data_len;
        private int received_len;

        private static final int FIND_HEAD = 0;
        private static final int READ_LEN = 1;
        private static final int READ_DATA = 2;

        private final byte[] HEAD = "GgRd:".getBytes();

        private static final byte SC_DATA = (byte)0x80;
        private static final byte SC_IMAGE = (byte)0x81;

        public ConnectedThread(DatagramSocket socket) {

            mDatagramSocket = socket;
            mConnectedThread = this;

            state = FIND_HEAD;
            bytes_buffer = new byte[0];
            data_buffer = new byte[0];
            data_len = 0;
            received_len = 0;
            byte data[] = new byte[1024];
            mDatagramPacket = new DatagramPacket(data, data.length);

            mState = STATE_CONNECTED;
            Message msg = mHandler.obtainMessage(MESSAGE_SET_CONNECTED);
            mHandler.sendMessage(msg);
        }

        public void run() {

            while (mState == STATE_CONNECTED) {
                popMessage("connect");
                try {
                    // Read from the InputStream
                    mDatagramSocket.receive(mDatagramPacket);
                    byte[] data = new byte[0];
                    byte[] buffer = mDatagramPacket.getData();
                    int bytes = mDatagramPacket.getLength();

                    if(bytes > 0)
                        data = DataTransfer.BytesSub(buffer,0,bytes);
                    handleData(data);
                } catch (IOException e) {
                    popMessage("已失去连接");
                    mState = STATE_NONE;
                    Message msg1 = mHandler.obtainMessage(MESSAGE_SET_TO_CONNECT);
                    mHandler.sendMessage(msg1);
                    break;
                }

            }
        }

        public void write(byte[] buffer) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket (buffer, buffer.length,
                        InetAddress.getByName(SERVER_HOST_IP), SERVER_HOST_PORT);
                mDatagramSocket.send(datagramPacket);
            } catch (IOException e) {
                popMessage("写操作出现异常");
            }
        }

        public void cancel() {
            mDatagramSocket.close();
            mState = STATE_NONE;
        }

        public void handleUDPack(byte state_code,byte[] pack) {
            switch(state_code){
                case SC_DATA:
                    int len = DataTransfer.Bytes2Int(DataTransfer.BytesSub(pack,0,4));
                    long[] time = new long[len];
                    double[] value = new double[len];
                    for(int i = 0; i < len; i++){
                        time[i] =
                                DataTransfer.Bytes2Long(DataTransfer.BytesSub(pack,i * 16 + 4, i * 16 + 12));
                        value[i] =
                                DataTransfer.Bytes2Double(DataTransfer.BytesSub(pack,i * 16 + 12, i * 16 + 20));

                    }
                    Message msg = mHandler.obtainMessage(MESSAGE_INVALIDATE);
                    Bundle b = new Bundle();
                    b.putLongArray("time",time);
                    b.putDoubleArray("value",value);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    break;
                case SC_IMAGE:
                    byte[] image = DataTransfer.BytesSub(pack,6,pack.length);
                    Message msg1 = mHandler.obtainMessage(MESSAGE_IMAGE);
                    msg1.obj = image;
                    mHandler.sendMessage(msg1);
                default:
            }
        }

        private synchronized boolean handleData(byte[] data) {
            data = DataTransfer.BytesConcact(bytes_buffer, data);
            // 状态机 获取帧头后，将帧传送给handle_udp_data(data)处理
            bytes_buffer = new byte[1];
            while(bytes_buffer.length != 0) {
                if (state == FIND_HEAD) {
                    int index = DataTransfer.BytesFind(data,HEAD);
                    if (index <= -1) {
                        bytes_buffer = new byte[0];
                        state = FIND_HEAD;
                    } else {
                        bytes_buffer = DataTransfer.BytesSub(data,index + HEAD.length,data.length);
                        data = DataTransfer.BytesSub(data,index + HEAD.length,data.length);
                        state = READ_LEN;
                    }
                } else if (state == READ_LEN) {
                    byte[] len_byte = DataTransfer.BytesSub(data,0,4);
                    for (int i = 0; i < 4; i++) {
                        data_len = (data_len << 8) + (len_byte[i]&0xFF);
                    }
                    bytes_buffer = DataTransfer.BytesSub(data,4,data.length);
                    data = DataTransfer.BytesSub(data,4,data.length);
                    data_buffer = new byte[0];
                    state = READ_DATA;
                    received_len = 0;
                } else if (state == READ_DATA) {
                    received_len = received_len + data.length;
                    if (received_len < data_len) {
                        bytes_buffer = new byte[0];
                        data_buffer = DataTransfer.BytesConcact(data_buffer, data);
                        state = READ_DATA;
                    } else {
                        bytes_buffer = DataTransfer.BytesSub(data,data_len - received_len + data.length, data.length);
                        data_buffer = DataTransfer.BytesConcact(data_buffer,
                                DataTransfer.BytesSub(data,0, data_len - received_len + data.length));
                        state = FIND_HEAD;
                        cancel();
                        handleUDPack(data_buffer[0],DataTransfer.BytesSub(data_buffer,1,data_buffer.length));
                        return true;
                    }
                }
            }
            return false;
        }
    }
    public void popMessage(String string){
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle b = new Bundle();
        b.putString("msg", string );
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    public interface OnInternetUDPServiceInteractionListener {
        void onInternetUDPServiceInteraction(Message msg);
    }
}
