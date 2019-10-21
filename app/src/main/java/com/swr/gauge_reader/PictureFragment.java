package com.swr.gauge_reader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;

import static com.swr.gauge_reader.DataFragment.MESSAGE;
import static com.swr.gauge_reader.DataFragment.TOAST_MESSAGE;


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

    public static final int TOAST_MESSAGE = 3;
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    public static final String ARG_PARAM1 = "param1";
//    public static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
//    public String mParam1;
//    public String mParam2;

    public static final int START_PLAY = 0;
    public static final int STOP_PLAY = 1;

    public static final int STATE_PLAY = 0;
    public static final int STATE_STOP = 1;
    public int mPlayButtonState;

    public PictureView mPictureView;
    public OnPictureFragmentInteractionListener mListener;

    public Button mPicturePlayButton;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mPictureFragmentView = inflater.inflate(R.layout.fragment_picture, container, false);
        mPictureView = mPictureFragmentView.findViewById(R.id.pictureview);
        mPictureView.setMode(PictureView.MODE_RECT);
        mPicturePlayButton = mPictureFragmentView.findViewById(R.id.play_button);
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

                        Rect pictureRect = mPictureView.subPicture(selectedRect);
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

//                        gaugeMaxButton.setScrollBarStyle();

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
                                double maxTh = Double.parseDouble(gaugeThMin);
                                if(min > max){
                                    toastMessage("Gauge minimum value must be less than Gauge maximum value!");
                                    return;
                                }
                                mPictureView.setTagRect(0,name,selectedRect,Color.BLUE);
                                mPictureView.invalidate();
                                dialog.dismiss();
                            }
                        });

                    }
                }
            }
        });
        mPicturePlayButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mPlayButtonState == STATE_PLAY){
                    onInteract(START_PLAY, null);
                    mPicturePlayButton.setText("STOP");
                    mPlayButtonState = STATE_STOP;
                }else if(mPlayButtonState == STATE_STOP){
                    onInteract(STOP_PLAY, null);
                    mPicturePlayButton.setText("PLAY");
                    mPlayButtonState = STATE_PLAY;
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
    class TemplateVariable{
        public String name;

        public RectF region;

        public float center_x;
        public float center_y;

        public float min;
        public float min_ang;
        public float min_threshold;
        public float max;
        public float max_ang;
        public float max_threshold;

    }
}
