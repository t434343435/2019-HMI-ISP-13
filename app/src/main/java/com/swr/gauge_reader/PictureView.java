package com.swr.gauge_reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class PictureView extends View {

    private Bitmap mPicture;
    
    private int mode;
    public static final int MODE_RECT = 1;
    public static final int MODE_VECTOR = 3;

    private RectF pictureRegion;
    private RectF selectedRect;
    private Rect bound;
    
    private float first_x,first_y,cur_x,cur_y;
    
    private GestureDetector mGestureDetector;

    private OnRectClickListener mOnRectClickListener;

    List<TagRect> tagRectList;
    List<TagVector> tagVectorList;

    public interface OnRectClickListener {
        void onRectClick(MotionEvent e);
        void onVectorUpdate(RectF r);
    }


    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(mode == MODE_RECT) {
                if (mOnRectClickListener != null) {
                    mOnRectClickListener.onRectClick(e);
                    selectedRect.set(-10.0f,-10.0f,-10.0f,-10.0f);
                }
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float tempCurX,tempCurY;
            tempCurX = cur_x - distanceX;
            tempCurY = cur_y - distanceY;
            if(pictureRegion.contains(tempCurX,tempCurY)) {
                cur_x = tempCurX;
                cur_y = tempCurY;
                selectedRect.set(first_x, first_y, cur_x, cur_y);
            }
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
            if(pictureRegion.contains(e.getX(),e.getY())) {
                first_x = e.getX();
                first_y = e.getY();
                cur_x = first_x;
                cur_y = first_y;
                invalidate();
            }
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
        if(event.getAction() == MotionEvent.ACTION_UP && mode == MODE_VECTOR)
            if(mOnRectClickListener != null && (selectedRect.height() != 0 && selectedRect.width() != 0)) {
                mOnRectClickListener.onVectorUpdate(selectedRect);
                selectedRect.set(-10.0f,-10.0f,-10.0f,-10.0f);
                invalidate();
            }
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
        pictureRegion = new RectF(-10.0f,-10.0f,-10.0f,-10.0f);
        selectedRect = new RectF(-10.0f,-10.0f,-10.0f,-10.0f);
        bound = new Rect();
        tagRectList = new ArrayList<>();
        tagVectorList = new ArrayList<>();
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inScaled = false;
        Bitmap bm = BitmapFactory.decodeResource(getResources(),R.drawable.yibiao,op);
        setPicture(bm);
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
        drawTagRect(canvas,contentWidth,contentHeight);
        drawTagVector(canvas,contentWidth,contentHeight);
        if(mode == MODE_RECT)
            drawRect(canvas,contentWidth,contentHeight);
        if(mode == MODE_VECTOR)
            drawVector(canvas,selectedRect,Color.RED,contentWidth,contentHeight);
        Paint paint = new Paint();
        paint.setStrokeWidth((contentHeight+contentWidth)/500);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(pictureRegion,paint);
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
            canvas.drawRect(pictureRegion,paint);
            paint.setAlpha(255);
            Rect pictureRect = subPicture(selectedRect);
            canvas.drawBitmap(mPicture,pictureRect,selectedRect,paint);
            paint.setStrokeWidth((contentWidth+contentHeight)/200);
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(selectedRect,paint);
        }
    }

    public void drawVector(Canvas canvas,RectF rect,int color, int contentWidth,int contentHeight) {
        Paint paint = new Paint();
        paint.setStrokeWidth((contentWidth+contentHeight)/200);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(rect.left,rect.top,rect.right,rect.bottom,paint);
        float ang = (float) 0.5;
        float arrow_length = (contentWidth+contentHeight)/50;
        double vector_length = Math.sqrt(rect.height()*rect.height()+rect.width()*rect.width());
        double px = rect.width()/vector_length*arrow_length;
        double py = rect.height()/vector_length*arrow_length;
        double vx1 = px * Math.cos(ang) - py * Math.sin(ang);
        double vy1 = px * Math.sin(ang) + py * Math.cos(ang);
        double vx2 = px * Math.cos(-ang) - py * Math.sin(-ang);
        double vy2 = px * Math.sin(-ang) + py * Math.cos(-ang);
        canvas.drawLine(rect.right,rect.bottom,rect.right-(float)vx1,rect.bottom-(float)vy1,paint);
        canvas.drawLine(rect.right,rect.bottom,rect.right-(float)vx2,rect.bottom-(float)vy2,paint);
        int red = (paint.getColor()&0x00FF0000)>>16;
        int green = (paint.getColor()&0x0000FF00)>>8;
        int blue = (paint.getColor()&0x000000FF);
        paint.setARGB(255,255-red,255-green,255-blue);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(rect.left,rect.top,(contentWidth+contentHeight)/200,paint);
    }

    public void drawTagRect(Canvas canvas, int contentWidth,int contentHeight) {
        Paint paint = new Paint();
        for(int i = 0; i < tagRectList.size(); i++){
            String tag = tagRectList.get(i).getText();
            RectF region = tagRectList.get(i).getRect();
            float stokeWidth = (contentWidth+contentHeight)/200;
            int color =tagRectList.get(i).getColor();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stokeWidth);
            paint.setTextSize((contentWidth+contentHeight)/50);
            canvas.drawRect(region,paint);

            int maxWidth = 0;
            int textHeight = 0;
            String[] tag_in_line = tag.split("\n");
            for(int j = 0; j < tag_in_line.length; j++){
                paint.getTextBounds(tag_in_line[j],0,tag_in_line[j].length(),bound);
//                Log.d(""+i,""+bound.left+","+bound.top+","+bound.right+","+bound.bottom+",");
                maxWidth = Math.max(maxWidth,bound.right);
                textHeight = bound.height();
            }
            bound.set((int)-stokeWidth/2+bound.left,0,maxWidth + (int)stokeWidth
                    ,(int)(tag_in_line.length * textHeight * 1.5 +(contentWidth+contentHeight)/200));
            Log.d(""+i,""+bound.left+","+bound.top+","+bound.right+","+bound.bottom+",");
            bound.offset((int)region.left,(int)region.bottom);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRect(bound,paint);
            int red = (paint.getColor()&0x00FF0000)>>16;
            int green = (paint.getColor()&0x0000FF00)>>8;
            int blue = (paint.getColor()&0x000000FF);
            if(red + green +blue > 510)
                paint.setColor(Color.BLACK);
            else
                paint.setColor(Color.WHITE);

            for(int j = 0; j < tag_in_line.length; j++){
                canvas.drawText(tag_in_line[j],region.left,region.bottom + (j + 1) * textHeight * (float)1.5,paint);
            }
        }
    }

    public void drawTagVector(Canvas canvas, int contentWidth,int contentHeight) {
        Paint paint = new Paint();
        for(int i = 0; i < tagVectorList.size(); i++) {
            String tag = tagVectorList.get(i).getText();
            RectF region = tagVectorList.get(i).getVector();
            float stokeWidth = (contentWidth + contentHeight) / 200;
            int color = tagVectorList.get(i).getColor();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stokeWidth);
            paint.setTextSize((contentWidth + contentHeight) / 50);
            drawVector(canvas,region,color,contentWidth,contentHeight);
            if(tag != "") {
                int maxWidth = 0;
                int textHeight = 0;
                String[] tag_in_line = tag.split("\n");
                for (int j = 0; j < tag_in_line.length; j++) {
                    paint.getTextBounds(tag_in_line[j], 0, tag_in_line[j].length(), bound);
                    maxWidth = Math.max(maxWidth, bound.right);
                    textHeight = bound.height();
                }
                bound.set((int) -stokeWidth / 2 + bound.left, 0, maxWidth + (int) stokeWidth
                        , (int) (tag_in_line.length * textHeight * 1.5 + (contentWidth + contentHeight) / 200));

                int dx = (int) region.centerX() - bound.centerX();
                int dy = (int) region.centerY() - bound.centerY();
                bound.offset(dx, dy);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                canvas.drawRect(bound, paint);
                int red = (paint.getColor() & 0x00FF0000) >> 16;
                int green = (paint.getColor() & 0x0000FF00) >> 8;
                int blue = (paint.getColor() & 0x000000FF);
                if (red + green + blue > 382)
                    paint.setColor(Color.BLACK);
                else
                    paint.setColor(Color.WHITE);
                for (int j = 0; j < tag_in_line.length; j++) {
                    canvas.drawText(tag_in_line[j], dx, dy + (j + 1) * textHeight * (float) 1.5, paint);
                }
            }
        }
    }

    public Rect subPicture(RectF rect){
        Rect sub = new Rect();
        float widthScale = mPicture.getWidth()/pictureRegion.width();
        float heightScale = mPicture.getHeight()/pictureRegion.height();
        int pictureX = (int)((rect.left-pictureRegion.left) * widthScale);
        int pictureY = (int)((rect.top-pictureRegion.top) * heightScale);
        int pictureW = (int)((rect.right-pictureRegion.left)* widthScale);
        int pictureH = (int)((rect.bottom-pictureRegion.top) * heightScale);
        sub.set(pictureX,pictureY,pictureW,pictureH);
        return sub;
    }

    public RectF subPicture(Rect rect){
        RectF sub = new RectF();
        float widthScale = pictureRegion.width()/mPicture.getWidth();
        float heightScale = pictureRegion.height()/mPicture.getHeight();
        float pictureX = rect.left * widthScale;
        float pictureY = rect.top * heightScale;
        float pictureW = rect.right* widthScale;
        float pictureH = rect.bottom * heightScale;
        sub.set(pictureX,pictureY,pictureW,pictureH);
        return sub;
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

    public void setOnRectClickListener(OnRectClickListener mOnRectClickListener) {
        this.mOnRectClickListener = mOnRectClickListener;
    }

    public RectF getSelectedRect() {
        return selectedRect;
    }

    public TagVector getTagVector(int type) {
        for (int i = 0;i < tagVectorList.size(); i++){
            if (tagVectorList.get(i).getType() == type){
                return tagVectorList.get(i);
            }
        }
        return null;
    }

    public void clearRectList(){
        tagRectList.clear();
    }
    public void clearVectorList(){
        tagVectorList.clear();
    }
    public void setTagVector(int type, String text, RectF vector, int color) {
        for (int i = 0;i < tagVectorList.size(); i++){
            if (tagVectorList.get(i).getType() == type){
                TagVector tv = tagVectorList.get(i);
                tv.setText(text);
                tv.setVector(vector);
                tv.setColor(color);
                return;
            }
        }
        TagVector tv = new TagVector(vector, text, type, color);
        tagVectorList.add(tv);
    }

    public TagRect getTagRect(String text) {
        for (int i = 0;i < tagRectList.size(); i++){
            if (text.equals(tagRectList.get(i).getText())){
                return tagRectList.get(i);
            }
        }
        return null;
    }

    public void setTagRect(int type, String text, RectF rect, int color) {
        for (int i = 0;i < tagRectList.size(); i++){
            if (text.equals(tagRectList.get(i).getText())){
                TagRect tv = tagRectList.get(i);
                tv.setType(type);
                tv.setRect(rect);
                tv.setColor(color);
                return;
            }
        }
        TagRect tv = new TagRect(rect, text, type, color);
        tagRectList.add(tv);
        return;
    }
    
    public class TagRect{
        private RectF rect;
        private String text;
        private int type;
        private int color;
        public TagRect(){
            rect = new RectF();
            text = "";
            type = 0;
            this.color = 0xFF000000;
        }

        public TagRect(RectF mRect, String mText, int mType, int color){
            rect = new RectF(mRect);
            rect.sort();
            text = mText;
            type = mType;
            this.color = color;
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

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }

    public class TagVector{
        private RectF vector;
        private String text;
        private int type;
        private int color;

        public static final int MAX = 0;
        public static final int MIN = 1;
        public TagVector(){
            vector = new RectF();
            text = "";
            type = 0;
            this.color = 0xFF000000;
        }

        public TagVector(RectF mVector, String mText, int mType, int color){
            vector = new RectF(mVector);
            text = mText;
            type = mType;
            this.color = color;
        }

        public int getType() {
            return type;
        }

        public RectF getVector() {
            return vector;
        }

        public String getText() {
            return text;
        }

        public void setVector(RectF vector) {
            this.vector = vector;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }
    
}