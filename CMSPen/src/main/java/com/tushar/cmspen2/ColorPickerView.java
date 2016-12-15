package com.tushar.cmspen2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

class ColorPickerView extends View {
    private Paint mPaint;
    private float mCurrentHue = 0;
    private int mCurrentX = 0, mCurrentY = 0;
    private int mCurrentColor;
    private final int[] mHueBarColors = new int[258];
    private int[] mMainColors = new int[65536];
    private OnColorChangedListener mListener;
    private int mWidth;
    private int margin = 10;
    private float hueSpacing;
    private float yHueSpacing;
    private int selectorHeight;
    private int mainHeight;
    private int touchRegionID = -1;
    private float sliderX;
    private float sliderWidth;
    private float sliderHeight;
    private float sliderSpacing;
    private Paint sliderPaint;
    private int sliderOffset;

    public interface OnColorChangedListener {
        void colorChanged(int color, int sliderOffset);
    }

    ColorPickerView(Context c, OnColorChangedListener l, int color,
                    int sliderOffset, int margin) {
        super(c);
        mListener = l;
        this.margin = margin;
        this.sliderOffset = sliderOffset;
        initialize();

        // Get the current hue from the current color and update the main
        // color field
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mCurrentHue = hsv[0];
        updateMainColors();

        mCurrentColor = color;

        // Initialize the colors of the hue slider bar
        int index = 0;
        for (float i = 0; i < 256; i += 256 / 42) // Red (#f00) to pink
        // (#f0f)
        {
            mHueBarColors[index] = Color.rgb(255, 0, (int) i);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) // Pink (#f0f) to blue
        // (#00f)
        {
            mHueBarColors[index] = Color.rgb(255 - (int) i, 0, 255);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) // Blue (#00f) to light
        // blue (#0ff)
        {
            mHueBarColors[index] = Color.rgb(0, (int) i, 255);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) // Light blue (#0ff) to
        // green (#0f0)
        {
            mHueBarColors[index] = Color.rgb(0, 255, 255 - (int) i);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) // Green (#0f0) to yellow
        // (#ff0)
        {
            mHueBarColors[index] = Color.rgb((int) i, 255, 0);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) // Yellow (#ff0) to red
        // (#f00)
        {
            mHueBarColors[index] = Color.rgb(255, 255 - (int) i, 0);
            index++;
        }

        // Initializes the Paint that will draw the View
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(12);
        
        sliderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sliderPaint.setStyle(Paint.Style.STROKE);
    }

    // Get the current selected color from the hue bar
    private int getCurrentMainColor() {
        int translatedHue = 255 - (int) (mCurrentHue * 255 / 360);
        int index = 0;
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb(255, 0, (int) i);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb(255 - (int) i, 0, 255);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb(0, (int) i, 255);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb(0, 255, 255 - (int) i);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb((int) i, 255, 0);
            index++;
        }
        for (float i = 0; i < 256; i += 256 / 42) {
            if (index == translatedHue)
                return Color.rgb(255, 255 - (int) i, 0);
            index++;
        }
        return Color.RED;
    }

    // Update the main field colors depending on the current selected hue
    private void updateMainColors() {
        int mainColor = getCurrentMainColor();
        int index = 0;
        int[] topColors = new int[256];
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                if (y == 0) {
                    mMainColors[index] = Color.rgb(
                            255 - (255 - Color.red(mainColor)) * x / 255,
                            255 - (255 - Color.green(mainColor)) * x / 255,
                            255 - (255 - Color.blue(mainColor)) * x / 255);
                    topColors[x] = mMainColors[index];
                } else
                    mMainColors[index] = Color.rgb(
                            (255 - y) * Color.red(topColors[x]) / 255,
                            (255 - y) * Color.green(topColors[x]) / 255,
                            (255 - y) * Color.blue(topColors[x]) / 255);
                index++;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(160, 0, 0, 0);
        int translatedHue = 255 - (int) (mCurrentHue * 255 / 360);
        // Display all the colors of the hue bar with lines
        for (int x = 0; x < 256; x++) {
            // If this is not the current selected hue, display the actual
            // color
            if (translatedHue != x) {
                mPaint.setColor(mHueBarColors[x]);
                mPaint.setStrokeWidth(hueSpacing * 1.5f);
            } else // else display a slightly larger black line
            {
                mPaint.setColor(Color.BLACK);
                mPaint.setStrokeWidth(hueSpacing * 3);
            }
            float effectiveX = x * hueSpacing;
            canvas.drawLine(effectiveX + margin, margin, effectiveX + margin, selectorHeight, mPaint);
            // canvas.drawLine(0, x+10, 40, x+10, mPaint);
        }

        // Display the main field colors using LinearGradient
        for (int x = 0; x < 256; x++) {
            int[] colors = new int[2];
            colors[0] = mMainColors[x];
            colors[1] = Color.BLACK;
            Shader shader = new LinearGradient(0, selectorHeight + margin, 0, mainHeight, colors, null,
                    Shader.TileMode.REPEAT);
            mPaint.setShader(shader);
            float effectiveX = x * hueSpacing;
            canvas.drawLine(effectiveX + margin, selectorHeight + margin, effectiveX + margin, mainHeight, mPaint);
        }
        mPaint.setShader(null);

        // Display the circle around the currently selected color in the
        // main field
        if (mCurrentX != 0 && mCurrentY != 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.BLACK);
            mPaint.setStrokeWidth(hueSpacing);
            canvas.drawCircle(mCurrentX, mCurrentY, margin, mPaint);
        }

        sliderPaint.setStrokeCap(Paint.Cap.BUTT);
        sliderPaint.setColor(Color.parseColor("#444444"));
        sliderPaint.setStrokeWidth(8);
        canvas.drawLine(2 * margin, sliderHeight, sliderWidth, sliderHeight, sliderPaint);

        sliderPaint.setColor(Color.parseColor("#dddddd"));
        sliderPaint.setStrokeWidth(15);
        sliderPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(sliderX, sliderHeight - 0.75f * margin, sliderX, sliderHeight + 0.75f * margin, sliderPaint);

        sliderPaint.setColor(mCurrentColor);
        sliderPaint.setStrokeWidth(DrawView.drawWidthMap[sliderOffset]);
        canvas.drawPoint(mWidth - 1.85f * sliderSpacing, sliderHeight, sliderPaint);
    }

    private void initialize()
    {
        mWidth = getWidth();
        int mHeight = getHeight();
        hueSpacing = (mWidth - 2 * margin) / 256f;
        selectorHeight = (int)(0.11f * mHeight) + margin;
        mainHeight = (int)(0.836f * mHeight);
        yHueSpacing = (mainHeight - selectorHeight - margin) / 256f;
        sliderSpacing = (mWidth - 2 * margin) / 14f;
        sliderX = 2 * margin + sliderSpacing * 0.5f + sliderSpacing * sliderOffset;
        sliderWidth = 2 * margin + sliderSpacing * 10f;
        sliderHeight = mainHeight + ((mHeight - mainHeight) / 2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initialize();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_MOVE)
            return true;
        float x = event.getX();
        float y = event.getY();

        if(action == MotionEvent.ACTION_UP && touchRegionID != 2)
            touchRegionID = -1;

        // If the touch event is located in the hue bar
        if (x > margin && x < mWidth - margin && y > margin && y < selectorHeight) {

            if(touchRegionID == -1)
                touchRegionID = 0;
            if(touchRegionID == 0)
            {
                mCurrentHue = (255 - ((x - margin) / hueSpacing)) * 360 / 255;
                updateMainColors();

                // Update the current selected color
                int transX = (int)((mCurrentX / hueSpacing) - margin);
                int transY = (int)((mCurrentY - selectorHeight - margin)/ yHueSpacing);
                int index = 256 * (transY - 1) + transX;
                if (index > 0 && index < mMainColors.length)
                    mCurrentColor = mMainColors[index];

                // Force the redraw of the dialog
                invalidate();
            }
                //return true;

            // Update the main field colors

        }

        // If the touch event is located in the main field
        if (x > margin && x < mWidth - margin && y > selectorHeight + margin && y < mainHeight) {

            if(touchRegionID == -1)
                touchRegionID = 1;
            if(touchRegionID == 1)
            {
                mCurrentX = (int) x;
                mCurrentY = (int) y;
                int transX = (int)((mCurrentX / hueSpacing) - margin);
                int transY = (int)((mCurrentY - selectorHeight - margin)/ yHueSpacing);
                int index = 256 * (transY - 1) + transX;
                if (index > 0 && index < mMainColors.length) {
                    // Update the current color
                    mCurrentColor = mMainColors[index];
                    // Force the redraw of the dialog
                    invalidate();
                }
            }
                //return true;


        }

        // If the touch event is located in the left button, notify the
        // listener with the current color
        //if (x > margin && x < mWidth / 2 && y > mainHeight + margin && y < mHeight - margin)
            //mListener.colorChanged(/*"", */mCurrentColor);

        // If the touch event is located in the right button, notify the
        // listener with the default color
        //if (x > mWidth / 2 && x < mWidth - margin && y > mainHeight + margin && y < mHeight - margin)
            //mListener.colorChanged(/*"", */mDefaultColor);

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
            mListener.colorChanged(mCurrentColor, sliderOffset);
        }

        return true;
    }
}
