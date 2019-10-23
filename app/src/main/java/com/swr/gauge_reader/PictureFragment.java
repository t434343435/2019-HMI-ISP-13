package com.swr.gauge_reader;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PictureFragment.OnPictureFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PictureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PictureFragment extends Fragment{
    public static final String MESSAGE = "MSG";



    // TODO: Rename and change types of parameters

    public static final int START_PLAY = 0;
    public static final int STOP_PLAY = 1;
    public static final int PLAY_FRAME =2;
    public static final int SET_TEMPLATE =3;
    public static final int CLEAR_TEMPLATE =4;
    public static final int TOAST_MESSAGE = 5;

    public static final int STATE_PLAY = 0;
    public static final int STATE_STOP = 1;
    public int mPlayButtonState;

    public static final String MSG_SET_TPL = "SET_TEMPLATE";
    
    public PictureView mPictureView;
    public Button setTemplateButton;
    public Button clearTemplateButton;
    public OnPictureFragmentInteractionListener mListener;

    public Button mPicturePlayButton;

    List<TemplateVariable> templateVariables;
    public PictureFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param args Parameter 1.
     * @return A new instance of fragment PictureFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PictureFragment newInstance(Bundle args) {
        PictureFragment fragment = new PictureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlayButtonState = STATE_PLAY;
        if (getArguments() != null) {
        }
        templateVariables = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View mPictureFragmentView = inflater.inflate(R.layout.fragment_picture, container, false);
        mPictureView = mPictureFragmentView.findViewById(R.id.pictureview);
        mPictureView.setMode(PictureView.MODE_RECT);
        mPicturePlayButton = mPictureFragmentView.findViewById(R.id.play_button);
        setTemplateButton = mPictureFragmentView.findViewById(R.id.set_template);
        clearTemplateButton = mPictureFragmentView.findViewById(R.id.clear_template);
        setTemplateButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                int size = templateVariables.size();
                byte [] bytes =  Util.Int2Bytes(size);
                for(int i = 0; i < size; i++){
                    bytes = Util.BytesConcat(bytes, templateVariables.get(i).getBytes());
                }
                b.putByteArray(MSG_SET_TPL, bytes);
                onInteract(SET_TEMPLATE, b);
            }
        });
        clearTemplateButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                mPictureView.clearRectList();
                mPictureView.clearVectorList();
                templateVariables.clear();
                onInteract(CLEAR_TEMPLATE, null);
            }
        });
        mPicturePlayButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mPlayButtonState == STATE_PLAY){
                    mPicturePlayButton.setText("STOP");
                    mPlayButtonState = STATE_STOP;
                    new Thread(new PlayingThread(30)).start();
                }else if(mPlayButtonState == STATE_STOP){
                    mPicturePlayButton.setText("PLAY");
                    mPlayButtonState = STATE_PLAY;
                }
            }
        });
        clearTemplateButton = mPicturePlayButton.findViewById(R.id.clear_template);
        mPictureView.setOnRectClickListener(new PictureView.OnRectClickListener(){
            @Override
            public void onVectorUpdate(RectF r) {
            }

            @Override
            public void onRectClick(final MotionEvent e){
                final RectF selectedRect = new RectF(mPictureView.getSelectedRect());
                if(selectedRect.left!=selectedRect.right&&selectedRect.top!=selectedRect.bottom) {
                    selectedRect.sort();
                    if(selectedRect.contains(e.getX(),e.getY())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        final View dialogView = LayoutInflater.from(getContext())
                                .inflate(R.layout.template_set_up,null);
                        final PictureView pictureView = dialogView.findViewById(R.id.picture);
                        final EditText gaugeMaxEdit = dialogView.findViewById(R.id.gauge_max_edit);
                        final EditText gaugeMinEdit = dialogView.findViewById(R.id.gauge_min_edit);
                        final EditText gaugeNameEdit = dialogView.findViewById(R.id.gauge_name_edit);
                        final EditText gaugeMaxThEdit = dialogView.findViewById(R.id.max_threshold_edit);
                        final EditText gaugeMinThEdit = dialogView.findViewById(R.id.min_threshold_edit);

                        final RadioButton gaugeMinButton = dialogView.findViewById(R.id.gauge_min_button);
                        final RadioButton gaugeMaxButton = dialogView.findViewById(R.id.gauge_max_button);

                        final Rect pictureRect = mPictureView.subPicture(selectedRect);
                        pictureView.setPicture(Bitmap.createBitmap(mPictureView.getPicture(),
                                pictureRect.left,pictureRect.top,pictureRect.width(),pictureRect.height()));
                        pictureView.setMode(PictureView.MODE_VECTOR);
                        pictureView.setOnRectClickListener(new PictureView.OnRectClickListener(){
                            @Override
                            public void onVectorUpdate(RectF r) {
                                if(gaugeMinButton.isChecked()){
                                    pictureView.setTagVector(PictureView.TagVector.MIN,"MIN",new RectF(r),Color.GREEN);
                                }else if(gaugeMaxButton.isChecked()){
                                    pictureView.setTagVector(PictureView.TagVector.MAX,"MAX",new RectF(r),Color.RED);
                                }
                            }

                            @Override
                            public void onRectClick(MotionEvent e) {
                            }
                        });

                        builder.setIcon(R.drawable.osc);
                        builder.setTitle("Gauge Template");
                        builder.setPositiveButton("Next",null);
                        builder.setNegativeButton("Cancel",null);
                        builder.setView(dialogView);
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String name = gaugeNameEdit.getText().toString();
                                String gaugeMin = gaugeMinEdit.getText().toString();
                                String gaugeMax = gaugeMaxEdit.getText().toString();
                                String gaugeThMin = gaugeMinThEdit.getText().toString();
                                String gaugeThMax = gaugeMaxThEdit.getText().toString();
                                if(name.length() == 0){
                                    toastMessage("Name cannot be null!");
                                    return;
                                }
                                if(gaugeMin.length() == 0){
                                    toastMessage("Gauge minimum value cannot be null!");
                                    return;
                                }
                                if(gaugeMax.length() == 0){
                                    toastMessage("Gauge maximum value cannot be null!");
                                    return;
                                }
                                if(gaugeThMin.length() == 0){
                                    toastMessage("Gauge minimum threshold cannot be null!");
                                    return;
                                }
                                if(gaugeThMax.length() == 0){
                                    toastMessage("Gauge maximum threshold cannot be null!");
                                    return;
                                }
                                double min = Double.parseDouble(gaugeMin);
                                double max = Double.parseDouble(gaugeMax);
                                double minTh = Double.parseDouble(gaugeThMin);
                                double maxTh = Double.parseDouble(gaugeThMax);
                                if(min > max){
                                    toastMessage("Gauge minimum value must be less than Gauge maximum value!");
                                    return;
                                }

                                TemplateVariable tv = new TemplateVariable();
                                tv.name =  name;
                                tv.min =  min;
                                tv.max =  max;
                                tv.min_threshold =  minTh;
                                tv.max_threshold =  maxTh;
                                tv.region = new Rect(pictureRect);
                                tv.region_in_pic = new RectF(selectedRect);
                                PictureView.TagVector minTagVector;
                                PictureView.TagVector maxTagVector;
                                Rect minVec;
                                Rect maxVec;
                                try {
                                    minTagVector = pictureView.getTagVector(PictureView.TagVector.MIN);
                                    maxTagVector = pictureView.getTagVector(PictureView.TagVector.MAX);
                                    minVec = pictureView.subPicture(minTagVector.getVector());
                                    maxVec = pictureView.subPicture(maxTagVector.getVector());
                                }catch (NullPointerException e){
                                    e.printStackTrace();
                                    toastMessage("Please select minimum/maximum needles");
                                    return;
                                }

                                RectF minV = mPictureView.subPicture(minVec);
                                RectF maxV = mPictureView.subPicture(maxVec);
                                tv.center_x = (minV.left + maxV.left)/2;
                                tv.center_y = (minV.top + maxV.top)/2;
                                float length1 = (float)Math.sqrt(Math.pow(minV.right - tv.center_x,2) + Math.pow(minV.bottom - tv.center_y,2));
                                float length2 = (float)Math.sqrt(Math.pow(maxV.right - tv.center_x,2) + Math.pow(maxV.bottom - tv.center_y,2));
                                tv.needle_length = (length1 + length2)/2;
                                tv.center_min_x = minVec.left + pictureRect.left;
                                tv.center_min_y = minVec.top + pictureRect.top;
                                tv.center_max_x = maxVec.left + pictureRect.left;
                                tv.center_max_y = maxVec.top + pictureRect.top;
                                tv.end_min_x = minVec.right + pictureRect.left;
                                tv.end_min_y = minVec.bottom + pictureRect.top;
                                tv.end_max_x = maxVec.right + pictureRect.left;
                                tv.end_max_y = maxVec.bottom + pictureRect.top;
                                templateVariables.add(tv);
                                mPictureView.setTagRect(0,name,selectedRect,Color.BLUE);
                                mPictureView.invalidate();
                                dialog.dismiss();
                            }
                        });

                    }
                }
            }
        });
        return mPictureFragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onInteract(int state_code, Bundle bundle) {
        if (mListener != null) {
            mListener.onPictureFragmentInteraction(state_code, bundle);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPictureFragmentInteractionListener) {
            mListener = (OnPictureFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPictureFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnPictureFragmentInteractionListener {
        // TODO: Update argument type and name
        void onPictureFragmentInteraction(int state_code, Bundle bundle);
    }

    public void toastMessage(String string) {
        if (mListener != null) {
            Bundle bundle = new Bundle();
            bundle.putString(MESSAGE,string);
            mListener.onPictureFragmentInteraction(TOAST_MESSAGE, bundle);
        }
    }

    private class PlayingThread extends Thread {
        private long delay;
        public PlayingThread(long millis) {
            delay = millis;
        }
        public void run() {
            while (mPlayButtonState == STATE_STOP) {
                try {
                    sleep(delay);//毫秒
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onInteract(PLAY_FRAME, null);
            }
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }
    }

    public RectF getGaugeCenter(int index){
        TemplateVariable tv = templateVariables.get(index);
        float cx = tv.region_in_pic.left + tv.center_x;
        float cy = tv.region_in_pic.top + tv.center_y;
        return new RectF(cx,cy,cx,cy);
    }

    public float getNeedleLength(int index){
        TemplateVariable tv = templateVariables.get(index);
        return tv.needle_length;
    }
    public RectF getTemplateRegion(int index){
        TemplateVariable tv = templateVariables.get(index);
        return tv.region_in_pic;
    }
    public String getGaugeName(int index){
        return templateVariables.get(index).name;
    }
    class TemplateVariable {
        public String name;

        public Rect region;
        public RectF region_in_pic;
        public int center_min_x;
        public int center_min_y;
        public int center_max_x;
        public int center_max_y;

        public int end_min_x;
        public int end_min_y;
        public int end_max_x;
        public int end_max_y;
        
        public double min;
        public double min_threshold;
        public double max;
        public double max_threshold;

        public float center_x;
        public float center_y;
        public float needle_length;

        public byte[] getBytes(){
            byte[] res = new byte[0];
            res = Util.BytesConcat(res,Util.Int2Bytes(region.left));
            res = Util.BytesConcat(res,Util.Int2Bytes(region.top));
            res = Util.BytesConcat(res,Util.Int2Bytes(region.right));
            res = Util.BytesConcat(res,Util.Int2Bytes(region.bottom));
            res = Util.BytesConcat(res,Util.Int2Bytes(center_min_x));
            res = Util.BytesConcat(res,Util.Int2Bytes(center_min_y));
            res = Util.BytesConcat(res,Util.Int2Bytes(end_min_x));
            res = Util.BytesConcat(res,Util.Int2Bytes(end_min_y));
            res = Util.BytesConcat(res,Util.Int2Bytes(center_max_x));
            res = Util.BytesConcat(res,Util.Int2Bytes(center_max_y));
            res = Util.BytesConcat(res,Util.Int2Bytes(end_max_x));
            res = Util.BytesConcat(res,Util.Int2Bytes(end_max_y));
            return res;
        }
    }
}
