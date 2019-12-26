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
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class ScalableImageView extends View {

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
    private float currentScale;//当前缩放值
    private GestureDetectorCompat gestureDetectorCompat;
    private ObjectAnimator scaleAnimator;
    private OverScroller overScroller;
    private ScaleGestureDetector scaleGestureDetector;//注意ScaleGestureDetectorCompat
    // 并不是ScaleGestureDetector 的兼容类

    private FlingRunner flingRunner = new FlingRunner();
    private ViewBaseGestureListener viewBaseGestureListener =
            new ViewBaseGestureListener();
    private ViewScaleGestureListener viewScaleGestureListener = new ViewScaleGestureListener();

    public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gestureDetectorCompat = new GestureDetectorCompat(context,
                viewBaseGestureListener);
        overScroller = new OverScroller(context);
        bitmap = Utils.getBitmapByDrawableResources(getResources(), R.drawable.gem,
                (int) IMAGE_WIDTH);
        scaleGestureDetector = new ScaleGestureDetector(context, viewScaleGestureListener);
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

        currentScale = smallScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress()) {//如果双指没有发生捏撑操作，使用单指触摸
            result = gestureDetectorCompat.onTouchEvent(event);
        }
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        float scaleFaction = (currentScale - smallScale) / (bigScale - smallScale);
        canvas.translate(offsetX * scaleFaction, offsetY * scaleFaction);//⼿动偏移
        canvas.scale(currentScale, currentScale, getWidth() / 2f, getHeight() / 2f);
        canvas.drawBitmap(bitmap, originalOffsetX, originalOffsetY, paint);
    }

    public float getCurrentScale() {
        return currentScale;
    }

    public void setCurrentScale(float currentScale) {
        this.currentScale = currentScale;
        invalidate();
    }

    private ObjectAnimator getAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "currentScale", 0);
        }
        scaleAnimator.setFloatValues(smallScale, bigScale);
        return scaleAnimator;
    }

    private void fixOffsets() {
        offsetX = Math.min(offsetX, (bitmap.getWidth() * bigScale - getWidth()) / 2);
        offsetX = Math.max(offsetX, -(bitmap.getWidth() * bigScale - getWidth()) / 2);
        offsetY = Math.min(offsetY, (bitmap.getHeight() * bigScale - getHeight()) / 2);
        offsetY = Math.max(offsetY, -(bitmap.getHeight() * bigScale - getHeight()) / 2);
    }

    class ViewBaseGestureListener implements GestureDetector.OnGestureListener,
            GestureDetector.OnDoubleTapListener {

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
                offsetY -= distanceY;
                fixOffsets();
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
                        -(int) (bitmap.getWidth() * bigScale - getWidth()) / 2,
                        (int) (bitmap.getWidth() * bigScale - getWidth()) / 2,
                        -(int) (bitmap.getHeight() * bigScale - getHeight()) / 2,
                        (int) (bitmap.getHeight() * bigScale - getHeight()) / 2, 100, 100);
                //下一帧到来时刷新
                postOnAnimation(flingRunner);
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
                //如果需要双击放大后的点以手指点击处为中心放大，需要在此计算偏移值
                offsetX = (e.getX() - getWidth() / 2f) -//双击放大前的偏移量
                        (e.getX() - getWidth() / 2f) * bigScale / smallScale;//双击放大后的偏移量
                offsetY = (e.getY() - getHeight() / 2f) -//双击放大前的偏移量
                        (e.getY() - getHeight() / 2f) * bigScale / smallScale;//双击放大后的偏移量
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

    }

    class ViewScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        float initialScale;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            currentScale = initialScale * detector.getScaleFactor();//缩放倍数
            invalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            initialScale = currentScale;
            return true;//消费事件
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }

    class FlingRunner implements Runnable {

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
//            ViewCompat.postOnAnimation(this,this);
            }
        }
    }
}
