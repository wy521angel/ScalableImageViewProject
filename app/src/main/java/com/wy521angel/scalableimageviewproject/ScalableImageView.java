package com.wy521angel.scalableimageviewproject;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class ScalableImageView extends View implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, Runnable {

    private static final float IMAGE_WIDTH = Utils.dp2px(300);
    private static final float OVER_SCALE_FACTOR = 1.5f;//图片撑满屏幕之后再放大的额外系数

    private Bitmap bitmap;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //初始时为了将图片移动到 View 的正中央的值
    private float originalOffsetX;
    private float originalOffsetY;
    //用户手指拖动的偏移
    private float offsetX;
    private float offsetY;

    private float smallScale;
    private float bigScale;
    private boolean big;//当前的图片是否是大图
    private float scaleFraction;//从 0到1的值 ，0为smallScale 1为bigScale
    private GestureDetectorCompat gestureDetectorCompat;
    private ObjectAnimator scaleAnimator;
    private OverScroller overScroller;


    public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gestureDetectorCompat = new GestureDetectorCompat(context, this);
        overScroller = new OverScroller(context);
        bitmap = Utils.getBitmapByDrawableResources(getResources(), R.drawable.gem,
                (int) IMAGE_WIDTH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        originalOffsetX = (getWidth() - bitmap.getWidth()) / 2f;
        originalOffsetY = (getHeight() - bitmap.getHeight()) / 2f;
        if ((float) getWidth() / bitmap.getWidth() > (float) getHeight() / bitmap.getHeight()) {
            smallScale = (float) getHeight() / bitmap.getHeight();
            bigScale = (float) getWidth() / bitmap.getWidth() * OVER_SCALE_FACTOR;
        } else {
            bigScale = (float) getHeight() / bitmap.getHeight() * OVER_SCALE_FACTOR;
            smallScale = (float) getWidth() / bitmap.getWidth();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetectorCompat.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.translate(offsetX, offsetY);//⼿动偏移
        float scale = smallScale + (bigScale - smallScale) * scaleFraction;
        canvas.scale(scale, scale, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint);
    }

    private float getScaleFraction() {
        return scaleFraction;
    }

    private void setScaleFraction(float scaleFraction) {
        invalidate();
        this.scaleFraction = scaleFraction;
    }

    private ObjectAnimator getAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "scaleFraction", 0, 1);
        }
        return scaleAnimator;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //向右下移动distanceX distanceY 为负值，向左上移动distanceX distanceY 为正值
        Log.i("ScalableImageView", "distanceX:" + distanceX + ";distanceY:" + distanceY);
        //如果现在图片已经是放大后的图片才开始拖动
        if (big) {
            //bitmap.getWidth() * bigScale 表示图片放大后的宽度
            // getWidth() 表示View的宽度
            //二者差值的一半就是图片移动的最大范围
            offsetX -= distanceX;
            offsetX = Math.min(offsetX, (bitmap.getWidth() * bigScale - getWidth()) / 2);
            offsetX = Math.max(offsetX, -(bitmap.getWidth() * bigScale - getWidth()) / 2);
            offsetY -= distanceY;
            offsetY = Math.min(offsetY, (bitmap.getHeight() * bigScale - getHeight()) / 2);
            offsetY = Math.max(offsetY, -(bitmap.getHeight() * bigScale - getHeight()) / 2);
            invalidate();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (big) {
            //滑动的坐标轴是自己定义的，此处将图片的中心选择坐标原点，则起点的坐标为（0,0）
            //startX, startY 的值为0,0，与offsetX、offsetY的初始值相等，所以将offsetX、offsetY填入即可
            overScroller.fling((int) offsetX, (int) offsetY, (int) velocityX, (int) velocityY,
                    -(int) (bitmap.getWidth() * bigScale - getWidth()),
                    (int) (bitmap.getWidth() * bigScale - getWidth()),
                    -(int) (bitmap.getHeight() * bigScale - getHeight()),
                    (int) (bitmap.getHeight() * bigScale - getHeight())
            );
            //下一帧到来时刷新
            postOnAnimation(this);
        }

        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        big = !big;
        if (big) {
            getAnimator().start();
        } else {
            getAnimator().reverse();
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public void run() {
        // 计算此时的位置，并且如果滑动已经结束，就停⽌
        if (overScroller.computeScrollOffset()) {
            // 把此时的位置应⽤于界⾯
            offsetX = overScroller.getCurrX();
            offsetY = overScroller.getCurrY();
            invalidate();
            // 下⼀帧刷新
            postOnAnimation(this);
        }
    }
}
