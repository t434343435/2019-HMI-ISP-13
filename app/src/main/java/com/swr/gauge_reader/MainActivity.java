package com.swr.gauge_reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;



public class MainActivity extends Activity {


//    final int TRIGGER_U = 0;
//    final int TRIGGER_D = 1;
//    final int TRIGGER_N = 2;


    InternetService mInternetService;

    public MainView mainView;
    public TextView log;
    public Button mInternetButton;
    public Button mHelpButton;
    public Button mHistoricalDataButton;
    public Button mSaveDataButton;
    public Button mCaptureButton;
    public Button mOptionsButton;

//    int triggerLevel = 0x7F;
//    int triggerMode = TRIGGER_U;
//    int samplePoints = 1023;
//    int speed = 3;

    File[] files;
    ArrayList<Integer> yourChoices = new ArrayList<>();
    ArrayList<Integer> matchChoices = new ArrayList<>();
    boolean [] initChoiceSets;

    int tempGridX;
    int tempGridY;
    int tempSpeed;
    int tempTriggerLevel;
    int tempPoints;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);
        //约束socket允许运行在主线程
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().penaltyDeath().build());

        byte[] a = DataTransfer.Double2Bytes(1.2);
        double b = DataTransfer.Bytes2Double(a);
        mainView = findViewById(R.id.mainview);
        loadSharedPreference();
        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());
        mInternetButton = findViewById(R.id.bluetooth);
        mInternetService = new InternetService(this);
        Message msg = mInternetService.mHandler.obtainMessage(InternetService.MESSAGE_UPDATEINTERNETBUTOON);
        mInternetService.mHandler.sendMessage(msg);
        mHelpButton = findViewById(R.id.delete_wave);
        mHelpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setIcon(R.drawable.osc);
                final File file=new File(getFilesDir().getAbsolutePath());
                files=file.listFiles();
                matchChoices.clear();
                for (int i = 0; i < files.length; i++){
                    if(files[i].getName().contains("wave+")){
                        matchChoices.add(i);
                    }

                }
                String[] mWave = new String [matchChoices.size()];
                initChoiceSets = new boolean[matchChoices.size()];// 设置默认选中的选项，全为false默认均未选中
                yourChoices.clear();
                try {

                    for (int i = 0; i < matchChoices.size(); i++){
                        mWave[i] = files[i].getName().substring(5);
                        initChoiceSets[i] = false;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                builder.setTitle("请选择需要删除的捕获：");

                builder.setMultiChoiceItems(mWave, initChoiceSets,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    yourChoices.add(which);
                                } else {
                                    yourChoices.remove(which);
                                }
                            }
                        });

                builder.setPositiveButton("删除",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    int size = yourChoices.size();
                                    for (int i = 0; i < size; i++) {
                                        File file = new File(files[matchChoices.get(yourChoices.get(i))].getAbsolutePath());
                                        file.delete();
                                        mInternetService.popMessage("波形已被删除:"+
                                                files[matchChoices.get(yourChoices.get(i))].getName().substring(5));
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                builder.setNeutralButton("全部删除",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    for (int i = 0; i < matchChoices.size(); i++) {
                                        File file = new File(files[matchChoices.get(i)].getAbsolutePath());
                                        file.delete();
                                    }
                                    mInternetService.popMessage("全部波形已被删除");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                builder.setNegativeButton("取消", null);
                builder.show();
            }
        });
        mHistoricalDataButton = findViewById(R.id.historical_data);
        mHistoricalDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setIcon(R.drawable.osc);
                ArrayAdapter<String> mWaveArrayAdapter =
                        new ArrayAdapter<String>(MainActivity.this, R.layout.device_name);
                File file=new File(getFilesDir().getAbsolutePath());
                files=file.listFiles();
                try {
                    String[] mWave = new String [files.length];
                    for (int i = 0; i < files.length; i++) {
                        if(files[i].getName().contains("wave+")) {
                            mWaveArrayAdapter.add(files[i].getName().substring(5));
                            mWave[i] = files[i].getCanonicalPath();
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                builder.setTitle("请选择捕获时间：");
                builder.setAdapter(mWaveArrayAdapter, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            FileInputStream fis = new FileInputStream(files[which].getAbsolutePath());
                            int len = fis.available();
                            byte []buffer = new byte[len];
                            fis.read(buffer);
                            String s = new String(buffer);
                            String [] ss = s.split(" ");
                            mainView.data = new int [ss.length - 1];
                            mainView.dataSize = ss.length - 1;
//                            mainView.speed = Integer.parseInt(ss[0]);
                            for(int i = 0;i<ss.length-1;i++){
                                mainView.data[i] = Integer.parseInt(ss[i+1]);
                            }
                            mainView.resetScaleOrigin();
                            mainView.invalidate();
                            mSaveDataButton.setEnabled(true);
                            mInternetService.popMessage("波形已被载入:"+
                                    files[which].getName().substring(5));
                            fis.close();
                        } catch (Exception e) {
                            mainView.data = null;
                            mainView.dataSize = 0;
                            mSaveDataButton.setEnabled(false);
                            mainView.invalidate();
                            e.printStackTrace();
                        }

                    }
                });
                builder.show();
            }
        });
        mSaveDataButton = findViewById(R.id.save_data);
        mSaveDataButton.setEnabled(false);
        mSaveDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss

                Date date = new Date(System.currentTimeMillis()); //获取当前时间
                String mDateString = simpleDateFormat.format(date);
                try {
                    int [] mData = mainView.data;
                    if(mData == null)return;
                    FileOutputStream mFOS = MainActivity.this.openFileOutput("wave+"+mDateString, MODE_PRIVATE);//获得FileOutputStream //////
                    //将要写入的字符串转换为byte数组
                    StringBuffer tBuffer = new StringBuffer();
//                    tBuffer.append(String.format("%d ", speed));
                    for(int val:mData){
                        tBuffer.append(String.format("%d ", val));
                    }
                    String string = tBuffer.toString();
                    byte [] bytes = string.getBytes();
                    mFOS.write(bytes);//将byte数组写入文件
                    mFOS.close();//关闭文件输出流
                    mInternetService.popMessage("波形已保存:"+mDateString);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        mCaptureButton = findViewById(R.id.capture_wave);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                if(mInternetService.mState == mInternetService.STATE_CONNECTED)
//                    mInternetService.sendCapture();
            }
        });
        mCaptureButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
//                if(mInternetService.mState == mInternetService.STATE_CONNECTED)
//                    mInternetService.sendContinuousCapture();
                return true;
            }
        });
        mOptionsButton = findViewById(R.id.options);
        mOptionsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(MainActivity.this);
                final View dialogView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.options,null);
                builder.setTitle("选项");
                builder.setIcon(R.drawable.osc);
                builder.setView(dialogView);
                // 获取EditView中的输入内容
                tempGridX = mainView.gridX;
                tempGridY = mainView.gridY;
//                tempPoints = samplePoints;
//                tempSpeed = speed;
//                tempTriggerLevel = triggerLevel;

                TextView mGridXText =
                        (TextView) dialogView.findViewById(R.id.gridX);
                TextView mGridYText =
                        (TextView) dialogView.findViewById(R.id.gridY);
                TextView mSampleTimeText =
                        (TextView) dialogView.findViewById(R.id.sample_time);
                TextView mTriggerLevelText =
                        (TextView) dialogView.findViewById(R.id.triggerlevel);
                TextView mPointsText =
                        (TextView) dialogView.findViewById(R.id.sample_points);

                SeekBar mGridXSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.gridXSeekBar);
                SeekBar mGridYSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.gridYSeekBar);
                SeekBar mSampleTimeSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.sampleTimeSeekBar);
                SeekBar mTriggerLevelSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.scaleSeekBar);
                SeekBar mPointsSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.samplePointsSeekBar);
                RadioButton mBlackStyleRadioButton = (RadioButton) dialogView.findViewById(R.id.black_style);
                RadioButton mWhiteStyleRadioButton = (RadioButton) dialogView.findViewById(R.id.white_style);
//                RadioButton mTriggerURadioButton = (RadioButton) dialogView.findViewById(R.id.upward);
//                RadioButton mTriggerDRadioButton = (RadioButton) dialogView.findViewById(R.id.downward);
//                RadioButton mTriggerNRadioButton = (RadioButton) dialogView.findViewById(R.id.notrigger);

                if(mainView.style == mainView.STYLE_BLACK)
                    mBlackStyleRadioButton.setChecked(true);
                else
                    mWhiteStyleRadioButton.setChecked(true);

//                if(triggerMode == TRIGGER_U)
//                    mTriggerURadioButton.setChecked(true);
//                else if (triggerMode == TRIGGER_D)
//                    mTriggerDRadioButton.setChecked(true);
//                else
//                    mTriggerNRadioButton.setChecked(true);

                mGridXSeekBar.setProgress(tempGridX);
                mGridYSeekBar.setProgress(tempGridY);
                mPointsSeekBar.setProgress(tempPoints);
                mSampleTimeSeekBar.setProgress(tempSpeed);
                mTriggerLevelSeekBar.setProgress(tempTriggerLevel);
                mGridXText.setText("x轴的网格数:"+tempGridX);
                mGridYText.setText("y轴的网格数:"+tempGridY);
                mTriggerLevelText.setText("触发电平:"+tempTriggerLevel);
                mPointsText.setText("采样点数:"+(tempPoints+1));
                mSampleTimeText.setText("采样速率:"+tempSpeed);

                mGridXSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mGridXText =
                                (TextView) dialogView.findViewById(R.id.gridX);
                        tempGridX = progress;
                        mGridXText.setText("x轴的网格数:"+tempGridX);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                mGridYSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mGridYText =
                                (TextView) dialogView.findViewById(R.id.gridY);
                        tempGridY = progress;
                        mGridYText.setText("y轴的网格数:"+tempGridY);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {}
                });

                mGridYSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mGridYText =
                                (TextView) dialogView.findViewById(R.id.gridY);
                        tempGridY = progress;
                        mGridYText.setText("y轴的网格数:"+tempGridY);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {}
                });

                mSampleTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mSampleTimeText =
                                (TextView) dialogView.findViewById(R.id.sample_time);
                        tempSpeed = progress;
                        mSampleTimeText.setText("采样速率:"+tempSpeed);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {}
                });

                mTriggerLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mTriggerLevelText =
                                (TextView) dialogView.findViewById(R.id.triggerlevel);
                        tempTriggerLevel = progress;
                        mTriggerLevelText.setText("触发电平:"+tempTriggerLevel);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {}
                });

                mPointsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView mPointsText =
                                (TextView) dialogView.findViewById(R.id.sample_points);
                        SeekBar mGridYSeekBar =
                                (SeekBar) dialogView.findViewById(R.id.samplePointsSeekBar);
                        tempPoints = mGridYSeekBar.getProgress();
                        mPointsText.setText("采样点数:"+(tempPoints+1));
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void  onStopTrackingTouch(SeekBar seekBar) {}
                });
                builder.setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mainView.gridX = tempGridX;
                                mainView.gridY = tempGridY;
                                RadioButton mBlackStyleRadioButton = (RadioButton) dialogView.findViewById(R.id.black_style);
                                if(mBlackStyleRadioButton.isChecked())
                                    mainView.style = mainView.STYLE_BLACK;
                                else
                                    mainView.style = mainView.STYLE_WHITE;
                                mainView.invalidate();

//                                triggerLevel = tempTriggerLevel;
//                                RadioButton mTriggerURadioButton = (RadioButton) dialogView.findViewById(R.id.upward);
//                                RadioButton mTriggerDRadioButton = (RadioButton) dialogView.findViewById(R.id.downward);
//                                if(mTriggerURadioButton.isChecked())
//                                    triggerMode = TRIGGER_U;
//                                else if(mTriggerDRadioButton.isChecked())
//                                    triggerMode = TRIGGER_D;
//                                else
//                                    triggerMode = TRIGGER_N;
//
//                                samplePoints = tempPoints;
//                                speed = tempSpeed;
                                if(mInternetService.mState == mInternetService.STATE_CONNECTED)
//                                    mInternetService.sendOptionsAndStart(triggerLevel,triggerMode,speed,samplePoints);
                                saveSharedPreference();
                            }
                        });
                builder.setNegativeButton("取消", null);
                builder.show();

            }
        });
        /*new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                //do something
            }
        }, 3000);    //延时3s执行*/
        //Intent myIntent = new Intent(MainActivity.this, OscilloscopeActivity.class);
        //MainActivity.this.startActivity(myIntent);*/
    }
    private void loadSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);
//        triggerLevel = userSettings.getInt("triggerLevel",0x7F);
//        triggerMode = userSettings.getInt("triggerMode",TRIGGER_U);
//        samplePoints = userSettings.getInt("samplePoints",1023);
//        speed = userSettings.getInt("speed",0);
        mainView.gridX = userSettings.getInt("gridX",4);
        mainView.gridY = userSettings.getInt("gridY",3);
        mainView.style = userSettings.getInt("style ",mainView.STYLE_BLACK);

    }
    private void saveSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);
        SharedPreferences.Editor editor = userSettings.edit();
//        editor.putInt("triggerLevel",triggerLevel);
//        editor.putInt("triggerMode",triggerMode);
//        editor.putInt("samplePoints",samplePoints);
//        editor.putInt("speed",speed);
        editor.putInt("gridX",mainView.gridX);
        editor.putInt("gridY",mainView.gridY);
        editor.putInt("style ",mainView.style);
        editor.commit();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
}
