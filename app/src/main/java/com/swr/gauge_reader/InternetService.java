package com.swr.gauge_reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by t4343 on 2019/9/23.
 */

public class InternetService {

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

    private final String SERVER_HOST_IP = "106.54.219.89";  //"192.168.43.72";  // "106.54.219.89";
    private final int SERVER_HOST_PORT = 9999;
    private final byte[] AA = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x80};
    private final byte[] FIRST_BYTE = DataTransfer.BytesConcact("GgRd:".getBytes(),AA);
    public Handler mHandler;
    private Context context;
    public ConnectedThread mConnectedThread;

    private InternetService.OnInternetServiceInteractionListener mListener;
    public InternetService(final Context context){
        //这里把接受到的字符串写进去
        this.context = context;
        if (context instanceof InternetService.OnInternetServiceInteractionListener) {
            mListener = (InternetService.OnInternetServiceInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInternetServiceInteractionListener");
        }
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (mListener != null) {
                    mListener.onInternetServiceInteraction(msg);
                }
                return true;
            }
        });
    }
    public void connect(byte[] send){
        // If there are paired devices, add each one to the ArrayAdapter
        new Thread(new ConnectThread(send)).start();
    }


    private class ConnectThread extends Thread {
        private Socket socket;
        private byte[] mSend;
        public ConnectThread(byte[] send) {
            mSend = send;
            mState = STATE_CONNECTING;
            Message msg = mHandler.obtainMessage(MESSAGE_SET_CONNECTING);
            mHandler.sendMessage(msg);
        }

        public void run() {
            try{
                socket = new Socket();
                socket.connect(new InetSocketAddress(SERVER_HOST_IP, SERVER_HOST_PORT), 2000);
            } catch (UnknownHostException e) {
                popMessage("Unknown host exception: " + e.toString());
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e2) {
                    popMessage("unable to close() socket during connection failure: "+e2.getMessage()+"\n");
                    socket = null;
                }
            } catch (IOException e) {
                popMessage( "IO exception: " + e.toString());
                socket = null;
            }catch (SecurityException e) {
                popMessage( "Security exception: " + e.toString());
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e2) {
                    popMessage("unable to close() socket during connection failure: "+e2.getMessage()+"\n");
                    socket = null;
                }
                socket = null;
            }
            if (socket != null) {
                new Thread(new ConnectedThread(socket,mSend)).start();
            } else {
                mState = STATE_NONE;
                Message msg1 = mHandler.obtainMessage(MESSAGE_SET_TO_CONNECT);
                mHandler.sendMessage(msg1);
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
            Message msg = mHandler.obtainMessage(MESSAGE_SET_CONNECTED);
            mHandler.sendMessage(msg);

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
                mmOutStream.write(buffer);
            } catch (IOException e) {
                popMessage("写操作出现异常");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                mState = STATE_NONE;
            } catch (IOException e) {
                popMessage("关闭套接字失败");
            }
        }

        public void handleTCPPack(byte state_code,byte[] pack) {
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
////                        popMessage("capture_time:" +  String.valueOf(capture_time));
////                        popMessage("value:" +  String.valueOf(value));

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
                        data_len = (data_len << 8) + len_byte[i];
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
                        handleTCPPack(data_buffer[0],DataTransfer.BytesSub(data_buffer,1,data_buffer.length));
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

    public interface OnInternetServiceInteractionListener {
        // TODO: Update argument type and name
        void onInternetServiceInteraction(Message msg);
    }
}

