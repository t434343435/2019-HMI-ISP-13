package com.swr.gauge_reader;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OptionsFragment.OnOptionsFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OptionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OptionsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String GRIDX = "GRIDX";
    public static final String GRIDY = "GRIDY";
    public static final String STYLE = "STYLE";

    public static final int UPDATE_OPTIONS = 1;

    private View mOptionsView;
    private OnOptionsFragmentInteractionListener mListener;

    public OptionsFragment() {
        // Required empty public constructor
        
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OptionsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OptionsFragment newInstance(Bundle args) {
        OptionsFragment fragment = new OptionsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mOptionsView = inflater.inflate(R.layout.fragment_options, container, false);

        int tempGridX =getArguments().getInt(GRIDX,3);
        int tempGridY =getArguments().getInt(GRIDY,4);

        TextView mGridXText =
                (TextView) mOptionsView.findViewById(R.id.gridX);
        TextView mGridYText =
                (TextView) mOptionsView.findViewById(R.id.gridY);


        SeekBar mGridXSeekBar =
                (SeekBar) mOptionsView.findViewById(R.id.gridXSeekBar);
        SeekBar mGridYSeekBar =
                (SeekBar) mOptionsView.findViewById(R.id.gridYSeekBar);

        RadioButton mBlackStyleRadioButton = (RadioButton) mOptionsView.findViewById(R.id.black_style);
        RadioButton mWhiteStyleRadioButton = (RadioButton) mOptionsView.findViewById(R.id.white_style);

        if(getArguments().getInt(STYLE,DataView.STYLE_BLACK) == DataView.STYLE_BLACK)
            mBlackStyleRadioButton.setChecked(true);
        else
            mWhiteStyleRadioButton.setChecked(true);

        mGridXSeekBar.setProgress(tempGridX);
        mGridYSeekBar.setProgress(tempGridY);

        mGridXText.setText("x轴的网格数:"+tempGridX);
        mGridYText.setText("y轴的网格数:"+tempGridY);
        mBlackStyleRadioButton.setOnClickListener(new RadioButton.OnClickListener(){
            @Override
            public void onClick(View view) {
                getArguments().putInt(STYLE,DataView.STYLE_BLACK);
                onOptionChanged();
            }
        });
        mWhiteStyleRadioButton.setOnClickListener(new RadioButton.OnClickListener(){
            @Override
            public void onClick(View view) {
                getArguments().putInt(STYLE,DataView.STYLE_WHITE);
                onOptionChanged();
            }
        });
        mGridXSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView mGridXText =
                        (TextView) mOptionsView.findViewById(R.id.gridX);
                getArguments().putInt(GRIDY,progress);
                onOptionChanged();
                mGridXText.setText("x轴的网格数:" + progress);
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
                        (TextView) mOptionsView.findViewById(R.id.gridY);
                getArguments().putInt(GRIDX,progress);
                onOptionChanged();
                mGridYText.setText("y轴的网格数:"+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void  onStopTrackingTouch(SeekBar seekBar) {}
        });
        return mOptionsView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(int state_code, Bundle bundle) {
        if (mListener != null) {
            mListener.onOptionsFragmentInteraction(state_code, bundle);
        }
    }

    public void onOptionChanged() {
        if (mListener != null) {
            mListener.onOptionsFragmentInteraction(UPDATE_OPTIONS, getArguments());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOptionsFragmentInteractionListener) {
            mListener = (OnOptionsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOptionsFragmentInteractionListener");
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
    public interface OnOptionsFragmentInteractionListener {
        // TODO: Update argument type and name
        void onOptionsFragmentInteraction(int state_code, Bundle bundle);
    }
}
