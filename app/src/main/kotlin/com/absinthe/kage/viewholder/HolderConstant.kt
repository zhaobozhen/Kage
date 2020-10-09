package com.absinthe.kage.viewholder

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.blankj.utilcode.util.Utils

class HolderConstant {
    companion object {
        private const val ANIMATION_DURATION: Long = 300
        private const val TRANSLATION_Z: Float = 10f

        @SuppressLint("ClickableViewAccessibility")
        val onTouchListener: View.OnTouchListener = View.OnTouchListener { v, event ->
            var touchFlag = false
            var lastX = 0
            var lastY = 0
            val touchSlop = ViewConfiguration.get(Utils.getApp()).scaledTouchSlop

            when(event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchFlag = false
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()

                    val animator = ObjectAnimator.ofFloat(v, "translationZ", 0f, TRANSLATION_Z)
                    animator.duration = ANIMATION_DURATION
                    animator.start()
                }
                MotionEvent.ACTION_UP -> {
                    val animator = ObjectAnimator.ofFloat(v, "translationZ", TRANSLATION_Z, 0f)
                    animator.duration = ANIMATION_DURATION
                    animator.start()
                }
                MotionEvent.ACTION_MOVE -> {
                    touchFlag = true

                    val x = event.rawX.toInt() - lastX
                    val y = event.rawY.toInt() - lastY

                    if (x * x + y * y > touchSlop * touchSlop) {
                        val animator = ObjectAnimator.ofFloat(v, "translationZ", TRANSLATION_Z, 0f)
                        animator.duration = ANIMATION_DURATION
                        animator.start()
                    }
                }
            }
            touchFlag
        }
    }
}