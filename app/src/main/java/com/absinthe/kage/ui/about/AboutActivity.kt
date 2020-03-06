package com.absinthe.kage.ui.about

import android.widget.ImageView
import android.widget.TextView
import com.absinthe.kage.BuildConfig
import com.absinthe.kage.R
import com.drakeet.about.*

class AboutActivity : AbsAboutActivity() {

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.drawable.pic_splash)
        slogan.setText(R.string.app_name)
        version.text = String.format("Version: %s", BuildConfig.VERSION_NAME)
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("What's this"))
        items.add(Card(getString(R.string.about_info)))

        items.add(Category("Developers"))
        items.add(Contributor(R.mipmap.pic_rabbit, "Absinthe", "Developer & Designer", "https://www.coolapk.com/u/482045"))

        items.add(Category("Open Source Licenses"))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
        items.add(License("BaseRecyclerViewAdapterHelper", "CymChad", License.MIT, "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"))
        items.add(License("Matisse", "zhihu", License.APACHE_2, "https://github.com/zhihu/Matisse"))
        items.add(License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"))
        items.add(License("Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"))
        items.add(License("RxAndroid", "JakeWharton", License.APACHE_2, "https://github.com/ReactiveX/RxAndroid"))
        items.add(License("RxJava", "ReactiveX", License.APACHE_2, "https://github.com/ReactiveX/RxJava"))
        items.add(License("RxPermission", "tbruyelle", License.APACHE_2, "https://github.com/tbruyelle/RxPermissions"))
        items.add(License("imagezoom", "sephiroth74", License.MIT, "https://github.com/sephiroth74/ImageViewZoom"))
        items.add(License("glide", "bumptech", License.APACHE_2, "https://github.com/bumptech/glide"))
        items.add(License("gson", "google", License.APACHE_2, "https://github.com/google/gson"))
        items.add(License("AndroidUtilCode", "Blankj", License.APACHE_2, "https://github.com/Blankj/AndroidUtilCode"))
    }
}