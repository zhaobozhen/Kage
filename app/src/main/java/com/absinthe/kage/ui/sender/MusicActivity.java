package com.absinthe.kage.ui.sender;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.R;
import com.absinthe.kage.connect.proxy.AudioProxy;
import com.absinthe.kage.databinding.ActivityMusicBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.DeviceObserverImpl;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.AudioInfo;
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
    public static final String EXTRA_DEVICE_TYPE = "DEVICE_TYPE";
    public static final int TYPE_NONE = -1;
    public static final int TYPE_SENDER = 0;
    public static final int TYPE_RECEIVER = 1;

    private ActivityMusicBinding mBinding;
    private LocalMusic mLocalMusic;
    private DeviceManager mDeviceManager;
    private IDeviceObserver deviceObserver;
    private AudioPlayer mAudioPlayer;
    private ObjectAnimator mObjectAnimator;

    private Handler mHandler = new Handler();
    private final Runnable mShowProgressTask = new Runnable() {
        public void run() {
            mHandler.postDelayed(mShowProgressTask, 1000 - (updatePlayPosition() % 1000));
        }
    };

    private boolean isSeekBarTouch = false;
    private float mCurrentRotation = 0.0f;

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
        pauseAnimation();
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

        int launchType = intent.getIntExtra(EXTRA_DEVICE_TYPE, TYPE_NONE);
        switch (launchType) {
            case TYPE_NONE:
                finish();
                break;
            case TYPE_SENDER:
                mBinding.btnCast.setVisibility(View.VISIBLE);
                break;
            case TYPE_RECEIVER:
                mBinding.btnCast.setVisibility(View.GONE);
                break;
            default:
        }
    }

    private void initView() {
        Window window = getWindow();
        View view = window.getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        applyBlurBackground(mLocalMusic.getAlbumId());
        applyRouletteAlbum(mLocalMusic.getAlbumId());
        mBinding.toolbar.ibConnect.setSelected(mDeviceManager.isConnected());
        initAnimator();
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
        mBinding.layoutSeekBar.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        mBinding.btnCast.setOnClickListener(v -> {
            AudioInfo audioInfo = new AudioInfo();
            audioInfo.setName(mLocalMusic.getTitle());
            audioInfo.setAlbum(mLocalMusic.getAlbum());
            audioInfo.setArtist(mLocalMusic.getArtist());
            audioInfo.setCoverPath("null");
            audioInfo.setUrl(mLocalMusic.getUrl());
            AudioProxy.getInstance().play(audioInfo);
        });
        mBinding.layoutControls.btnPlay.setOnClickListener(v -> {
            int state = mAudioPlayer.getPlaySate();
            if (state == PlaybackState.STATE_PLAYING) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_play_arrow);
                mAudioPlayer.pause();
            } else if (state == PlaybackState.STATE_PAUSED) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause);
                mAudioPlayer.play();
            }
        });
        mBinding.layoutControls.btnPrevious.setOnClickListener(v -> {
            if (mAudioPlayer != null) {
                mAudioPlayer.playPrevious();
            }
        });
        mBinding.layoutControls.btnNext.setOnClickListener(v -> {
            if (mAudioPlayer != null) {
                mAudioPlayer.playNext();
            }
        });
    }

    private void initPlayer() {
        mAudioPlayer = AudioPlayer.getInstance(this);
        mAudioPlayer.changePlayer(AudioPlayer.REPEAT_ONE);
        PlayList playList = new PlayList();
        for (LocalMusic localMusic : MusicListActivity.sMusicList) {
            playList.addMedia(localMusic);
        }
        playList.setCurrentIndex(MusicListActivity.sMusicList.indexOf(mLocalMusic));
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
        mBinding.layoutSeekBar.seekBar.setMax(max);

        if (isSeekBarTouch) {
            isSeekBarTouch = false;
        } else {
            mBinding.layoutSeekBar.seekBar.setProgress(current);
        }

        mBinding.layoutSeekBar.seekBar.setSecondaryProgress(buffer);
        mBinding.layoutSeekBar.tvCurrentTime.setText(LocalMedia.millisecondToTimeString(current));
        mBinding.layoutSeekBar.tvDuration.setText(LocalMedia.millisecondToTimeString(max));
        return current;
    }

    private void updatePlayState(PlaybackState playbackState, boolean isNotify) {
        int state = playbackState.getState();
        if (state == PlaybackState.STATE_BUFFERING) {
            mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause);
        } else if (state == PlaybackState.STATE_PLAYING) {
            mHandler.post(mShowProgressTask);
            startAnimation();
            mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause);
        } else {
            mHandler.removeCallbacks(mShowProgressTask);
            pauseAnimation();
            mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_play_arrow);
        }

        long actions = playbackState.getActions();
        if ((PlaybackState.ACTION_SKIP_TO_PREVIOUS & actions) != 0) {
            mBinding.layoutControls.btnPrevious.setEnabled(true);
        } else {
            mBinding.layoutControls.btnPrevious.setEnabled(false);
        }
        if ((PlaybackState.ACTION_SKIP_TO_NEXT & actions) != 0) {
            mBinding.layoutControls.btnNext.setEnabled(true);
        } else {
            mBinding.layoutControls.btnNext.setEnabled(false);
        }

        if (isNotify && state == PlaybackState.STATE_STOPPED) {
            finish();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateMediaInfo(LocalMedia media, boolean isNotify) {
        if (media != null) {
            if (isNotify) {
                stopAnimation();
            }
            if (media instanceof LocalMusic) {
                mBinding.toolbar.tvMusicName.setText(media.getTitle());
                mBinding.toolbar.tvArtist.setText(((LocalMusic) media).getArtist());
                applyBlurBackground(((LocalMusic) media).getAlbumId());
                applyRouletteAlbum(((LocalMusic) media).getAlbumId());
                mHandler.post(mShowProgressTask);
            }
            mBinding.layoutSeekBar.tvCurrentTime.setText("00:00");
            mBinding.layoutSeekBar.tvDuration.setText(LocalMedia.millisecondToTimeString((int) media.getDuration()));
        }
    }

    private void applyRouletteAlbum(int albumId) {
        Glide.with(this)
                .load(MusicHelper.getAlbumArt(this, albumId))
                .into(mBinding.musicRoulette);
    }

    private void applyBlurBackground(int albumId) {
        Glide.with(this)
                .asBitmap()
                .load(MusicHelper.getAlbumArt(this, albumId))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap result = ImageUtils.renderScriptBlur(resource, 25);

                        ColorMatrix cMatrix = new ColorMatrix();
                        int brightness = -70;
                        cMatrix.set(new float[] {
                                1, 0, 0, 0, brightness,
                                0, 1, 0, 0, brightness,
                                0, 0, 1, 0, brightness,
                                0, 0, 0, 1, 0 });

                        Paint paint = new Paint();
                        paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));

                        Canvas canvas = new Canvas(result);
                        // 在 Canvas 上绘制一个已经存在的 Bitmap
                        canvas.drawBitmap(result, 0, 0, paint);

                        mBinding.ivBackground.setImageBitmap(result);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void initAnimator() {
        mCurrentRotation = 0.0f;
        if (mObjectAnimator == null) {
            mObjectAnimator = new ObjectAnimator();
            mObjectAnimator.setTarget(mBinding.musicRoulette);
            mObjectAnimator.setPropertyName("rotation");
            mObjectAnimator.setDuration(20000);
            mObjectAnimator.setInterpolator(new LinearInterpolator());
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.setRepeatMode(ValueAnimator.RESTART);
            mObjectAnimator.addUpdateListener(animation ->
                    mCurrentRotation = (Float) animation.getAnimatedValue());
        }
        if (mObjectAnimator.isStarted()) {
            mObjectAnimator.cancel();
        }
    }

    private void startAnimation() {
        if (mObjectAnimator != null && !mObjectAnimator.isStarted()) {
            mObjectAnimator.setFloatValues(mCurrentRotation, mCurrentRotation + 360.0f);
            mObjectAnimator.start();
        }
    }

    public void pauseAnimation() {
        if (mObjectAnimator != null && mObjectAnimator.isStarted()) {
            mObjectAnimator.cancel();
        }
    }

    public void stopAnimation() {
        if (mObjectAnimator != null) {
            if (mObjectAnimator.isStarted() || mObjectAnimator.isRunning()) {
                mObjectAnimator.end();
            }
            mCurrentRotation = 0.0f;
        }
    }
}
