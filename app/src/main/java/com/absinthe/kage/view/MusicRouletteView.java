package com.absinthe.kage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.appcompat.widget.AppCompatImageView;

import com.absinthe.kage.R;

public class MusicRouletteView extends AppCompatImageView {

    private static final Config BITMAP_CONFIG = Config.ARGB_8888;
    private static final int COLOR_DRAWABLE_DIMENSION = 1;
    private static final int DEFAULT_BORDER_COLOR = Color.BLACK;
    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;

    private final Paint mBitmapPaint;
    private final Paint mBorderPaint;
    private final RectF mBorderRect;
    private final RectF mDrawableRect;
    private final Matrix mShaderMatrix;
    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;
    private int mBitmapHeight;
    private int mBitmapWidth;
    private int mBorderWidth;
    private int mBorderColor;
    private float mBorderRadius;
    private float mDrawableRadius;
    private boolean mIsReady;
    private boolean mIsSetupPending;

    public MusicRouletteView(Context context) {
        super(context);
        mDrawableRect = new RectF();
        mBorderRect = new RectF();
        mShaderMatrix = new Matrix();
        mBitmapPaint = new Paint();
        mBorderPaint = new Paint();
        mBorderColor = DEFAULT_BORDER_COLOR;
        mBorderWidth = DEFAULT_BORDER_WIDTH;
        init();
    }

    public MusicRouletteView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicRouletteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDrawableRect = new RectF();
        mBorderRect = new RectF();
        mShaderMatrix = new Matrix();
        mBitmapPaint = new Paint();
        mBorderPaint = new Paint();
        mBorderColor = DEFAULT_BORDER_COLOR;
        mBorderWidth = DEFAULT_BORDER_WIDTH;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MusicRouletteView, defStyle, 0);
        mBorderWidth = a.getDimensionPixelSize(R.styleable.MusicRouletteView_borderWidth, 0);
        mBorderColor = a.getColor(R.styleable.MusicRouletteView_borderColor, Color.BLACK);
        a.recycle();

        init();
    }

    private void init() {
        super.setScaleType(SCALE_TYPE);
        mIsReady = true;
        if (mIsSetupPending) {
            setup();
            mIsSetupPending = false;
        }
    }

    public ScaleType getScaleType() {
        return SCALE_TYPE;
    }

    public void setScaleType(ScaleType scaleType) {
        if (scaleType != SCALE_TYPE) {
            throw new IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType));
        }
    }

    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            canvas.drawCircle((float) (getWidth() / 2), (float) (getHeight() / 2), mDrawableRadius, mBitmapPaint);

            if (mBorderWidth != 0) {
                canvas.drawCircle((float) (getWidth() / 2), (float) (getHeight() / 2), mBorderRadius, mBorderPaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
        setOutlineProvider(new CustomOutline(w, h));
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        if (borderColor != mBorderColor) {
            mBorderColor = borderColor;
            mBorderPaint.setColor(mBorderColor);
            invalidate();
        }
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        if (borderWidth != mBorderWidth) {
            mBorderWidth = borderWidth;
            setup();
        }
    }

    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;
        setup();
    }

    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        try {
            Bitmap bitmap;
            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLOR_DRAWABLE_DIMENSION, COLOR_DRAWABLE_DIMENSION, BITMAP_CONFIG);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), BITMAP_CONFIG);
            }
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private void setup() {
        if (mIsReady) {
            if (mBitmap != null) {
                mBitmapShader = new BitmapShader(mBitmap, TileMode.CLAMP, TileMode.CLAMP);
                mBitmapPaint.setAntiAlias(true);
                mBitmapPaint.setShader(mBitmapShader);
                mBorderPaint.setStyle(Style.STROKE);
                mBorderPaint.setAntiAlias(true);
                mBorderPaint.setColor(mBorderColor);
                mBorderPaint.setStrokeWidth((float) mBorderWidth);
                mBitmapHeight = mBitmap.getHeight();
                mBitmapWidth = mBitmap.getWidth();
                mBorderRect.set(0.0f, 0.0f, (float) getWidth(), (float) getHeight());
                mBorderRadius = Math.min((mBorderRect.height() - ((float) mBorderWidth)) / 2.0f, (mBorderRect.width() - ((float) mBorderWidth)) / 2.0f);
                mDrawableRect.set((float) mBorderWidth, (float) mBorderWidth,
                        mBorderRect.width() - ((float) mBorderWidth), mBorderRect.height() - ((float) mBorderWidth));
                mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f);
                updateShaderMatrix();
                invalidate();
                return;
            }
            return;
        }
        mIsSetupPending = true;
    }

    private void updateShaderMatrix() {
        float scale;
        float dx = 0.0f;
        float dy = 0.0f;
        mShaderMatrix.set(null);
        if (((float) mBitmapWidth) * mDrawableRect.height() > mDrawableRect.width() * ((float) mBitmapHeight)) {
            scale = mDrawableRect.height() / ((float) mBitmapHeight);
            dx = (mDrawableRect.width() - (((float) mBitmapWidth) * scale)) * 0.5f;
        } else {
            scale = mDrawableRect.width() / ((float) mBitmapWidth);
            dy = (mDrawableRect.height() - (((float) mBitmapHeight) * scale)) * 0.5f;
        }
        mShaderMatrix.setScale(scale, scale);
        int i = (int) (dx + 0.5f);
        mShaderMatrix.postTranslate((float) (i + mBorderWidth), (float) (((int) (0.5f + dy)) + mBorderWidth));
        mBitmapShader.setLocalMatrix(mShaderMatrix);
    }

    private static class CustomOutline extends ViewOutlineProvider {

        int width;
        int height;

        CustomOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, width, height);
        }
    }
}

