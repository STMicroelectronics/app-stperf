package com.stmicroelectronics.stperf;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

public class CustomButton extends AppCompatButton {

    private final OnClickListener mListener;

    public CustomButton(Context context) {
        super(context);
        mListener = null;
    }

    public CustomButton(Context context, OnClickListener listener) {
        super(context);
        mListener = listener;
    }

    public CustomButton(Context context, AttributeSet attrs, OnClickListener listener) {
        super(context, attrs);
        mListener = listener;
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr, OnClickListener listener) {
        super(context, attrs, defStyleAttr);
        mListener = listener;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (mListener != null) {
            mListener.onClick();
        }
        return true;
    }

    public interface OnClickListener {
        void onClick();
    }
}
