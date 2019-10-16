package com.swr.gauge_reader;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.*;



public class MainActivity extends AppCompatActivity implements DataFragment.OnDataFragmentInteractionListener,
        PictureFragment.OnPictureFragmentInteractionListener,OptionsFragment.OnOptionsFragmentInteractionListener ,
        InternetService.OnInternetServiceInteractionListener{

    InternetService mInternetService;

    public DataView dataView;
    public TextView log;

    private static final int INDEX_PICTURE = 0;
    private static final int INDEX_DATA = 1;
    private static final int INDEX_OPTIONS = 2;

    public ImageView mImageView;

    TabLayout mTabLayout;
    ViewPager mViewPager;

    List<String> mTitle;
    List<Fragment> mFragment;

    private final byte[] AA = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x80};
    private final byte[] FIRST_BYTE = DataTransfer.BytesConcact("GgRd:".getBytes(),AA);

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);
        dataView = findViewById(R.id.mainview);

        mTabLayout = findViewById(R.id.tablayout);
        mViewPager = findViewById(R.id.viewpager);


        mTitle = new ArrayList<>();
        mTitle.add("Picture");
        mTitle.add("Data");
        mTitle.add("Options");

        mFragment = new ArrayList<>();
        mFragment.add(PictureFragment.newInstance("abc","aaa"));
        mFragment.add(DataFragment.newInstance(new Bundle()));
        mFragment.add(OptionsFragment.newInstance(new Bundle()));

        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragment.get(position);
            }

            @Override
            public int getCount() {
                return mFragment.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return mTitle.get(position);
            }
        });

        mTabLayout.setupWithViewPager(mViewPager);
//        loadSharedPreference();
        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());
//
        mInternetService = new InternetService(this);
//        Message msg = mInternetService.mHandler.obtainMessage(InternetService.MESSAGE_SET_TO_CONNECT);
//        mInternetService.mHandler.sendMessage(msg);
    }
    
    private void loadSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);

        dataView.gridX = userSettings.getInt("gridX",4);
        dataView.gridY = userSettings.getInt("gridY",3);
        dataView.style = userSettings.getInt("style ", dataView.STYLE_BLACK);

    }
    
    private void saveSharedPreference(){
        SharedPreferences userSettings = getSharedPreferences("options", 0);
        SharedPreferences.Editor editor = userSettings.edit();

        editor.putInt("gridX", dataView.gridX);
        editor.putInt("gridY", dataView.gridY);
        editor.putInt("style ", dataView.style);
        editor.commit();
    }

    @Override
    public void onInternetServiceInteraction(Message msg) {
        Button mInternetButton = ((DataFragment)mFragment.get(INDEX_DATA)).mInternetButton;
        Button mCaptureButton = ((DataFragment)mFragment.get(INDEX_DATA)).mCaptureButton;
        Button mSaveDataButton = ((DataFragment)mFragment.get(INDEX_DATA)).mSaveDataButton;
        DataView view = ((DataFragment)mFragment.get(INDEX_DATA)).mDataView;
        if (msg.what == InternetService.MESSAGE_INVALIDATE) {
            long[] time = msg.getData().getLongArray("time");
            double[] value = msg.getData().getDoubleArray("value");
            view.data = value;
            view.time = time;
            mSaveDataButton.setEnabled(true);
            view.invalidate();

        }else if (msg.what == InternetService.MESSAGE_IMAGE) {

//            ImageView imgview = ((MainActivity) context).mImageView;
//            byte[] img = (byte[]) msg.obj;
//            Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length);
////                        imgview.setImageBitmap(bm);
////                        imgview.invalidate();

        }else if (msg.what == InternetService.MESSAGE_TOAST) {

            String string = msg.getData().getString("msg");
            toastMessage(string);

        }else if (msg.what == InternetService.MESSAGE_SET_TO_CONNECT) {

            // set button states
            mCaptureButton.setEnabled(false);
            mInternetButton.setEnabled(true);
            mInternetButton.setText("网络连接");
            //set click motion
            //if you click the connect button, it connects to the SERVER_HOST_IP,SERVER_HOST_PORT specified before
            mInternetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInternetService.connect(FIRST_BYTE);
                    toastMessage("connecting...");
                }
            });
        }else if (msg.what == InternetService.MESSAGE_SET_CONNECTING) {
            mInternetButton.setEnabled(false);
            mInternetButton.setText("连接中");
            mInternetButton.setOnClickListener(null);
        }else if (msg.what == InternetService.MESSAGE_SET_CONNECTED) {
            mInternetButton.setEnabled(true);
            ((DataFragment)mFragment.get(INDEX_DATA)).mCaptureButton.setEnabled(true);
            mInternetButton.setText("断开连接");
            mInternetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInternetService.mState = InternetService.STATE_NONE;
                    Message msg = mInternetService.mHandler.obtainMessage(InternetService.MESSAGE_SET_TO_CONNECT);
                    mInternetService.mHandler.sendMessage(msg);
                }
            });
        }
    }

    @Override
    public void onDataFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == DataFragment.TOAST_MESSAGE){
            toastMessage(bundle.getString(DataFragment.MESSAGE," "));
        }else if(state_code == DataFragment.CHECK_WAVE) {
            mInternetService.connect(FIRST_BYTE);
        }
    }

    @Override
    public void onPictureFragmentInteraction(int state_code, Bundle bundle) {
        String string = bundle.getString("msg");
        Toast.makeText(this,string , Toast.LENGTH_SHORT).show();
        log.append(string+"\n");
        int scroll_amount = (int) (log.getLineCount() * log.getLineHeight()) - (log.getBottom() - log.getTop());
        log.scrollTo(0, scroll_amount);
    }

    @Override
    public void onOptionsFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == OptionsFragment.UPDATE_OPTIONS){
            DataView mDataView = ((DataFragment)mFragment.get(INDEX_DATA)).mDataView;
            mDataView.gridX = bundle.getInt(OptionsFragment.GRIDX,3);
            mDataView.gridY = bundle.getInt(OptionsFragment.GRIDY,4);
            mDataView.style = bundle.getInt(OptionsFragment.STYLE,DataView.STYLE_BLACK);
            mDataView.invalidate();
        }
    }

    public void toastMessage(String string){
        Toast.makeText(this,string , Toast.LENGTH_SHORT).show();
        log.append(string+"\n");
        int scroll_amount = (int) (log.getLineCount() * log.getLineHeight()) - (log.getBottom() - log.getTop());
        log.scrollTo(0, scroll_amount);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
