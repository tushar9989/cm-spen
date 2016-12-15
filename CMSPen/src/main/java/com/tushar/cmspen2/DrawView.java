package com.tushar.cmspen2;

import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

class DrawView extends View implements OnTouchListener, View.OnHoverListener {
    private Path    mPath;
    private Paint       mPaint;
    private ArrayList<Path> paths = new ArrayList<Path>();
    private ArrayList<Integer> colors = new ArrayList<Integer>();
    private ArrayList<Boolean> erased = new ArrayList<Boolean>();
    private ArrayList<Integer> widths = new ArrayList<Integer>();
    private ArrayList<Path> undonePaths = new ArrayList<Path>();
    private ArrayList<Integer> undoneColors = new ArrayList<Integer>();
    private ArrayList<Boolean> undoneErased = new ArrayList<Boolean>();
    private ArrayList<Integer> undoneWidths = new ArrayList<Integer>();
    private int chosenColor;
    private int drawWidth;
    private int eraseWidth;
    private boolean ERASE_MODE = false;
    private Paint erasePaint, eraseHover;
    private float hX, hY;
    private boolean hoverTimeout = false;
    private long lastEventTime = -1;
    private boolean checking = true;
    private Bitmap hoverPen;
    public static int drawWidthMap[] = {5, 10, 15, 20, 25, 35, 45, 55, 65, 75};
    private int drawWidthOffset = 1;
    public static int eraseWidthMap[] = {30, 40, 50, 60, 70, 80, 90, 100, 125, 150};
    private int eraseWidthOffset = 2;
    private float scale = 1.0f;
    private boolean edit_hover_enable;

    public DrawView(Context context)
    {
        super(context);
        initialize();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize();
    }

    public DrawView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize();
    }

    public void setScale(float scale)
    {
        this.scale = scale;
        initialize();
    }

    void initialize()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        chosenColor = pref.getInt("draw_color", Color.RED);
        drawWidthOffset = pref.getInt("draw_width", 1);
        drawWidth = (int)(drawWidthMap[drawWidthOffset] * scale);
        eraseWidthOffset = pref.getInt("erase_width", 2);
        eraseWidth = (int)(eraseWidthMap[eraseWidthOffset] * scale);

        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        this.setOnHoverListener(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(chosenColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(drawWidthMap[drawWidthOffset] * scale);
        setLayerType(View.LAYER_TYPE_SOFTWARE, mPaint);

        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setDither(true);
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeWidth(eraseWidthMap[eraseWidthOffset] * scale);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        eraseHover = new Paint();
        eraseHover.setAntiAlias(true);
        eraseHover.setStrokeWidth(1);

        hoverPen = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_edit);
        edit_hover_enable = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("edit_hover_enable", true);
    }

    void setColor(int color)
    {
        mPaint.setColor(color);
        ERASE_MODE = false;
        chosenColor = color;
    }

    int getChosenColor()
    {
        return chosenColor;
    }

    void eraserMode()
    {
        ERASE_MODE = true;
    }

    void setWidth(int drawWidthOffset)
    {
        mPaint.setStrokeWidth(drawWidthMap[drawWidthOffset] * scale);
        this.drawWidthOffset = drawWidthOffset;
        drawWidth = (int)(drawWidthMap[drawWidthOffset] * scale);
    }

    int getDrawWidthOffset()
    {
        return drawWidthOffset;
    }

    void setEraserWidth(int eraseWidthOffset)
    {
        erasePaint.setStrokeWidth(eraseWidthMap[eraseWidthOffset] * scale);
        eraseWidth = (int)(eraseWidthMap[eraseWidthOffset] * scale);
        this.eraseWidthOffset = eraseWidthOffset;
    }

    int getEraseWidthOffset()
    {
        return eraseWidthOffset;
    }

    void clearScreen()
    {
        paths.clear();
        undonePaths.clear();
        widths.clear();
        undoneWidths.clear();
        colors.clear();
        undoneColors.clear();
        erased.clear();
        undoneErased.clear();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(paths.isEmpty())
            canvas.drawARGB(0, 0, 0, 0);
        int temp = paths.size();
        for(int i = 0; i < temp; i++)
        {
            mPaint.setColor(colors.get(i));
            if(erased.get(i))
            {
                erasePaint.setStrokeWidth(widths.get(i));
                canvas.drawPath(paths.get(i), erasePaint);
            }
            else
            {
                mPaint.setStrokeWidth(widths.get(i));
                canvas.drawPath(paths.get(i), mPaint);
            }
        }
        mPaint.setColor(chosenColor);
        mPaint.setStrokeWidth(drawWidth);
        erasePaint.setStrokeWidth(eraseWidth);

        if(!hoverTimeout) {
            if (ERASE_MODE) {
                eraseHover.setColor(Color.argb(80, 0, 0, 0));
                canvas.drawCircle(hX, hY, erasePaint.getStrokeWidth() / 2, eraseHover);
                eraseHover.setColor(Color.argb(125, 255, 255, 255));
                canvas.drawCircle(hX, hY, erasePaint.getStrokeWidth() / 2 - 3, eraseHover);
            } else {
                if(edit_hover_enable)
                    canvas.drawBitmap(hoverPen, hX, hY - hoverPen.getHeight(), mPaint);
                canvas.drawPoint(hX, hY, mPaint);
            }
            hoverTimeout = true;
        }
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y)
    {
        undonePaths.clear();
        undoneColors.clear();
        undoneErased.clear();
        paths.add(new Path());
        colors.add(mPaint.getColor());
        erased.add(ERASE_MODE);
        if(ERASE_MODE)
            widths.add(eraseWidth);
        else
            widths.add(drawWidth);
        mPath = paths.get(paths.size() - 1);
        mPath.moveTo(x - 0.1f, y - 0.1f);
        mPath.lineTo(x, y);
        mX = x;
        mY = y;
        hoverTimeout = false;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        hoverTimeout = false;
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = hX = x;
            mY = hY = y;
        }
    }
    private void touch_up() {
        hoverTimeout = false;
        mPath.lineTo(mX, mY);
    }

    public void onClickUndo () {
        if (paths.size() > 0)
        {
            undonePaths.add(paths.remove(paths.size() - 1));
            undoneColors.add(colors.remove(colors.size() - 1));
            undoneErased.add(erased.remove(erased.size() - 1));
            undoneWidths.add(widths.remove(widths.size() - 1));
            invalidate();
        }
    }

    public void onClickRedo (){
        if (undonePaths.size() > 0)
        {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            colors.add(undoneColors.remove(undoneColors.size() - 1));
            erased.add(undoneErased.remove(undoneErased.size() - 1));
            widths.add(undoneWidths.remove(undoneWidths.size() - 1));
            invalidate();
        }
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        lastEventTime = System.currentTimeMillis();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        new Thread() {
            @Override
            public void run() {
                super.run();
                while(checking)
                {
                    try {
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    if(lastEventTime != -1) {
                        long dt = System.currentTimeMillis() - lastEventTime;
                        if (dt >= 500 && hoverTimeout) {
                            postInvalidate();
                        }
                    }
                }
            }
        }.start();
        handlingButton = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        checking = false;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt("draw_color", chosenColor);
        edit.putInt("draw_width", drawWidthOffset);
        edit.putInt("erase_width", eraseWidthOffset);
        edit.apply();
        handlingButton = false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if(visibility == View.GONE || visibility == INVISIBLE)
            handlingButton = false;
    }

    boolean buttonPressed = false;
    static boolean handlingButton = false;
    @Override
    public boolean onHover(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if(event.getButtonState() == MotionEvent.BUTTON_SECONDARY)
        {
            if(!buttonPressed) {
                ERASE_MODE = !ERASE_MODE;
                invalidate();
                buttonPressed = true;
            }
        }
        else {
            buttonPressed = false;
        }

        lastEventTime = System.currentTimeMillis();
        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                hoverTimeout = false;
                break;
            case MotionEvent.ACTION_HOVER_MOVE:
                hoverTimeout = false;
                float dx = Math.abs(x - hX);
                float dy = Math.abs(y - hY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    hX = x;
                    hY = y;
                }
                invalidate();
                break;
        }
        return true;
    }
}