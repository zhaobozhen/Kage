package com.absinthe.kage.ui.sender;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.R;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.utils.ToastUtil;

public class MusicActivity extends BaseActivity {

    public static final String EXTRA_MUSIC_INFO = "MUSIC_INFO";
    private LocalMusic mLocalMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        Intent intent = getIntent();
        if (intent != null) {
            getMusicInfo(intent);
        }

        ToastUtil.makeText(mLocalMusic.getTitle());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            getMusicInfo(intent);
        }
    }

    private void getMusicInfo(@NonNull Intent intent) {
        LocalMusic localMusic = intent.getParcelableExtra(EXTRA_MUSIC_INFO);
        if (localMusic != null) {
            mLocalMusic = localMusic;
        }
    }
}
