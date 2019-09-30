package com.swr.gauge_reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;



public class MainActivity extends Activity {

    InternetService mInternetService;

    public MainView mainView;
    public TextView log;
    public Button mInternetButton;
    public Button mDeleteWaveButton;
    public Button mHistoricalDataButton;
    public Button mSaveDataButton;
    public Button mCaptureButton;
    public Button mOptionsButton;

    public ImageView mImageView;

    File[] files;
    ArrayList<Integer> yourChoices = new ArrayList<>();
    ArrayList<Integer> matchChoices = new ArrayList<>();
    boolean [] initChoiceSets;

    int tempGridX;
    int tempGridY;

    private final byte[] AA = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x81};
    private final byte[] FIRST_BYTE = DataTransfer.BytesConcact("GgRd:".getBytes(),AA);

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);
        mainView = findViewById(R.id.mainview);
        loadSharedPreference();

        mImageView = findViewById(R.id.imageView);

        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        mInternetButton = findViewById(R.id.internet_button);
        mInternetService = new InternetService(this);
        Message msg = mInternetService.mHandler.obtainMessage(InternetService.MESSAGE_SET_TO_CONNECT);
        mInternetService.mHandler.sendMessage(msg);

        // set the delete wave function
        mDeleteWaveButton = findViewById(R.id.delete_wave);
        mDeleteWaveButton.setOnClickListener(new View.OnClickListener() {
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
        
        //set the historical data button
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
                            mainView.data = new double[ss.length];
                            for(int i = 0;i<ss.length;i++){
                                String[] res = ss[i].split(":");
                                mainView.time[i] = Long.parseLong(res[0]);
                                mainView.data[i] = Double.parseDouble(res[1]);
                            }
                            mainView.resetScaleOrigin();
                            mainView.invalidate();
                            mSaveDataButton.setEnabled(true);
                            mInternetService.popMessage("波形已被载入:"+
                                    files[which].getName().substring(5));
                            fis.close();
                        } catch (Exception e) {
                            mainView.data = null;
                            mSaveDataButton.setEnabled(false);
                            mainView.invalidate();
                            e.printStackTrace();
                        }

                    }
                });
                builder.show();
            }
        });
        
        // set the save data button function
        mSaveDataButton = findViewById(R.id.save_data);
        mSaveDataButton.setEnabled(false);
        mSaveDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss

                Date date = new Date(System.currentTimeMillis()); //获取当前时间
                String mDateString = simpleDateFormat.format(date);
                try {
                    double [] mData = mainView.data;
                    long [] mTime = mainView.time;
                    if(mData == null)return;
                    FileOutputStream mFOS = MainActivity.this.openFileOutput("wave+"+mDateString, MODE_PRIVATE);//获得FileOutputStream //////
                    //将要写入的字符串转换为byte数组
                    StringBuffer tBuffer = new StringBuffer();
//                    tBuffer.append(String.format("%d ", speed));
                    for(int i = 0; i < mData.length; i++){
                        tBuffer.append(String.format("%d:%f ", mTime[i], mData[i]));
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

        // set the capture button function
        mCaptureButton = findViewById(R.id.capture_wave);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mInternetService.connect(FIRST_BYTE);
            }
        });
        mCaptureButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {

                return true;
            }
        });

        
        // set the options button function
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

                TextView mGridXText =
                        (TextView) dialogView.findViewById(R.id.gridX);
                TextView mGridYText =
                        (TextView) dialogView.findViewById(R.id.gridY);


                SeekBar mGridXSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.gridXSeekBar);
                SeekBar mGridYSeekBar =
                        (SeekBar) dialogView.findViewById(R.id.gridYSeekBar);

                RadioButton mBlackStyleRadioButton = (RadioButton) dialogView.findViewById(R.id.black_style);
                RadioButton mWhiteStyleRadioButton = (RadioButton) dialogView.findViewById(R.id.white_style);

                if(mainView.style == mainView.STYLE_BLACK)
                    mBlackStyleRadioButton.setChecked(true);
                else
                    mWhiteStyleRadioButton.setChecked(true);

                mGridXSeekBar.setProgress(tempGridX);
                mGridYSeekBar.setProgress(tempGridY);

                mGridXText.setText("x轴的网格数:"+tempGridX);
                mGridYText.setText("y轴的网格数:"+tempGridY);

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
                                saveSharedPreference();
                            }
                        });
                builder.setNegativeButton("取消", null);
                builder.show();

            }
        });
    }
    
    private void loadSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);

        mainView.gridX = userSettings.getInt("gridX",4);
        mainView.gridY = userSettings.getInt("gridY",3);
        mainView.style = userSettings.getInt("style ",mainView.STYLE_BLACK);

    }
    
    private void saveSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);
        SharedPreferences.Editor editor = userSettings.edit();

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
