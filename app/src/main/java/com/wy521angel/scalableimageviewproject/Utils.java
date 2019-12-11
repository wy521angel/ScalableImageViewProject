package com.wy521angel.scalableimageviewproject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;

public class Utils {

    public static float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 通过 Drawable 图片资源获取 Bitmap
     *
     * @param res     Resources
     * @param resId   图片资源Id
     * @param density 表示 Bitmap 最终的像素密度
     * @return
     */
    public static Bitmap getBitmapByDrawableResources(Resources res, int resId, int density) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inJustDecodeBounds = false;
        options.inDensity = options.outWidth;
        options.inTargetDensity = density;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
