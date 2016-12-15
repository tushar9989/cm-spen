package com.tushar.cmspen2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

class EraserView extends View implements View.OnTouchListener {

    private float sliderX;
    private float sliderWidth;
    private float sliderHeight;
    private float sliderSpacing;
    private Paint sliderPaint;
    private int sliderOffset;
    private OnWidthChangedListener mListener;
    int margin;
    private int mWidth;
    private int touchRegionID = -1;

    public interface OnWidthChangedListener {
        void widthChanged(int width);
    }

    public EraserView(Context context, int margin, int sliderOffset, OnWidthChangedListener l) {
        super(context);
        mListener = l;
        this.margin = margin;
        this.sliderOffset = sliderOffset;
        init();
    }

    public EraserView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EraserView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    private void init() {
        sliderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sliderPaint.setStyle(Paint.Style.STROKE);

        this.setFocusableInTouchMode(true);
        this.setOnTouchListener(this);

        mWidth = getWidth();
        int mHeight = getHeight();
        sliderSpacing = (mWidth - 2 * margin) / 14f;
        sliderX = 2 * margin + sliderSpacing * 0.5f + sliderSpacing * sliderOffset;
        sliderWidth = 2 * margin + sliderSpacing * 10f;
        sliderHeight = mHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawARGB(160, 0, 0, 0);
        sliderPaint.setStrokeCap(Paint.Cap.BUTT);
        sliderPaint.setColor(Color.parseColor("#444444"));
        sliderPaint.setStrokeWidth(8);
        canvas.drawLine(2 * margin, sliderHeight, sliderWidth, sliderHeight, sliderPaint);

        sliderPaint.setColor(Color.parseColor("#dddddd"));
        sliderPaint.setStrokeWidth(15);
        sliderPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(sliderX, sliderHeight - 0.75f * margin, sliderX, sliderHeight + 0.75f * margin, sliderPaint);

        sliderPaint.setColor(Color.WHITE);
        sliderPaint.setStrokeWidth(DrawView.eraseWidthMap[sliderOffset]);
        canvas.drawPoint(mWidth - 1.85f * sliderSpacing, sliderHeight, sliderPaint);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_MOVE)
            return true;
        float x = event.getX();
        float y = event.getY();

        if(action == MotionEvent.ACTION_DOWN)
        {
            if(Math.abs(x - sliderHeight) >= 2 * margin)
            {
                if(touchRegionID == -1)
                    touchRegionID = 2;
                if(touchRegionID == 2)
                {
                    if(x >= margin * 2 && x <= sliderWidth )
                    {
                        sliderX = x;
                        float offset = sliderX - 2 * margin;
                        sliderOffset = (int)(offset / sliderSpacing);
                    }
                    invalidate();
                }
                //return true;

            }
        }

        if(action == MotionEvent.ACTION_MOVE && touchRegionID == 2)
        {
            if(x >= margin * 2 && x <= sliderWidth)
            {
                sliderX = x;
                float offset = sliderX - 2 * margin;
                sliderOffset = (int)(offset / sliderSpacing);
            }
            invalidate();
        }

        if(action == MotionEvent.ACTION_UP && touchRegionID == 2)
        {
            touchRegionID = -1;
            float offset = sliderX - 2 * margin;
            sliderOffset = (int)(offset / sliderSpacing);
            sliderX = 2 * margin + sliderSpacing * 0.5f + sliderSpacing * sliderOffset;
        }

        //canvas.drawPoint(mWidth - 1.85f * sliderSpacing, sliderHeight, sliderPaint);
        if(Math.abs(x - mWidth + 1.85f * sliderSpacing) <= 70 && Math.abs(y - sliderHeight) <= 70)
        {
            if(touchRegionID == -1)
                touchRegionID = 3;
            if(touchRegionID != 3)
                return true;
            mListener.widthChanged(sliderOffset);
        }

        return true;
    }
}
