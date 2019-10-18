package com.swr.gauge_reader;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class PictureView extends View {

    private Bitmap mPicture;
    
    private int mode;
    public static final int MODE_NONE = 0;
    public static final int MODE_RECT = 1;
    public static final int MODE_DOT = 2;
    public static final int MODE_VECTOR = 3;

    private RectF pictureRegion;
    private RectF selectedRect;

    private Context context;
    private float first_x,first_y,cur_x,cur_y;
    private float[] past_position = new float[4];
    private GestureDetector mGestureDetector;

    private OnClickListener mOnClickListener;
    public interface OnRectClickListener {
        void onRectClick();
    }


    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            

            if(past_position[0]!=past_position[2]&&past_position[1]!=past_position[3]) {
                if((Math.min(past_position[0],past_position[2])<e.getX()&&e.getX()<Math.max(past_position[0],past_position[2]))
                        &&(Math.min(past_position[1],past_position[3])<e.getY()&&e.getY()<Math.max(past_position[1],past_position[3]))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    final View dialogView = LayoutInflater.from(context)
                            .inflate(R.layout.template_set_up,null);
                    PictureView pictureView = dialogView.findViewById(R.id.picture);
                    int pictureX = (int)((Math.min(past_position[0],past_position[2])-pictureRegion.left)
                            * mPicture.getWidth()/(pictureRegion.width()));
                    int pictureY = (int)((Math.min(past_position[1],past_position[3])-pictureRegion.top)
                            * mPicture.getHeight()/(pictureRegion.height()));
                    int pictureW = (int)(Math.abs(past_position[0] - past_position[2])
                            * mPicture.getWidth()/(pictureRegion.width()));
                    int pictureH = (int)(Math.abs(past_position[1] - past_position[3])
                            * mPicture.getHeight()/(pictureRegion.height()));
                    Log.d("TAG",""+ pictureX + "," + pictureY + "," + pictureW + "," + pictureH);
                    pictureView.setPicture(Bitmap.createBitmap(mPicture,pictureX,pictureY,pictureW,pictureH));
                    builder.setIcon(R.drawable.osc);
                    builder.setPositiveButton("Yes", null);
                    builder.setView(dialogView);
                    builder.show();
                }
            }
            for(int i = 0; i < 4; i++)
                past_position[i] = 0;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // TODO: 增加边界判定
            cur_x = cur_x - distanceX;
            cur_y = cur_y - distanceY;
            past_position[0] = first_x;
            past_position[1] = first_y;
            past_position[2] = cur_x;
            past_position[3] = cur_y;
            invalidate();
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
            first_x = e.getX();
            first_y = e.getY();
            cur_x = first_x;
            cur_y = first_y;
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

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
        return mGestureDetector.onTouchEvent(event);
    }

    public PictureView(Context context) {
        super(context);
        init(context,null, 0);
    }

    public PictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs, 0);
    }

    public PictureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        this.context = context;
        pictureRegion = new RectF();
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;


        drawBitmap(canvas,contentWidth,contentHeight);
        drawRect(canvas,contentWidth,contentHeight);
    }

    public void drawBitmap(Canvas canvas, int contentWidth,int contentHeight){
        Paint paint = new Paint();
        // Draw the example drawable on top of the text.
        if (mPicture != null) {
            float scale = (float) mPicture.getHeight() / (float)mPicture.getWidth();
            if(scale < (float)contentHeight/(float)contentWidth) {
                pictureRegion.set(0, (float)contentHeight/2 - contentWidth*scale/2,
                        contentWidth, (float)contentHeight/2 + contentWidth*scale/2);
            }else{
                pictureRegion.set((float)contentWidth/2-contentHeight/scale/2, 0,
                        (float)contentWidth/2+contentHeight/scale/2, contentHeight);
            }
            canvas.drawBitmap(mPicture, null, pictureRegion, paint);
//            Bitmap.createBitmap()
        }
    }

    public void drawRect(Canvas canvas, int contentWidth,int contentHeight) {
        Paint paint = new Paint();
        if(first_x!=cur_x&&first_y!=cur_y) {
            paint.setColor(Color.BLACK);
            paint.setAlpha(127);
            canvas.drawRect(pictureRegion.left, pictureRegion.top, pictureRegion.right, Math.min(past_position[1],
                    past_position[3]), paint);
            canvas.drawRect(pictureRegion.left, Math.max(past_position[1], past_position[3]),
                    pictureRegion.right, pictureRegion.bottom, paint);
            canvas.drawRect(pictureRegion.left, Math.min(past_position[1], past_position[3]),
                    Math.min(past_position[0], past_position[2]),
                    Math.max(past_position[1], past_position[3]), paint);
            canvas.drawRect(Math.max(past_position[0], past_position[2]),
                    Math.min(past_position[1], past_position[3]),
                    pictureRegion.right, Math.max(past_position[1], past_position[3]), paint);
            paint.setStrokeWidth(5);
            paint.setColor(Color.YELLOW);
            canvas.drawLine(past_position[0], past_position[1], past_position[0], past_position[3], paint);
            canvas.drawLine(past_position[0], past_position[1], past_position[2], past_position[1], paint);
            canvas.drawLine(past_position[2], past_position[1], past_position[2], past_position[3], paint);
            canvas.drawLine(past_position[0], past_position[3], past_position[2], past_position[3], paint);
        }
    }
    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public Bitmap getPicture() {
        return mPicture;
    }

    public void setPicture(Bitmap mPicture) {
        this.mPicture = mPicture;
    }

    public void setOnClickListener(OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    public class TagedRect{
        private RectF rect;
        private String text;
        private int type;
        public TagedRect(){
            rect = new RectF();
            text = "";
            type = 0;
        }

        public TagedRect(RectF mRect, String mText, int mType){
            rect = mRect;
            text = mText;
            type = mType;
        }

        public int getType() {
            return type;
        }

        public RectF getRect() {
            return rect;
        }

        public String getText() {
            return text;
        }

        public void setRect(RectF rect) {
            this.rect = rect;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}