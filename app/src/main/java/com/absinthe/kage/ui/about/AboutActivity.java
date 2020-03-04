package com.absinthe.kage.ui.about;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.absinthe.kage.BuildConfig;
import com.absinthe.kage.R;
import com.drakeet.about.AbsAboutActivity;
import com.drakeet.about.Card;
import com.drakeet.about.Category;
import com.drakeet.about.Contributor;
import com.drakeet.about.License;

import java.util.List;

public class AboutActivity extends AbsAboutActivity {

    @Override
    protected void onCreateHeader(@NonNull ImageView icon, @NonNull TextView slogan, @NonNull TextView version) {
        icon.setImageResource(R.drawable.pic_splash);
        slogan.setText(R.string.app_name);
        version.setText(String.format("Version: %s", BuildConfig.VERSION_NAME));
    }

    @Override
    protected void onItemsCreated(@NonNull List<Object> items) {
        items.add(new Category("What's this"));
        items.add(new Card(getString(R.string.about_info)));

        items.add(new Category("Developers"));
        items.add(new Contributor(R.mipmap.pic_rabbit, "Absinthe", "Developer & Designer", "https://www.coolapk.com/u/482045"));

        items.add(new Category("Open Source Licenses"));
        items.add(new License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"));
        items.add(new License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"));
        items.add(new License("BaseRecyclerViewAdapterHelper", "CymChad", License.MIT, "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"));
        items.add(new License("Matisse", "zhihu", License.APACHE_2, "https://github.com/zhihu/Matisse"));
        items.add(new License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"));
        items.add(new License("Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"));
        items.add(new License("RxAndroid", "JakeWharton", License.APACHE_2, "https://github.com/ReactiveX/RxAndroid"));
        items.add(new License("RxJava", "ReactiveX", License.APACHE_2, "https://github.com/ReactiveX/RxJava"));
        items.add(new License("RxPermission", "tbruyelle", License.APACHE_2, "https://github.com/tbruyelle/RxPermissions"));
        items.add(new License("imagezoom", "sephiroth74", License.MIT, "https://github.com/sephiroth74/ImageViewZoom"));
        items.add(new License("glide", "bumptech", License.APACHE_2, "https://github.com/bumptech/glide"));
        items.add(new License("gson", "google", License.APACHE_2, "https://github.com/google/gson"));
        items.add(new License("AndroidUtilCode", "Blankj", License.APACHE_2, "https://github.com/Blankj/AndroidUtilCode"));

    }
}