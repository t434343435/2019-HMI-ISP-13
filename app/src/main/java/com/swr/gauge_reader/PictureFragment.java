package com.swr.gauge_reader;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PictureFragment.OnPictureFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PictureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PictureFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    public static final int START_PLAY = 0;
    public static final int STOP_PLAY = 1;

    public static final int STATE_PLAY = 0;
    public static final int STATE_STOP = 1;
    public int mPlayButtonState;

    public PictureView mPictureView;
    private OnPictureFragmentInteractionListener mListener;

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
}
