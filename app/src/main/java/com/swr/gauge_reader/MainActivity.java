package com.swr.gauge_reader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.*;



public class MainActivity extends AppCompatActivity implements DataFragment.OnDataFragmentInteractionListener,
        PictureFragment.OnPictureFragmentInteractionListener,OptionsFragment.OnOptionsFragmentInteractionListener ,
        InternetTCPService.OnInternetTCPServiceInteractionListener, InternetUDPService.OnInternetUDPServiceInteractionListener{

    InternetTCPService mInternetTCPService;
    InternetUDPService mInternetUDPService;
    public TextView log;

    private static final int INDEX_PICTURE = 0;
    private static final int INDEX_DATA = 1;
    private static final int INDEX_OPTIONS = 2;

    TabLayout mTabLayout;
    SlideViewPager mViewPager;

    List<String> mTitle;
    List<Fragment> mFragment;

    private final byte[] AA = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x81};
    private final byte[] FIRST_BYTE = DataTransfer.BytesConcact("GgRd:".getBytes(),AA);

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);

        mTabLayout = findViewById(R.id.tablayout);
        mViewPager = findViewById(R.id.viewpager);


        mTitle = new ArrayList<>();
        mTitle.add("Picture");
        mTitle.add("Data");
        mTitle.add("Options");

        mFragment = new ArrayList<>();
        mFragment.add(PictureFragment.newInstance(new Bundle()));
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
        mViewPager.setIsScanScroll(false);
        mTabLayout.setupWithViewPager(mViewPager);
//        loadSharedPreference();
        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());
//
        mInternetTCPService = new InternetTCPService(this);
        mInternetUDPService = new InternetUDPService(this);
        mInternetUDPService.connect();
    }
    
//    private void loadSharedPreference(){
//        SharedPreferences userSettings = getSharedPreferences("options", 0);
//
//        dataView.gridX = userSettings.getInt("gridX",4);
//        dataView.gridY = userSettings.getInt("gridY",3);
//        dataView.style = userSettings.getInt("style ", dataView.STYLE_BLACK);
//
//    }
//
//    private void saveSharedPreference(){
//        SharedPreferences userSettings = getSharedPreferences("options", 0);
//        SharedPreferences.Editor editor = userSettings.edit();
//
//        editor.putInt("gridX", dataView.gridX);
//        editor.putInt("gridY", dataView.gridY);
//        editor.putInt("style ", dataView.style);
//        editor.commit();
//    }

    @Override
    public void onInternetTCPServiceInteraction(Message msg) {
        Button mInternetButton = ((DataFragment)mFragment.get(INDEX_DATA)).mInternetButton;
        Button mCaptureButton = ((DataFragment)mFragment.get(INDEX_DATA)).mCaptureButton;
        Button mSaveDataButton = ((DataFragment)mFragment.get(INDEX_DATA)).mSaveDataButton;
        DataView view = ((DataFragment)mFragment.get(INDEX_DATA)).mDataView;
        if (msg.what == InternetTCPService.MESSAGE_INVALIDATE) {
            long[] time = msg.getData().getLongArray("time");
            double[] value = msg.getData().getDoubleArray("value");
            view.data = value;
            view.time = time;
            mSaveDataButton.setEnabled(true);
            view.invalidate();

        }else if (msg.what == InternetTCPService.MESSAGE_IMAGE) {
            PictureView imgview = ((PictureFragment)mFragment.get(INDEX_PICTURE)).mPictureView;
            int playButtonState = ((PictureFragment)mFragment.get(INDEX_PICTURE)).mPlayButtonState;
            byte[] img = (byte[]) msg.obj;
            Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length);
            imgview.setPicture(bm);
            imgview.invalidate();
            if(playButtonState == PictureFragment.STATE_STOP)mInternetTCPService.interact(FIRST_BYTE);

        }else if (msg.what == InternetTCPService.MESSAGE_TOAST) {

            String string = msg.getData().getString("msg");
            toastMessage(string);

        }else if (msg.what == InternetTCPService.MESSAGE_SET_TO_CONNECT) {

            // set button states
            mCaptureButton.setEnabled(false);
            mInternetButton.setEnabled(true);
            mInternetButton.setText("网络连接");
            //set click motion
            //if you click the interact button, it connects to the SERVER_HOST_IP,SERVER_HOST_PORT specified before
            mInternetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInternetTCPService.interact(FIRST_BYTE);
                    toastMessage("connecting...");
                }
            });
        }else if (msg.what == InternetTCPService.MESSAGE_SET_CONNECTING) {
            mInternetButton.setEnabled(false);
            mInternetButton.setText("连接中");
            mInternetButton.setOnClickListener(null);
        }else if (msg.what == InternetTCPService.MESSAGE_SET_CONNECTED) {
            mInternetButton.setEnabled(true);
            ((DataFragment)mFragment.get(INDEX_DATA)).mCaptureButton.setEnabled(true);
            mInternetButton.setText("断开连接");
            mInternetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInternetTCPService.mState = InternetTCPService.STATE_NONE;
                    Message msg = mInternetTCPService.mHandler.obtainMessage(InternetTCPService.MESSAGE_SET_TO_CONNECT);
                    mInternetTCPService.mHandler.sendMessage(msg);
                }
            });
        }
    }

    @Override
    public void onInternetUDPServiceInteraction(Message msg) {
        if (msg.what == InternetTCPService.MESSAGE_TOAST) {

            String string = msg.getData().getString("msg");
            toastMessage(string);

        }else if (msg.what == InternetTCPService.MESSAGE_IMAGE) {
            PictureView imgview = ((PictureFragment)mFragment.get(INDEX_PICTURE)).mPictureView;
            byte[] img = (byte[]) msg.obj;
            Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length);
            imgview.setPicture(bm);
            imgview.invalidate();
        }
    }
    @Override
    public void onDataFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == DataFragment.TOAST_MESSAGE){
            toastMessage(bundle.getString(DataFragment.MESSAGE," "));
        }else if(state_code == DataFragment.CHECK_WAVE) {

        }
    }

    @Override
    public void onPictureFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == PictureFragment.START_PLAY){
            mInternetTCPService.interact(FIRST_BYTE);
        }else if(state_code == PictureFragment.STOP_PLAY){
//            mInternetTCPService.interact(FIRST_BYTE);
        }
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
