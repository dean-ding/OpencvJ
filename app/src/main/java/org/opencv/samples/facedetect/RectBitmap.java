package org.opencv.samples.facedetect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created: tvt on 2018/4/17 10:12
 */
@SuppressLint("AppCompatCustomView")
public class RectBitmap extends ImageView
{
    public RectBitmap(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    private Bitmap mCacheBitmap = null;
    private Matrix matrix = new Matrix(); // I rotate it with minimal process

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (mCacheBitmap != null)
        {
            matrix.reset();

            matrix.preTranslate((float) (canvas.getWidth() - mCacheBitmap.getWidth()) / 2, (float) (canvas.getHeight() - mCacheBitmap.getHeight()) / 2);
            matrix.postRotate(90f, (canvas.getWidth()) / 2, (canvas.getHeight()) / 2);
            float scaleX = (float) canvas.getWidth() / (float) mCacheBitmap.getHeight();
            float scaleY = (float) canvas.getHeight() / (float) mCacheBitmap.getWidth();
            matrix.postScale(scaleX, scaleY, canvas.getWidth() / 2, canvas.getHeight() / 2);
            canvas.drawBitmap(mCacheBitmap, matrix, null);
        }
    }

    public void updateRect(Bitmap bitmap)
    {
        this.mCacheBitmap = bitmap;
        postInvalidate();
    }
}
