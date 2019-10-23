package com.swr.gauge_reader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
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

    private static final byte[] HEAD = "GgRd:".getBytes();
    private static final byte[] REQUEST_DATA = {0x01,0x00,0x00,0x00,(byte)0x80};
    private static final byte[] REQUEST_IMAGE = {0x01,0x00,0x00,0x00,(byte)0x81};
    private static final byte[] SET_TEMPLATE = {(byte)0x82};
    private static final byte[] CLEAR_TEMPLATE = {0x01,0x00,0x00,0x00,(byte)0x83};
    private static final byte[] REQUEST_IMAGE_CODE = Util.BytesConcat(HEAD, REQUEST_IMAGE);
    private static final byte[] REQUEST_DATA_CODE = Util.BytesConcat(HEAD, REQUEST_DATA);
    private static final byte[] CLEAR_TEMPLATE_CODE = Util.BytesConcat(HEAD, CLEAR_TEMPLATE);

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
        mInternetTCPService = new InternetTCPService(this);
//        mInternetUDPService = new InternetUDPService(this);
//        mInternetUDPService.connect();
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
    public void onResume()
    {
        super.onResume();
    }
    @Override
    public void onInternetTCPServiceInteraction(Message msg) {
        Button mCaptureButton = ((DataFragment) mFragment.get(INDEX_DATA)).mCaptureButton;
        Button mSaveDataButton = ((DataFragment) mFragment.get(INDEX_DATA)).mSaveDataButton;
        DataView view = ((DataFragment) mFragment.get(INDEX_DATA)).mDataView;
        if (msg.what == InternetTCPService.MESSAGE_INVALIDATE) {
            double[] value = msg.getData().getDoubleArray("value");
            view.data = Util.DoublesConcat(view.data, value[0]);
            view.time = Util.LongsConcat(view.time, System.currentTimeMillis());
            mSaveDataButton.setEnabled(true);
            view.invalidate();
        } else if (msg.what == InternetTCPService.MESSAGE_IMAGE) {
            PictureView imgview = ((PictureFragment) mFragment.get(INDEX_PICTURE)).mPictureView;
            PictureFragment fragment = (PictureFragment) mFragment.get(INDEX_PICTURE);
            byte[] img = (byte[]) msg.obj;
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inScaled = false;
            Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length,op);
            imgview.setPicture(bm);
            ArrayList<PictureView.TagRect> tgl = (ArrayList<PictureView.TagRect>)imgview.tagRectList;
            double[] value = msg.getData().getDoubleArray("value");
            for(int i = 0; i < value.length; i++){
                float gc = fragment.getNeedleLength(i);
                RectF vector = fragment.getGaugeCenter(i);
                vector.right -= (float) Math.cos(value[i]*Math.PI/180)*gc;
                vector.bottom -= (float) Math.sin(value[i]*Math.PI/180)*gc;
                imgview.setTagVector(i,"",vector, Color.GREEN);
            }
            for(int i = 0; i< value.length; i++){
                tgl.get(i).setText(fragment.getGaugeName(i) + String.format(":%.2f",value[i]));
            }
            imgview.invalidate();
        } else if (msg.what == InternetTCPService.MESSAGE_TOAST) {
            String string = msg.getData().getString("msg");
            toastMessage(string);
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
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inScaled = false;
            imgview.invalidate();

        }
    }
    @Override
    public void onDataFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == DataFragment.TOAST_MESSAGE){
            toastMessage(bundle.getString(DataFragment.MESSAGE," "));
        }else if(state_code == DataFragment.CHECK_WAVE) {
            mInternetTCPService.interact(REQUEST_DATA_CODE);
        }
    }

    @Override
    public void onPictureFragmentInteraction(int state_code, Bundle bundle) {
        if(state_code == PictureFragment.TOAST_MESSAGE) {
            toastMessage(bundle.getString(PictureFragment.MESSAGE, " "));
        }
        else if(state_code == PictureFragment.PLAY_FRAME) {
//            mInternetTCPService.interact(REQUEST_DATA_CODE);
            mInternetTCPService.interact(REQUEST_IMAGE_CODE);
        }else if(state_code == PictureFragment.SET_TEMPLATE) {
            if(mInternetTCPService.mState == InternetTCPService.STATE_NONE){
                byte [] code = bundle.getByteArray(PictureFragment.MSG_SET_TPL);
                code = Util.BytesConcat(SET_TEMPLATE,code);
                byte [] head = Util.BytesConcat(HEAD,Util.Int2Bytes(code.length));
                ((DataFragment) mFragment.get(INDEX_DATA)).waveList.clear();
                mInternetTCPService.interact(Util.BytesConcat(head,code));
            }
        }else if(state_code == PictureFragment.CLEAR_TEMPLATE) {
            if(mInternetTCPService.mState == InternetTCPService.STATE_NONE)
                mInternetTCPService.interact(CLEAR_TEMPLATE_CODE);
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

}
