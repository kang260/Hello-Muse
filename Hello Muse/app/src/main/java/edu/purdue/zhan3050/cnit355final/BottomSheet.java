package edu.purdue.zhan3050.cnit355final;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * A class for BottomSheetDialogFragment with three bottoms
 */
public class BottomSheet extends BottomSheetDialogFragment {
    private BottomSheetListener mListener;

    Button button5, button1, buttonDemo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.bottom_layout, container, false);

        button5 = v.findViewById(R.id.sleep30);
        button1 = v.findViewById(R.id.sleep15);
        buttonDemo = v.findViewById(R.id.sleepDemo);

        button5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

            }
        });
        button5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mListener.onBottonClicked("300000");
                dismiss();
            }
        });
        button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mListener.onBottonClicked("60000");
                dismiss();
            }
        });
        buttonDemo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mListener.onBottonClicked("1000");
                dismiss();
            }
        });

        return v;
    }

    public interface BottomSheetListener{
        void onBottonClicked(String Text);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        try{
            mListener =(BottomSheetListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString());
        }
    }



}