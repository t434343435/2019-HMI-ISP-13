package com.swr.gauge_reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by t4343 on 2018/3/31.
 */

public class MainView extends AppCompatImageView {
    double[] data = null;
    long[] time = new long[0];

    final float INITIAL_SCALE_VALUE = (float)0.9;
    final int STYLE_BLACK = 0;
    final int STYLE_WHITE = 1;

    private float mContentWidth;
    private float mContentHeight;
    private float mOriginX = mContentWidth*(1-INITIAL_SCALE_VALUE)/2;
    private float mOriginY = mContentHeight*(1-INITIAL_SCALE_VALUE)/2;

    private boolean isScale = false;
    private float mScaleX = INITIAL_SCALE_VALUE;
    private float mScaleY = INITIAL_SCALE_VALUE;
    private int capturedX = -1;

    public int gridX = 4;
    public int gridY = 3;
    public int style = STYLE_BLACK;

     //定义GestureDetector类
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(data.length == time.length) {
                int mDataSize = data.length;
                int x = (int) ((e.getX() - mOriginX) / mContentWidth * (mDataSize - 1) / mScaleX);
                if (x >= 0 && x < mDataSize) {
                    capturedX = x;
                    invalidate();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(!isScale) {
                mOriginX -= distanceX;
                mOriginY -= distanceY;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            resetScaleOrigin();
            //Log.d("tuacy", "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return true;
        }
    };
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event)||mScaleGestureDetector.onTouchEvent(event);
    }
    public MainView(Context context) {
        super(context);
        init(context);
    }

    public MainView(Context context,AttributeSet attrs) {
        super(context,attrs);
        init(context);
    }

    public MainView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean	onScale(ScaleGestureDetector detector){
                if(detector.getCurrentSpanX()>detector.getCurrentSpanY()){
                    float scale = detector.getScaleFactor();
                    mOriginX -= ((int)detector.getFocusX() - mOriginX)*(scale-1);
                    mScaleX *= scale;

                }else {
                    float scale = detector.getScaleFactor();
                    mOriginY -= ((int)detector.getFocusY() - mOriginY)*(scale-1);
                    mScaleY *= scale;

                }
                invalidate();
                return true;
            }
            @Override
            public boolean	onScaleBegin(ScaleGestureDetector detector){
                isScale = true;
                return true;
            }
            @Override
            public void	onScaleEnd(ScaleGestureDetector detector){
                isScale = false;
            }
        });
    }
    @Override
    protected void onSizeChanged(int width, int height, int oldW, int oldH) {// **
        super.onSizeChanged(width, height, oldW, oldH);
        getLayout(width, height);
    }
    private void getLayout(int width,int height){
        mContentWidth = width;
        mContentHeight = height;
        mOriginX = mContentWidth*(1-INITIAL_SCALE_VALUE)/2;
        mOriginY = mContentHeight*(1-INITIAL_SCALE_VALUE)/2;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawStart(canvas);
        if(data!=null) {
            drawAxis(canvas);
            drawData(canvas);
            if ((capturedX < data.length) && (capturedX >= 0)) drawCursor(canvas);
            calculateValue(canvas);
        }
    }
    void drawAxis(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(1);
        float mTextSize = mContentHeight/20;
        paint.setTextSize(mTextSize);
        paint.setColor(Color.DKGRAY);
        for(int i =0;i < gridX*2+1;i++){

            canvas.drawLine(mOriginX+mContentWidth*mScaleX*i/(gridX*2),mOriginY,
                    mOriginX+mContentWidth*mScaleX*i/(gridX*2),mOriginY+mContentHeight*mScaleY,paint);
        }
        for(int i =0;i < gridY*2+1;i++){
            canvas.drawLine(mOriginX,mOriginY+mContentHeight*mScaleY*i/(gridY*2),
                    mOriginX+mContentWidth*mScaleX,mOriginY+mContentHeight*mScaleY*i/(gridY*2),paint);
        }
        if(style == STYLE_BLACK)
            paint.setColor(Color.WHITE);
        else{
            paint.setColor(Color.BLACK);
        }
        paint.setStrokeWidth(4);
        canvas.drawLine(mOriginX,mOriginY+mContentHeight/2*mScaleY,
                mOriginX + mContentWidth*mScaleX,mOriginY+mContentHeight/2*mScaleY,paint);
        canvas.drawLine(mOriginX + mContentWidth/2*mScaleX,mOriginY,
                mOriginX + mContentWidth/2*mScaleX,mOriginY+mContentHeight*mScaleY,paint);

//        paint.setColor(Color.GRAY);
//        for(int i =0;i < gridX*2+1;i++){
//            float str = (float)((float)(i)/(gridX*2)*dataSize*mTimeScale[speed]);
//            canvas.drawText(String.format("%.1f",str),mOriginX+mContentWidth*mScaleX*i/(gridX*2)+mTextSize/5,
//                    mOriginY+mContentHeight/2*mScaleY-mTextSize/5,paint);
//        }
//
//        for(int i =0;i < gridY*2+1;i++){
//            canvas.drawText(""+(int)((double)(gridY-i)/(gridY*2)*MAX_VALUE*mVoltageScale),mOriginX+mContentWidth/2*mScaleX+mTextSize/5,
//                    mOriginY+mContentHeight*mScaleY*i/(gridY*2)+mTextSize,paint);
//        }
    }
    void drawData(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(3);
        if(data.length == time.length) {
            int mDataSize = data.length;
            double mDataMax = data[0];
            double mDataMin = data[0];
            for(int i = 1; i < data.length; i++){
                mDataMax = Math.max(data[i],mDataMax);
                mDataMin = Math.min(data[i],mDataMin);
            }
            double mDataMaxMin = mDataMax - mDataMin;
            for (int i = 0; i < mDataSize - 1; i++) {
                canvas.drawLine(mOriginX + i * mContentWidth / (mDataSize - 1) * mScaleX,
                        (float)(mOriginY + (mDataMax - data[i]) * mContentHeight / mDataMaxMin * mScaleY),
                        mOriginX + (i + 1) * mContentWidth / (mDataSize - 1) * mScaleX,
                        (float)(mOriginY + (mDataMax - data[i + 1]) * mContentHeight / mDataMaxMin * mScaleY), paint);
            }
        }

    }

    void drawStart(Canvas canvas){
        Paint paint = new Paint();
        if(style == STYLE_BLACK)
            paint.setColor(Color.BLACK);
        else{
            paint.setColor(Color.WHITE);
        }
        canvas.drawRect(0,0,mContentWidth,mContentHeight,paint);
        if(style == STYLE_BLACK)
            paint.setColor(Color.WHITE);
        else{
            paint.setColor(Color.BLACK);
        }
        paint.setStrokeWidth(4);
        canvas.drawLine(0,mContentHeight,
                mContentWidth,mContentHeight,paint);
        canvas.drawLine(mContentWidth,0,
                mContentWidth,mContentHeight,paint);
        canvas.drawLine(0,0,
                0,mContentHeight,paint);
        canvas.drawLine(0,0,mContentWidth,0,paint);


        if(data == null){
            paint.setStrokeWidth(1);
            float mTextSize = mContentHeight/8;

            paint.setTextSize(mTextSize);
            paint.setColor(Color.BLACK);
            paint.setColor(Color.RED);
            canvas.drawText("未捕获波形",mContentWidth/2-(float)2.7*mTextSize,
                    mContentHeight/2,paint);

        }
    }
    void drawCursor(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(2);
        try {
            if(data.length == time.length) {
                int mDataSize = data.length;
                canvas.drawLine(mOriginX + capturedX * mContentWidth / (mDataSize - 1) * mScaleX,
                        0,
                        mOriginX + capturedX * mContentWidth / (mDataSize - 1) * mScaleX,
                        mContentHeight, paint);
            }
            double mDataMax = data[0];
            double mDataMin = data[0];
            for(int i = 1; i < data.length; i++){
                mDataMax = Math.max(data[i],mDataMax);
                mDataMin = Math.min(data[i],mDataMin);
            }
            double mDataMaxMin = mDataMax - mDataMin;
            canvas.drawLine(0,
                    (float)(mOriginY + (mDataMax - data[capturedX]) * mContentHeight / mDataMaxMin * mScaleY),
                    mContentWidth,
                    (float)(mOriginY + (mDataMax - data[capturedX]) * mContentHeight / mDataMaxMin * mScaleY), paint);
        }catch(ArrayIndexOutOfBoundsException  e){
            e.printStackTrace();
        }
        float mTextSize = mContentHeight/20;
        paint.setTextSize(mTextSize);
        paint.setColor(Color.GRAY);
        try {
            canvas.drawText(String.format("value:%.1f",data[capturedX]),
                    (float)(mTextSize*0.5), (float)(mContentHeight - mTextSize*1.5),paint);
        }catch(ArrayIndexOutOfBoundsException  e){
            e.printStackTrace();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(time[capturedX]);
        canvas.drawText("time:" + sdf.format(date) ,
                (float)(mTextSize*0.5), (float)(mContentHeight - mTextSize*3),paint);
    }
    void calculateValue(Canvas canvas){
        float mTextSize = mContentHeight/20;
        Paint paint = new Paint();
        paint.setTextSize(mTextSize);
        paint.setColor(Color.GRAY);
    }

    public void resetScaleOrigin(){
        mOriginX = mContentWidth*(1-INITIAL_SCALE_VALUE)/2;
        mOriginY = mContentHeight*(1-INITIAL_SCALE_VALUE)/2;
        mScaleX = INITIAL_SCALE_VALUE;
        mScaleY = INITIAL_SCALE_VALUE;
        capturedX = -1;
        invalidate();
    }
}
