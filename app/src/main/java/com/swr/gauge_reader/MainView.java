package com.swr.gauge_reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by t4343 on 2018/3/31.
 */

public class MainView extends View {
    int dataSize = 0;
    int[] data = null;

    final int MAX_VALUE = 255;
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
    public float []mTimeScale = {(float)0.056338028,(float)0.040160643,(float)0.024140021,(float)0.016038503};
    public float mVoltageScale = (float)3.389670943;

    int speed = 3;

     //定义GestureDetector类
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int x = (int)((e.getX() - mOriginX) / mContentWidth*(dataSize-1)/mScaleX);
            if(x >= 0 && x <dataSize) {
                capturedX = x;
                invalidate();
                return true;
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
            //Log.d("tuacy", "onDown");
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
                //Log.d("tuacy", "onScaleEnd");
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
            if ((capturedX < dataSize) && (capturedX >= 0)) drawCursor(canvas);
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

        paint.setColor(Color.GRAY);
        for(int i =0;i < gridX*2+1;i++){
            float str = (float)((float)(i)/(gridX*2)*dataSize*mTimeScale[speed]);
            canvas.drawText(String.format("%.1f",str),mOriginX+mContentWidth*mScaleX*i/(gridX*2)+mTextSize/5,
                    mOriginY+mContentHeight/2*mScaleY-mTextSize/5,paint);
        }

        for(int i =0;i < gridY*2+1;i++){
            canvas.drawText(""+(int)((double)(gridY-i)/(gridY*2)*MAX_VALUE*mVoltageScale),mOriginX+mContentWidth/2*mScaleX+mTextSize/5,
                    mOriginY+mContentHeight*mScaleY*i/(gridY*2)+mTextSize,paint);
        }
    }
    void drawData(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(3);
        for(int i = 0 ;i < dataSize - 1;i++){
            canvas.drawLine(mOriginX + i * mContentWidth/(dataSize-1)*mScaleX,
                    mOriginY + (MAX_VALUE - data[i])*mContentHeight/MAX_VALUE*mScaleY,
                    mOriginX + (i + 1) * mContentWidth/(dataSize-1)*mScaleX,
                    mOriginY + (MAX_VALUE - data[i+1])*mContentHeight/MAX_VALUE*mScaleY,paint);
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
            canvas.drawLine(mOriginX + capturedX * mContentWidth / (dataSize - 1) * mScaleX,
                    0,
                    mOriginX + capturedX * mContentWidth / (dataSize - 1) * mScaleX,
                    mContentHeight, paint);

            canvas.drawLine(0,
                    mOriginY + (MAX_VALUE - data[capturedX]) * mContentHeight / MAX_VALUE * mScaleY,
                    mContentWidth,
                    mOriginY + (MAX_VALUE - data[capturedX]) * mContentHeight / MAX_VALUE * mScaleY, paint);
        }catch(ArrayIndexOutOfBoundsException  e){
            e.printStackTrace();
        }
        float mTextSize = mContentHeight/20;
        paint.setTextSize(mTextSize);
        paint.setColor(Color.GRAY);
        try {
            canvas.drawText(String.format("电压:%.1fV",(data[capturedX]-(float)MAX_VALUE/2)*mVoltageScale),
                    (float)(mTextSize*0.5), (float)(mContentHeight - mTextSize*1.5),paint);
        }catch(ArrayIndexOutOfBoundsException  e){
            e.printStackTrace();
        }
        canvas.drawText(String.format("时间:%.1fms",capturedX*mTimeScale[speed]),
                (float)(mTextSize*0.5), (float)(mContentHeight - mTextSize*3),paint);
    }
    void calculateValue(Canvas canvas){
        //峰峰值
        int min = MAX_VALUE,max = 0;
        long sum_ave = 0;
        long sum_root = 0;
        int first = 0;
        int first_trigger = 0;
        int second_trigger = 0;
        int state = 0;
        for(int i = 0;i<dataSize;i++){
            sum_ave = sum_ave + (data[i] - 127);
            sum_root = sum_root + (data[i] - 127) * (data[i] - 127);
            if(data[i]<min)min = data[i];
            if(data[i]>max)max = data[i];
            if((data[i] >50 && data[i]<206)&&state == 0){first = data[i];state = 1;first_trigger=i;}
            if(state == 1) {
                if(data[i]>first)state = 2;
                else if(data[i]<first)state = 3;
                else state = 1;
            }
            if(state == 2 && data[i]<first)state = 4;
            if(state == 3 && data[i]>first)state = 5;
            if(state == 4 && data[i]>first){second_trigger = i;state = 6;}
            if(state == 5 && data[i]<first){second_trigger = i;state = 6;}
        }

        float mTextSize = mContentHeight/20;
        Paint paint = new Paint();
        paint.setTextSize(mTextSize);
        paint.setColor(Color.GRAY);
        float vpp = (max - min)*mVoltageScale;
        float vmean = (float)(sum_ave)/(dataSize)*mVoltageScale;
        float vroot = 0;
        try {
            vroot = (float) Math.sqrt((float)sum_root / dataSize) * mVoltageScale;
        }catch (ArithmeticException e){
            e.printStackTrace();
        }
        canvas.drawText(String.format("峰峰值:%.1fV",vpp),(float)(mTextSize*0.5), (float)(mTextSize*1.5),paint);
        canvas.drawText(String.format("平均值:%.1fV",vmean),(float)(mTextSize*0.5), (float)(mTextSize*3),paint);
        canvas.drawText(String.format("方均根值:%.1fV",vroot),(float)(mTextSize*0.5), (float)(mTextSize*4.5),paint);
        if(second_trigger>first_trigger) {
            float freq = (float) 1000 / (second_trigger - first_trigger) / mTimeScale[speed];
            canvas.drawText(String.format("频率:%.1fHz", freq), (float) (mTextSize * 0.5), (float) (mTextSize * 6), paint);
        }
    }
    public void setDataSize(int datasize){
        dataSize = datasize;
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
