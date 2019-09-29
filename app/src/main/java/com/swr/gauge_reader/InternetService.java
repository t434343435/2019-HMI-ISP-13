package com.swr.gauge_reader;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by t4343 on 2018/2/23.
 */

public class InternetService {

    public int mState;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;


    public static final int MESSAGE_INVALIDATE = 0;
    public static final int MESSAGE_UPDATEINTERNETBUTOON = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_REQUESTENABLE = 3;
    public static final int MESSAGE_SENDSTART = 4;



    private final String SERVER_HOST_IP = "192.168.43.72";
    private final int SERVER_HOST_PORT = 9999;

    public Handler mHandler;
    private Context context;
    public ConnectedThread mConnectedThread;


    public InternetService(final Context context){
        //这里把接受到的字符串写进去
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                final MainActivity activity = ((MainActivity)context);
                switch (msg.what) {
                    case MESSAGE_INVALIDATE:
                        int[] Data = (int[]) msg.obj;
                        int DataSize = msg.arg1;
                        MainView view = ((MainActivity)context).mainView;
                        view.setDataSize(DataSize);
                        view.data = Data;
//                        view.speed = activity.speed;
                        activity.mSaveDataButton.setEnabled(true);
                        view.invalidate();
                        break;

                    case MESSAGE_TOAST:
                        String string = msg.getData().getString("msg");
                        Toast.makeText(InternetService.this.context,string , Toast.LENGTH_SHORT).show();
                        TextView log = ((MainActivity)context).log;
                        log.append(string+"\n");
                        int scroll_amount = (int) (log.getLineCount() * log.getLineHeight()) - (log.getBottom() - log.getTop());
                        log.scrollTo(0, scroll_amount);
                        break;
                    case MESSAGE_REQUESTENABLE:
                        //it seems that it's no use
//                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                        activity.startActivityForResult(enableBtIntent,0);
                        break;
                    case MESSAGE_SENDSTART:
//                        sendOptionsAndStart(activity.triggerLevel,activity.triggerMode,
//                                            activity.speed,activity.samplePoints);
                        break;
                    case MESSAGE_UPDATEINTERNETBUTOON:
                        Button mInternetButton = activity.mInternetButton ;

                        if (mState ==STATE_NONE){
                            // set button states
                            activity.mCaptureButton.setEnabled(false);
                            activity.mInternetButton.setEnabled(true);
                            activity.mInternetButton.setText("网络连接");
                            
                            //set click motion
                            //if you click the connect button, it connects to the SERVER_HOST_IP,SERVER_HOST_PORT specified before
                            mInternetButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    connect();
                                }
                            });
                        }else if(mState == STATE_CONNECTING){
                            mInternetButton.setEnabled(false);
                            mInternetButton.setText("连接中");
                            mInternetButton.setOnClickListener(null);
                        }else if(mState == STATE_CONNECTED){
                            mInternetButton.setEnabled(true);
                            activity.mCaptureButton.setEnabled(true);
                            mInternetButton.setText("断开连接");

                            mInternetButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
//                                    sendDisconnect();
                                    InternetService.this.mConnectedThread.cancel();

                                }
                            });

                        }

                        break;
                }
                return true;
            }
        });
        this.context = context;
    }
    public void connect(){
        // If there are paired devices, add each one to the ArrayAdapter
        new Thread(new ConnectThread()).start();
    }


    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }


    private class ConnectThread extends Thread {
        private Socket socket;
        public ConnectThread() {
            mState = STATE_CONNECTING;
            Message msg = mHandler.obtainMessage(MESSAGE_UPDATEINTERNETBUTOON);
            mHandler.sendMessage(msg);
        }

        public void run() {
//            popMessage("客户端连接中...");
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
//                popMessage("连接建立成功");

                new Thread(new ConnectedThread(socket)).start();
            } else {
                //popMessage("已建立连接,但是套接字为空");
                mState = STATE_NONE;
                Message msg1 = mHandler.obtainMessage(MESSAGE_UPDATEINTERNETBUTOON);
                mHandler.sendMessage(msg1);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                popMessage( "close() of connect socket failed: "+e.getMessage() +"\n");
            }
        }
    }
    public class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

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

        public ConnectedThread(Socket socket) {
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
            Message msg = mHandler.obtainMessage(MESSAGE_UPDATEINTERNETBUTOON);
            mHandler.sendMessage(msg);

        }

        public void run() {
//            popMessage("开始建立连接线程");
            byte[] buffer = new byte[1024];
            int bytes;
            try{
                mmOutStream.write("received".getBytes());}
            catch (IOException e){

            }
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    byte[] data = DataTransfer.BytesSub(buffer,0,bytes);
                    handleData(data);

                } catch (IOException e) {
                    popMessage("已失去连接");
                    mState = STATE_NONE;
                    Message msg1 = mHandler.obtainMessage(MESSAGE_UPDATEINTERNETBUTOON);
                    mHandler.sendMessage(msg1);
                    break;
                }

            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
            } catch (IOException e) {
                popMessage("写操作出现异常");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                mState = STATE_NONE;
                Message msg = mHandler.obtainMessage(MESSAGE_UPDATEINTERNETBUTOON);
                mHandler.sendMessage(msg);
            } catch (IOException e) {
                popMessage("关闭套接字失败");
            }
        }

        public void handleTCPPack(byte state_code,byte[] pack) {
            switch(state_code){
                case SC_DATA:
                    int len = DataTransfer.Bytes2Int(DataTransfer.BytesSub(pack,0,4));
                    for(int i = 0; i < len; i++){
                        long capture_time =
                                DataTransfer.Bytes2Long(DataTransfer.BytesSub(pack,i * 16 + 4, i * 16 + 12));
                        double value =
                                DataTransfer.Bytes2Double(DataTransfer.BytesSub(pack,i * 16 + 12, i * 16 + 20));
                        popMessage("capture_time:" +  String.valueOf(capture_time));
                        popMessage("value:" +  String.valueOf(value));

                }
                    break;
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


    private class HandleDataThread extends Thread {
        public HandleDataThread() {

        }

        public void run() {

        }

        public void cancel() {
        }
    }
}

