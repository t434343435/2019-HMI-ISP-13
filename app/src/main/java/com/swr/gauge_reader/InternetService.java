package com.swr.gauge_reader;

import android.content.Context;
import android.content.Intent;
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
    private final int SERVER_HOST_PORT = 9500;

    public Handler mHandler;
    private Context context;
    public ConnectedThread mConnectedThread;

    private String [] mPairedDeviceList;

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
//                        String str = "";
//                        for(int ch:Data) {
//                            str = str +" "+ch;
//                        }
//                        Log.d("swr",""+DataSize);
                        view.setDataSize(DataSize);//data[i] & 0xff);
                        view.data = Data;
                        view.speed = activity.speed;
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
                                    sendDisconnect();
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
            popMessage("客户端连接中...");
            try{
                socket = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
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
                popMessage("连接建立成功");

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
        private byte[]  data_buffer;
        private int data_len;
        private int received_len;

        private static final int FIND_HEAD = 0;
        private static final int READ_LEN = 1;
        private static final int READ_DATA = 2;
        private static final int DONE = 3;
        
        
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
            popMessage("开始建立连接线程");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            Message msg = mHandler.obtainMessage(MESSAGE_SENDSTART);
            mHandler.sendMessage(msg);
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    handleData(buffer, bytes);

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

        private synchronized void handleData(byte[] data, int num) {
            while True:
            data = bytes_buffer + self.request.recv(1024)
            // 状态机 获取帧头后，将帧传送给handle_udp_data(data)处理
            if （state == FIND_HEAD）{
                index = data.find(HEAD)
                if index <= -1:
                bytes_buffer = new byte[0];
                state = FIND_HEAD;
                else
                bytes_buffer = data[index + len(HEAD):];
                state = READ_LEN;
            }
            else if (state == READ_LEN) {
                data_len = int.from_bytes(data[:4],'big');
                bytes_buffer = data[4:];
                data_buffer = new byte[0];
                state = READ_DATA;
                received_len = 0;
            }
            else if (state == READ_DATA) {
                received_len = received_len + len(data);
                if (received_len<data_len) {
                    bytes_buffer = new byte[0];
                    data_buffer = data_buffer + data;
                    state = READ_DATA;
                }
                else {
                    bytes_buffer = data[data_len - received_len + len(data):];
                    data_buffer = data_buffer + data[:data_len - received_len + len(data)];
                    state = FIND_HEAD;
                    handle_udp_data(data_buffer);
                    self.request.close();
                    return;
                }
            }
//            for (int i = 0; i < num; i++) {
//                if (receivedflag == FLAG_END) {
//                    if ((data[i] & 0xff) != 0x55) {
//                        if (receivednum < mDataSize) {
//                            try {
//                                mData[receivednum] = 0xAA;
//                                receivednum++;
//                                mData[receivednum] = data[i] & 0xff;
//                                receivednum++;
//                            } catch (ArrayIndexOutOfBoundsException e){
//
//                            }
//                        }
//                        receivedflag = FLAG_DATA;
//                    } else {
//                        mHandler.obtainMessage(MESSAGE_INVALIDATE, mDataSize, 0, mData)
//                                .sendToTarget();
//                        receivedflag = FLAG_START1;
//                        receivednum = 0;
//                    }
//                } else if (receivedflag == FLAG_DATA) {     //存储高8位
//                    if ((data[i] & 0xff) == 0xAA)
//                        receivedflag = FLAG_END;
//                    else {
//                        receivedflag = FLAG_DATA;
//                        try {
//                            if (receivednum < mDataSize) {
//                                mData[receivednum] = (data[i] & 0xff);
//                                receivednum++;
//                            }
//                        }catch (ArrayIndexOutOfBoundsException e){
//
//                        }
//                    }
//                }else if (receivedflag == FLAG_DATASIZE2) {
////                    if (data[i] < 0) {
////                        mDataSize += data[i] & 0xff + 256;
////                    } else {
////                        mDataSize += data[i] & 0xff;
////                    }
//                    mDataSize += data[i] & 0xff;
//                    if(mDataSize >= 0) {
//                        mData = new int[mDataSize];
//                        receivedflag = FLAG_DATA;
//                    } else {
//                        receivedflag = FLAG_START1;
//                    }
//                } else if (receivedflag == FLAG_DATASIZE1) {
////                    if (data[i] < 0) {
////                        mDataSize = (data[i] & 0xff + 256)<<8;
////                    } else {
////                        mDataSize = (data[i] & 0xff)<<8;
////                    }
//                    mDataSize = (data[i] & 0xff)<<8;
//                    receivedflag = FLAG_DATASIZE2;
//                } else if (receivedflag == FLAG_START2) {      //第二位是 0x55
//                    if ((data[i] & 0xff) == 0x55)
//                        receivedflag = FLAG_DATASIZE1;
//                    else
//                        receivedflag = FLAG_START1;
//                } else if (receivedflag == FLAG_START1) {      //第一位是 0xAA
//                    if ((data[i] & 0xff) == 0xAA)
//                        receivedflag = FLAG_START2;
//                    else
//                        receivedflag = FLAG_START1;
//                }
//            }
        }
    }
    public void popMessage(String string){
    //    Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle b = new Bundle();
        b.putString("msg", string );
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

//发送0后接3个参数 1.触发电平 2.触发模式和采样速率和采样点数低2位 3.采样点数高8位
    public void sendOptionsAndStart(int trigger,int triggermode,int rate,int points){
        byte []mSendData = new byte[6];

        mSendData[0] = (byte)0xAA;
        mSendData[1] = (byte)0x55;
        mSendData[2] = (byte)0x00;
        mSendData[3] = (byte)(trigger&0xFF);
        mSendData[4] = (byte)((points&0x03)+((rate&0x03)<<2)+((triggermode&0x03)<<4));
        mSendData[5] = (byte)((points>>2)&0xFF);
        write(mSendData);
    }

    public void sendCapture(){
        byte []mSendData = new byte[3];
        mSendData[0] = (byte)0xAA;
        mSendData[1] = (byte)0x55;
        mSendData[2] = (byte)0x01;
        write(mSendData);
    }

    public void sendContinuousCapture(){
        byte []mSendData = new byte[3];
        mSendData[0] = (byte)0xAA;
        mSendData[1] = (byte)0x55;
        mSendData[2] = (byte)0x02;
        write(mSendData);
    }
    public void sendDisconnect(){
        byte []mSendData = new byte[3];
        mSendData[0] = (byte)0xAA;
        mSendData[1] = (byte)0x55;
        mSendData[2] = (byte)0x03;
        write(mSendData);
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

