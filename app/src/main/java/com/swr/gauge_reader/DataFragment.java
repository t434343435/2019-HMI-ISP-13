package com.swr.gauge_reader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DataFragment.OnDataFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DataFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DataFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String MESSAGE = "MSG";

    public static final int TOAST_MESSAGE = 1;
    public static final int CHECK_WAVE = 2;
    public DataView mDataView;

    public Button mDeleteWaveButton;
    public Button mHistoricalDataButton;
    public Button mSaveDataButton;
    public Button mCaptureButton;
    public Button mClearButton;
    public Spinner mSpinner;
    public int captureButtonState;
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONTI = 1;

    public int dataIndex = 0;
    File[] files;
    ArrayList<Integer> yourChoices = new ArrayList<>();
    ArrayList<Integer> matchChoices = new ArrayList<>();
    boolean [] initChoiceSets;

    private OnDataFragmentInteractionListener mListener;

    List<double[]> waveList;

    public DataFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param args Parameter 1.
     * @return A new instance of fragment DataFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DataFragment newInstance(Bundle args) {
        DataFragment fragment = new DataFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waveList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View mDataFragmentView = inflater.inflate(R.layout.fragment_data, container, false);
        mSpinner = mDataFragmentView.findViewById(R.id.spinner);
        mDataView = mDataFragmentView.findViewById(R.id.dataview);
        mDeleteWaveButton = mDataFragmentView.findViewById(R.id.delete_wave);
        mDeleteWaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setIcon(R.drawable.osc);
                final File file=new File(getContext().getFilesDir().getAbsolutePath());
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
                                        toastMessage("波形已被删除:"+
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
                                    toastMessage("全部波形已被删除");
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
        mHistoricalDataButton = mDataFragmentView.findViewById(R.id.historical_data);
        mHistoricalDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setIcon(R.drawable.osc);
                ArrayAdapter<String> mWaveArrayAdapter =
                        new ArrayAdapter<String>(getContext(), R.layout.item_drop);
                File file=new File(getContext().getFilesDir().getAbsolutePath());
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
                            mDataView.data = new double[ss.length];
                            mDataView.time = new long[ss.length];
                            for(int i = 0;i<ss.length;i++){
                                Log.d("Tag",ss[i]);
                                String[] res = ss[i].split(":");
                                mDataView.time[i] = Long.parseLong(res[0]);
                                mDataView.data[i] = Double.parseDouble(res[1]);
                            }
                            mDataView.resetScaleOrigin();
                            mDataView.invalidate();
                            mSaveDataButton.setEnabled(true);
                            toastMessage("波形已被载入:"+
                                    files[which].getName().substring(5));
                            fis.close();
                        } catch (Exception e) {
                            mDataView.data = null;
                            mSaveDataButton.setEnabled(false);
                            mDataView.invalidate();
                            e.printStackTrace();
                        }

                    }
                });
                builder.show();
            }
        });

        // set the save data button function
        mClearButton = mDataFragmentView.findViewById(R.id.clear);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDataView.data = new double[0];
                mDataView.time = new long[0];
                mDataView.invalidate();
            }
        });
        mSaveDataButton = mDataFragmentView.findViewById(R.id.save_data);
        mSaveDataButton.setEnabled(false);
        mSaveDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss

                Date date = new Date(System.currentTimeMillis()); //获取当前时间
                String mDateString = simpleDateFormat.format(date);
                try {
                    double [] mData = mDataView.data;
                    long [] mTime = mDataView.time;
                    if(mData == null)return;
                    FileOutputStream mFOS = getContext().openFileOutput("wave+"+mDateString, MODE_PRIVATE);//获得FileOutputStream //////
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
                    toastMessage("波形已保存:"+mDateString);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        // set the capture button function
        mCaptureButton = mDataFragmentView.findViewById(R.id.capture_wave);
        mCaptureButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if(captureButtonState == STATE_IDLE){
                    onInteract(CHECK_WAVE,null);
                }else if(captureButtonState == STATE_CONTI){
                    captureButtonState = STATE_IDLE;
                }
            }
        });
        mCaptureButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if(captureButtonState == STATE_IDLE){
                    EditText delayEdit = mDataFragmentView.findViewById(R.id.delay_time_edit);
                    String str = delayEdit.getText().toString();
                    if(!str.equals("")) {
                        captureButtonState = STATE_CONTI;
                        new Thread(new CapturingThread(Integer.parseInt(str))).start();
                    }
                }else if(captureButtonState == STATE_CONTI){

                }
                return true;
            }
        });
        return mDataFragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onInteract(int state_code, Bundle bundle) {
        if (mListener != null) {
            mListener.onDataFragmentInteraction(state_code, bundle);
        }
    }
    
    public void toastMessage(String string) {
        if (mListener != null) {
            Bundle bundle = new Bundle();
            bundle.putString(MESSAGE,string);
            mListener.onDataFragmentInteraction(TOAST_MESSAGE, bundle);
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDataFragmentInteractionListener) {
            mListener = (OnDataFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDataFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class CapturingThread extends Thread {
        private long delay;

        public CapturingThread(long millis) {
            delay = millis;
        }

        public void run() {
            while (captureButtonState == STATE_CONTI) {
                try {
                    sleep(delay);//毫秒
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onInteract(CHECK_WAVE, null);
            }
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDataFragmentInteractionListener {
        // TODO: Update argument type and name
        void onDataFragmentInteraction(int state_code, Bundle bundle);
    }
}
