package com.absinthe.kage.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.kage.R
import com.absinthe.kage.utils.Logger

class MusicRouletteView : AppCompatImageView {

    private val mBitmapPaint: Paint
    private val mBorderPaint: Paint
    private val mBorderRect: RectF
    private val mDrawableRect: RectF
    private val mShaderMatrix: Matrix

    private var mBitmap: Bitmap? = null
    private var mBitmapShader: BitmapShader? = null
    private var mObjectAnimator: ObjectAnimator = ObjectAnimator()
    private var mBitmapHeight = 0
    private var mBitmapWidth = 0
    private var mBorderWidth: Int
    private var mBorderColor: Int
    private var mBorderRadius = 0.0f
    private var mDrawableRadius = 0.0f
    private var mCurrentRotation = 0.0f
    private var mIsReady = false
    private var mIsSetupPending = false

    constructor(context: Context?) : super(context) {
        mDrawableRect = RectF()
        mBorderRect = RectF()
        mShaderMatrix = Matrix()
        mBitmapPaint = Paint()
        mBorderPaint = Paint()
        mBorderColor = DEFAULT_BORDER_COLOR
        mBorderWidth = DEFAULT_BORDER_WIDTH
        init()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0) : super(context, attrs, defStyle) {
        mDrawableRect = RectF()
        mBorderRect = RectF()
        mShaderMatrix = Matrix()
        mBitmapPaint = Paint()
        mBorderPaint = Paint()
        mBorderColor = DEFAULT_BORDER_COLOR
        mBorderWidth = DEFAULT_BORDER_WIDTH

        val a = context.obtainStyledAttributes(attrs, R.styleable.MusicRouletteView, defStyle, 0)
        mBorderWidth = a.getDimensionPixelSize(R.styleable.MusicRouletteView_borderWidth, 0)
        mBorderColor = a.getColor(R.styleable.MusicRouletteView_borderColor, Color.BLACK)
        a.recycle()
        init()
    }

    var borderColor: Int
        get() = mBorderColor
        set(borderColor) {
            if (borderColor != mBorderColor) {
                mBorderColor = borderColor
                mBorderPaint.color = mBorderColor
                invalidate()
            }
        }

    var borderWidth: Int
        get() = mBorderWidth
        set(borderWidth) {
            if (borderWidth != mBorderWidth) {
                mBorderWidth = borderWidth
                setup()
            }
        }

    override fun getScaleType(): ScaleType {
        return SCALE_TYPE
    }

    override fun setScaleType(scaleType: ScaleType) {
        require(scaleType == SCALE_TYPE) { String.format("ScaleType %s not supported.", scaleType) }
    }

    override fun onDraw(canvas: Canvas) {
        if (drawable != null) {
            canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), mDrawableRadius, mBitmapPaint)
            if (mBorderWidth != 0) {
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), mBorderRadius, mBorderPaint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
        outlineProvider = CustomOutline(w, h)
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mBitmap = bm
        setup()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        mBitmap = getBitmapFromDrawable(drawable)
        setup()
    }

    fun startAnimation() {
        if (!mObjectAnimator.isStarted) {
            mObjectAnimator.setFloatValues(mCurrentRotation, mCurrentRotation + 360.0f)
            mObjectAnimator.start()
        }
    }

    fun pauseAnimation() {
        if (mObjectAnimator.isStarted) {
            mObjectAnimator.cancel()
        }
    }

    fun stopAnimation() {
        if (mObjectAnimator.isStarted || mObjectAnimator.isRunning) {
            mObjectAnimator.end()
        }
        mCurrentRotation = 0.0f
    }

    private fun init() {
        super.setScaleType(SCALE_TYPE)
        mIsReady = true
        if (mIsSetupPending) {
            setup()
            mIsSetupPending = false
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else try {
            val bitmap: Bitmap = if (drawable is ColorDrawable) {
                Bitmap.createBitmap(COLOR_DRAWABLE_DIMENSION, COLOR_DRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun setup() {
        Logger.runningHere()
        if (mIsReady) {
            if (mBitmap != null) {
                mBitmapShader = BitmapShader(mBitmap!!, TileMode.CLAMP, TileMode.CLAMP)
                mBitmapPaint.isAntiAlias = true
                mBitmapPaint.shader = mBitmapShader
                mBorderPaint.style = Paint.Style.STROKE
                mBorderPaint.isAntiAlias = true
                mBorderPaint.color = mBorderColor
                mBorderPaint.strokeWidth = mBorderWidth.toFloat()
                mBitmapHeight = mBitmap!!.height
                mBitmapWidth = mBitmap!!.width
                mBorderRect[0.0f, 0.0f, width.toFloat()] = height.toFloat()
                mBorderRadius = ((mBorderRect.height() - mBorderWidth.toFloat()) / 2.0f).coerceAtMost((mBorderRect.width() - mBorderWidth.toFloat()) / 2.0f)
                mDrawableRect[mBorderWidth.toFloat(), mBorderWidth.toFloat(), mBorderRect.width() - mBorderWidth.toFloat()] = mBorderRect.height() - mBorderWidth.toFloat()
                mDrawableRadius = (mDrawableRect.height() / 2.0f).coerceAtMost(mDrawableRect.width() / 2.0f)
                updateShaderMatrix()
                initAnimator()
                invalidate()
                return
            }
            return
        }
        mIsSetupPending = true
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0.0f
        var dy = 0.0f
        mShaderMatrix.set(null)
        if (mBitmapWidth.toFloat() * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight.toFloat()) {
            scale = mDrawableRect.height() / mBitmapHeight.toFloat()
            dx = (mDrawableRect.width() - mBitmapWidth.toFloat() * scale) * 0.5f
        } else {
            scale = mDrawableRect.width() / mBitmapWidth.toFloat()
            dy = (mDrawableRect.height() - mBitmapHeight.toFloat() * scale) * 0.5f
        }
        mShaderMatrix.setScale(scale, scale)
        val i = (dx + 0.5f).toInt()
        mShaderMatrix.postTranslate((i + mBorderWidth).toFloat(), ((0.5f + dy).toInt() + mBorderWidth).toFloat())
        mBitmapShader!!.setLocalMatrix(mShaderMatrix)
    }

    private fun initAnimator() {
        mCurrentRotation = 0.0f
        mObjectAnimator.target = this
        mObjectAnimator.setPropertyName("rotation")
        mObjectAnimator.duration = 20000
        mObjectAnimator.interpolator = LinearInterpolator()
        mObjectAnimator.repeatCount = ValueAnimator.INFINITE
        mObjectAnimator.repeatMode = ValueAnimator.RESTART
        mObjectAnimator.addUpdateListener { animation: ValueAnimator -> mCurrentRotation = animation.animatedValue as Float }

        if (mObjectAnimator.isStarted) {
            mObjectAnimator.cancel()
        }
    }

    private class CustomOutline internal constructor(var width: Int, var height: Int) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRect(0, 0, width, height)
        }

    }

    companion object {
        private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
        private const val COLOR_DRAWABLE_DIMENSION = 1
        private const val DEFAULT_BORDER_COLOR = Color.BLACK
        private const val DEFAULT_BORDER_WIDTH = 0
        private val SCALE_TYPE = ScaleType.CENTER_CROP
    }
}