package com.tushar.cmspen2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;


class CropView extends View implements View.OnTouchListener {

    Point corners[];
    Context mContext;
    Paint mPaint;
    Path mPath;
    int cID = -1;
    static int CORNER_TOLERANCE;
    static int REDRAW_TOLERANCE;
    static int ANCHOR_BORDER;
    static int ANCHOR_INNER;
    static int ANCHOR_LINE_WIDTH;
    static int ANCHOR_ARROW_HEIGHT;
    static int ANCHOR_ARROW_WIDTH;
    static int CIRCLE_INNER;
    static int mWidth;
    static int mHeight;
    static int minWidth;
    static int minHeight;
    DashPathEffect dotted = new DashPathEffect(new float[]{10, 20}, 0);

    public CropView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    private void init()
    {
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        mWidth = getWidth();
        mHeight = getHeight();
        int left = mWidth / 6;
        int top = mHeight / 6;
        int right = left * 5;
        int bottom = top * 5;
        corners = new Point[4];
        corners[0] = new Point(left, top);
        corners[1] = new Point(right, top);
        corners[2] = new Point(left, bottom);
        corners[3] = new Point(right, bottom);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        minWidth = (int)(0.1f * size.x);
        minHeight = (int)(0.1f * size.y);
        CORNER_TOLERANCE = (int)(0.028f * size.x);
        REDRAW_TOLERANCE = (int)(0.12f * CORNER_TOLERANCE);
        ANCHOR_BORDER = (int)(1.25f * CORNER_TOLERANCE);
        ANCHOR_INNER = (int)(1.15f * CORNER_TOLERANCE);
        ANCHOR_LINE_WIDTH = (int)(0.1f * CORNER_TOLERANCE);
        ANCHOR_ARROW_HEIGHT = (int)(0.55f * CORNER_TOLERANCE);
        ANCHOR_ARROW_WIDTH = (int)(0.4f * CORNER_TOLERANCE);
        CIRCLE_INNER = (int)(0.85f * CORNER_TOLERANCE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(corners != null) {
            //draw background
            canvas.clipRect(corners[0].x, corners[0].y, corners[3].x, corners[3].y, Region.Op.DIFFERENCE);
            canvas.drawARGB(80, 0, 0, 0);
            canvas.clipRect(0, 0, canvas.getWidth(), canvas.getHeight()
                    , Region.Op.REPLACE);

            //draw boundary
            mPaint.setPathEffect(dotted);
            canvas.drawRect(corners[0].x, corners[0].y, corners[3].x, corners[3].y, mPaint);
            mPaint.setPathEffect(null);

            //draw circles
            mPaint.setStyle(Paint.Style.FILL);
            for (int i = 1; i < 4; i++) {
                mPaint.setColor(Color.BLACK);
                canvas.drawCircle(corners[i].x, corners[i].y, CORNER_TOLERANCE, mPaint);
                mPaint.setColor(Color.WHITE);
                canvas.drawCircle(corners[i].x, corners[i].y, CIRCLE_INNER, mPaint);
            }

            //draw anchor
            mPaint.setColor(Color.BLACK);
            canvas.drawRect(corners[0].x - ANCHOR_BORDER, corners[0].y - ANCHOR_BORDER,
                    corners[0].x + ANCHOR_BORDER, corners[0].y + ANCHOR_BORDER, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(corners[0].x - ANCHOR_INNER, corners[0].y - ANCHOR_INNER,
                    corners[0].x + ANCHOR_INNER, corners[0].y + ANCHOR_INNER, mPaint);
            mPaint.setColor(Color.BLACK);
            canvas.drawRect(corners[0].x - CORNER_TOLERANCE, corners[0].y - ANCHOR_LINE_WIDTH,
                    corners[0].x + CORNER_TOLERANCE, corners[0].y + ANCHOR_LINE_WIDTH, mPaint);
            canvas.drawRect(corners[0].x - ANCHOR_LINE_WIDTH, corners[0].y - CORNER_TOLERANCE,
                    corners[0].x + ANCHOR_LINE_WIDTH, corners[0].y + CORNER_TOLERANCE, mPaint);

            mPath.reset();
            mPath.moveTo(corners[0].x - ANCHOR_INNER, corners[0].y);
            mPath.lineTo(corners[0].x - ANCHOR_ARROW_HEIGHT, corners[0].y + ANCHOR_ARROW_WIDTH);
            mPath.lineTo(corners[0].x - ANCHOR_ARROW_HEIGHT, corners[0].y - ANCHOR_ARROW_WIDTH);
            mPath.close();
            canvas.drawPath(mPath, mPaint);

            mPath.reset();
            mPath.moveTo(corners[0].x + ANCHOR_INNER, corners[0].y);
            mPath.lineTo(corners[0].x + ANCHOR_ARROW_HEIGHT, corners[0].y + ANCHOR_ARROW_WIDTH);
            mPath.lineTo(corners[0].x + ANCHOR_ARROW_HEIGHT, corners[0].y - ANCHOR_ARROW_WIDTH);
            mPath.close();
            canvas.drawPath(mPath, mPaint);

            mPath.reset();
            mPath.moveTo(corners[0].x, corners[0].y - ANCHOR_INNER);
            mPath.lineTo(corners[0].x + ANCHOR_ARROW_WIDTH, corners[0].y - ANCHOR_ARROW_HEIGHT);
            mPath.lineTo(corners[0].x - ANCHOR_ARROW_WIDTH, corners[0].y - ANCHOR_ARROW_HEIGHT);
            mPath.close();
            canvas.drawPath(mPath, mPaint);

            mPath.reset();
            mPath.moveTo(corners[0].x, corners[0].y + ANCHOR_INNER);
            mPath.lineTo(corners[0].x + ANCHOR_ARROW_WIDTH, corners[0].y + ANCHOR_ARROW_HEIGHT);
            mPath.lineTo(corners[0].x - ANCHOR_ARROW_WIDTH, corners[0].y + ANCHOR_ARROW_HEIGHT);
            mPath.close();
            canvas.drawPath(mPath, mPaint);

            //restore
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                for(int i = 0; i < 4; i++)
                {
                    if(Math.abs(corners[i].x - (int)x) <= CORNER_TOLERANCE)
                        if(Math.abs(corners[i].y - (int)y) <= CORNER_TOLERANCE)
                        {
                            cID = i;
                        }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(cID != -1)
                {
                    int dx = corners[cID].x - (int) x;
                    int dy = corners[cID].y - (int) y;
                    if(Math.abs(dx) >= REDRAW_TOLERANCE || Math.abs(dy) >= REDRAW_TOLERANCE)
                    {
                        Point[] temp = new Point[corners.length];
                        for (int i = temp.length - 1; i >= 0; --i) {
                            Point p = corners[i];
                            if (p != null) {
                                temp[i] = new Point(p);
                            }
                        }
                        switch(cID)
                        {
                            case 0:
                                for(Point p: temp)
                                {
                                    p.x -= dx;
                                    p.y -= dy;
                                }
                                break;
                            case 1:
                                temp[1].x -= dx;
                                temp[1].y -= dy;
                                temp[0].y -= dy;
                                temp[3].x -= dx;
                                break;
                            case 2:
                                temp[2].x -= dx;
                                temp[2].y -= dy;
                                temp[3].y -= dy;
                                temp[0].x -= dx;
                                break;
                            case 3:
                                temp[3].x -= dx;
                                temp[3].y -= dy;
                                temp[2].y -= dy;
                                temp[1].x -= dx;
                                break;
                        }
                        for(Point p: temp)
                        {
                            if(p.x > mWidth)
                                p.x = mWidth;
                            if(p.x < 0)
                                p.x = 0;
                            if(p.y > mHeight)
                                p.y = mHeight;
                            if(p.y < 0)
                                p.y = 0;
                        }
                        if(temp[3].x - temp[0].x >= minWidth)
                        {
                            for(int i = 0; i < 4; i++)
                            {
                                corners[i].x = temp[i].x;
                            }
                        }
                        if(temp[3].y - temp[0].y >= minHeight)
                        {
                            for(int i = 0; i < 4; i++)
                            {
                                corners[i].y = temp[i].y;
                            }
                        }
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                cID = -1;
                break;
        }
        return true;
    }
}
