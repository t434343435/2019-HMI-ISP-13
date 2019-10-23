package com.swr.gauge_reader;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by t4343 on 2019/9/23.
 */

public class InternetTCPService {

    public int mState;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final int MESSAGE_INVALIDATE = 0;
    public static final int MESSAGE_IMAGE = 1;
    public static final int MESSAGE_TOAST = 2;

    private final String SERVER_HOST_IP = "192.168.43.72";  //"192.168.43.72";  // "106.54.219.89";
    private final int SERVER_HOST_PORT = 9999;

    public Handler mHandler;
    public ConnectedThread mConnectedThread;

    private InternetTCPService.OnInternetTCPServiceInteractionListener mListener;
    public InternetTCPService(final Context context){
        //这里把接受到的字符串写进去
        if (context instanceof InternetTCPService.OnInternetTCPServiceInteractionListener) {
            mListener = (InternetTCPService.OnInternetTCPServiceInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInternetTCPServiceInteractionListener");
        }
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (mListener != null) {
                    mListener.onInternetTCPServiceInteraction(msg);
                }
                return true;
            }
        });
    }

    public void interact(byte[] send){
        new Thread(new ConnectThread(send)).start();
    }


    private class ConnectThread extends Thread {
        private Socket socket;
        private byte[] mSend;

        public ConnectThread(byte[] send) {
            mSend = send;
        }

        public void run() {
            for (int i = 0; i < 20; i++) {
                if (mState == STATE_NONE) {
                    mState = STATE_CONNECTING;
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(SERVER_HOST_IP, SERVER_HOST_PORT), 2000);
                    } catch (UnknownHostException e) {
                        popMessage("Unknown host exception: " + e.toString());
                        try {
                            socket.close();
                            socket = null;
                        } catch (IOException e2) {
                            popMessage("unable to close() socket during connection failure: " + e2.getMessage() + "\n");
                            socket = null;
                        }
                    } catch (IOException e) {
                        popMessage("IO exception: " + e.toString());
                        socket = null;
                    } catch (SecurityException e) {
                        popMessage("Security exception: " + e.toString());
                        try {
                            socket.close();
                            socket = null;
                        } catch (IOException e2) {
                            popMessage("unable to close() socket during connection failure: " + e2.getMessage() + "\n");
                            socket = null;
                        }
                        socket = null;
                    }
                    if (socket != null) {
                        new Thread(new ConnectedThread(socket, mSend)).start();
                    } else {
                        mState = STATE_NONE;
                    }
                    break;
                }
                try {
                    sleep(50);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private byte[] mSend;

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
        private static final byte SC_ST_TEMPLATE = (byte)0x82;

        private final byte[] END = {0x01,0x00,0x00,0x00,(byte)0x81};
        private final byte[] END_CODE = Util.BytesConcat(HEAD, END);

        public ConnectedThread(Socket socket, byte[] send) {
            mSend = send;

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mConnectedThread = this;

            state = FIND_HEAD;
            bytes_buffer = new byte[0];
            data_buffer = new byte[0];
            data_len = 0;
            received_len = 0;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                popMessage("套接口未定义");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            write(mSend);
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    byte[] data = new byte[0];
                    bytes = mmInStream.read(buffer);
                    if(bytes > 0)
                        data = Util.BytesSub(buffer,0,bytes);
                    handleData(data);
                } catch (IOException e) {
                    popMessage("已失去连接");
                    mState = STATE_NONE;
                }

            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                popMessage("写操作出现异常");
                mState = STATE_NONE;
            }
        }

        public void cancel() {
            try {
                mState = STATE_NONE;
                mmSocket.close();
            } catch (IOException e) {
                popMessage("关闭套接字失败");
            }
        }

        public void handleTCPPack(byte state_code,byte[] pack) {
            if(state_code == SC_DATA){
                int channels = Util.Bytes2Int(Util.BytesSub(pack,0,4));
                double[] value = new double[channels];
                for(int i = 0; i < channels; i++){
                    value[i] =
                            Util.Bytes2Double(Util.BytesSub(pack,i * 8 + 4, i * 8 + 12));
                }
                Message msg = mHandler.obtainMessage(MESSAGE_INVALIDATE);
                Bundle b = new Bundle();
                b.putDoubleArray("value",value);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }else if(state_code == SC_IMAGE){
                int channels = Util.Bytes2Int(Util.BytesSub(pack,0,4));
                double[] value = new double[channels];
                int i = 0;
                for(; i < channels; i++){
                    value[i] =
                            Util.Bytes2Double(Util.BytesSub(pack,i * 8 + 4, i * 8 + 12));
                }
                byte[] image = Util.BytesSub(pack,i * 8 + 10,pack.length);
                Message msg1 = mHandler.obtainMessage(MESSAGE_IMAGE);
                msg1.obj = image;
                Bundle b = new Bundle();
                b.putDoubleArray("value",value);
                msg1.setData(b);
                mHandler.sendMessage(msg1);
            }
        }

        private synchronized boolean handleData(byte[] data) {
            data = Util.BytesConcat(bytes_buffer, data);
            // 状态机 获取帧头后，将帧传送给handle_udp_data(data)处理
            bytes_buffer = new byte[1];
            while(bytes_buffer.length != 0) {
                if (state == FIND_HEAD) {
                    int index = Util.BytesFind(data,HEAD);
                    if (index <= -1) {
                        bytes_buffer = new byte[0];
                        state = FIND_HEAD;
                    } else {
                        bytes_buffer = Util.BytesSub(data,index + HEAD.length,data.length);
                        data = Util.BytesSub(data,index + HEAD.length,data.length);
                        state = READ_LEN;
                    }
                } else if (state == READ_LEN) {
                    byte[] len_byte = Util.BytesSub(data,0,4);
                    data_len = Util.Bytes2Int(len_byte);
                    bytes_buffer = Util.BytesSub(data,4,data.length);
                    data = Util.BytesSub(data,4,data.length);
                    data_buffer = new byte[0];
                    state = READ_DATA;
                    received_len = 0;
                } else if (state == READ_DATA) {
                    received_len = received_len + data.length;
                    if (received_len < data_len) {
                        bytes_buffer = new byte[0];
                        data_buffer = Util.BytesConcat(data_buffer, data);
                        state = READ_DATA;
                    } else {
                        bytes_buffer = Util.BytesSub(data,data_len - received_len + data.length, data.length);
                        data_buffer = Util.BytesConcat(data_buffer,
                                Util.BytesSub(data,0, data_len - received_len + data.length));
                        state = FIND_HEAD;
                        handleTCPPack(data_buffer[0], Util.BytesSub(data_buffer,1,data_buffer.length));
                        cancel();
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

    public interface OnInternetTCPServiceInteractionListener {
        void onInternetTCPServiceInteraction(Message msg);
    }
}

