package com.absinthe.kage.ui.sender;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.databinding.ActivityMusicBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.DeviceObserverImpl;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.PlayList;
import com.absinthe.kage.media.audio.AudioPlayer;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.media.audio.MusicHelper;
import com.absinthe.kage.ui.connect.ConnectActivity;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.Observable;
import java.util.Observer;

public class MusicActivity extends BaseActivity implements Observer {

    public static final String EXTRA_MUSIC_INFO = "MUSIC_INFO";

    private ActivityMusicBinding mBinding;
    private LocalMusic mLocalMusic;
    private DeviceManager mDeviceManager;
    private IDeviceObserver deviceObserver;
    private AudioPlayer mAudioPlayer;

    private Handler mHandler = new Handler();
    private final Runnable mShowProgressTask = new Runnable() {
        public void run() {
            mHandler.postDelayed(mShowProgressTask, 1000 - (updatePlayPosition() % 1000));
        }
    };

    private boolean isSeekBarTouch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMusicBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Intent intent = getIntent();
        if (intent != null) {
            getMusicInfo(intent);
            initListener();
            initView();
            initPlayer();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMediaInfo(mAudioPlayer.getCurrentMedia(), false);
        updatePlayState(mAudioPlayer.getPlaybackState(), false);
        mAudioPlayer.addObserver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mShowProgressTask);
        mAudioPlayer.deleteObserver(this);
    }

    @Override
    protected void onDestroy() {
        mDeviceManager.unregister(deviceObserver);
        mAudioPlayer.release();
        super.onDestroy();
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
            mBinding.toolbar.tvMusicName.setText(mLocalMusic.getTitle());
            mBinding.toolbar.tvArtist.setText(mLocalMusic.getArtist());
        }
    }

    private void initView() {
        Window window = getWindow();
        View view = window.getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        window.setStatusBarColor(Color.TRANSPARENT);

        applyBlurBackground(mLocalMusic.getAlbumId());
        mBinding.toolbar.ibConnect.setSelected(mDeviceManager.isConnected());
    }

    private void initListener() {
        mDeviceManager = DeviceManager.Singleton.INSTANCE.getInstance();
        deviceObserver = new DeviceObserverImpl() {
            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                mBinding.toolbar.ibConnect.setSelected(true);
            }

            @Override
            public void onDeviceDisConnect(DeviceInfo deviceInfo) {
                mBinding.toolbar.ibConnect.setSelected(false);
            }
        };
        mDeviceManager.register(deviceObserver);
        mBinding.toolbar.ibBack.setOnClickListener(v -> finish());
        mBinding.toolbar.ibConnect.setOnClickListener(v ->
                startActivity(new Intent(MusicActivity.this, ConnectActivity.class)));
        mBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHandler.removeCallbacks(mShowProgressTask);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarTouch = true;
                if (mAudioPlayer != null) {
                    if (seekBar.getProgress() > seekBar.getSecondaryProgress()) {
                        seekBar.setProgress(seekBar.getSecondaryProgress());
                    }
                    mAudioPlayer.seekTo(seekBar.getProgress());
                    mHandler.post(mShowProgressTask);
                } else {
                    seekBar.setProgress(0);
                }
            }
        });
    }

    private void initPlayer() {
        mAudioPlayer = AudioPlayer.getInstance(this);
        mAudioPlayer.changePlayer(AudioPlayer.REPEAT_ONE);
        PlayList playList = new PlayList();
        playList.addMedia(mLocalMusic);
        mAudioPlayer.playMediaList(playList);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof PlaybackState) {
            updatePlayState((PlaybackState) arg, true);
        } else if (arg instanceof LocalMedia) {
            updateMediaInfo((LocalMedia) arg, true);
        }
    }

    private int updatePlayPosition() {
        if (mAudioPlayer == null) {
            return 0;
        }
        int max = mAudioPlayer.getDuration();
        int current = mAudioPlayer.getCurrentPosition();
        int buffer = mAudioPlayer.getBufferPosition();
        mBinding.seekBar.setMax(max);
        if (isSeekBarTouch) {
            isSeekBarTouch = false;
        } else {
            mBinding.seekBar.setProgress(current);
        }
        mBinding.seekBar.setSecondaryProgress(buffer);
        mBinding.tvCurrentTime.setText(LocalMedia.millisecondToTimeString(current));
        mBinding.tvDuration.setText(LocalMedia.millisecondToTimeString(max));
        return current;
    }

    private void updatePlayState(PlaybackState playbackState, boolean isNotify) {
        int state = playbackState.getState();
        if (state == 6) {
//            this.mStateIV.setImageResource(R.drawable.icon_pause_music);
        } else if (state == 3) {
            mHandler.post(mShowProgressTask);
//            startAnimator();
//            this.mStateIV.setImageResource(R.drawable.icon_pause_music);
        } else {
            mHandler.removeCallbacks(mShowProgressTask);
//            pauseAnimation();
//            this.mStateIV.setImageResource(R.drawable.icon_play_music);
        }

        long actions = playbackState.getActions();
        if ((16 & actions) != 0) {
//            this.mPreIV.setEnabled(true);
        } else {
//            this.mPreIV.setEnabled(false);
        }
        if ((32 & actions) != 0) {
//            this.mNextIV.setEnabled(true);
        } else {
//            this.mNextIV.setEnabled(false);
        }

        if (isNotify && state == 1) {
            finish();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateMediaInfo(LocalMedia media, boolean isNotify) {
        if (media != null) {
            if (isNotify) {
//                stopAnimation();
            }
            if (media instanceof LocalMusic) {
                mBinding.toolbar.tvMusicName.setText(media.getTitle());
                mBinding.toolbar.tvArtist.setText(((LocalMusic) media).getArtist());
                applyBlurBackground(((LocalMusic) media).getAlbumId());
                mHandler.post(mShowProgressTask);
            }
            mBinding.tvCurrentTime.setText("00:00");
            mBinding.tvDuration.setText(LocalMedia.millisecondToTimeString((int) media.getDuration()));
        }
    }

    private void applyBlurBackground(int albumId) {
        Glide.with(this)
                .asBitmap()
                .load(MusicHelper.getAlbumArt(this, albumId))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap result = ImageUtils.renderScriptBlur(resource, 25);
                        mBinding.ivBackground.setImageBitmap(result);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
}
