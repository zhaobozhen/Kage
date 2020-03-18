package com.absinthe.kage.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.absinthe.kage.R
import com.google.android.material.card.MaterialCardView

class CategoryCardView : MaterialCardView {

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_category_card_view, this)
        val a = context!!.obtainStyledAttributes(attrs, R.styleable.CategoryCardView)

        findViewById<TextView>(R.id.tv_title)?.text = a.getString(R.styleable.CategoryCardView_categoryTitle)
        findViewById<ImageView>(R.id.image)?.setImageResource(a.getResourceId(R.styleable.CategoryCardView_categoryImage, 0))

        a.recycle()

        isClickable = true
        isFocusable = true
    }

}