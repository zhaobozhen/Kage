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
import android.net.Uri;
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
import com.absinthe.kage.utils.Logger;
import com.absinthe.kage.utils.StorageUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
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
    private int type = TYPE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMusicBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        initListener();
        initView();

        processIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMediaInfo(mAudioPlayer.getCurrentMedia());
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
    public void update(Observable o, Object arg) {
        if (arg instanceof PlaybackState) {
            updatePlayState((PlaybackState) arg, true);
        } else if (arg instanceof LocalMedia) {
            updateMediaInfo((LocalMedia) arg);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            getMusicInfo(intent);
            initPlayer();
        } else {
            finish();
        }
    }

    private void getMusicInfo(@NonNull Intent intent) {
        LocalMusic localMusic = intent.getParcelableExtra(EXTRA_MUSIC_INFO);
        if (localMusic != null) {
            mLocalMusic = localMusic;
        }

        type = intent.getIntExtra(EXTRA_DEVICE_TYPE, TYPE_NONE);
        switch (type) {
            case TYPE_NONE:
                finish();
                break;
            case TYPE_SENDER:
                mBinding.btnCast.setVisibility(View.VISIBLE);
                mBinding.toolbar.ibConnect.setVisibility(View.VISIBLE);
                break;
            case TYPE_RECEIVER:
                mBinding.btnCast.setVisibility(View.GONE);
                mBinding.toolbar.ibConnect.setVisibility(View.GONE);
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
            if (DeviceManager.Singleton.INSTANCE.getInstance().isConnected()) {
                mAudioPlayer.setPlayerType(AudioPlayer.TYPE_REMOTE);
            } else {
                startActivity(new Intent(MusicActivity.this, ConnectActivity.class));
            }
        });
        mBinding.layoutControls.btnPlay.setOnClickListener(v -> {
            int state = mAudioPlayer.getPlayState();
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_play_arrow);
                mAudioPlayer.pause();
            } else if (state == PlaybackState.STATE_PAUSED) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause);
                mAudioPlayer.resumePlay();
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
        mAudioPlayer.setPlayerType(AudioPlayer.TYPE_LOCAL);
        mAudioPlayer.playMedia(mLocalMusic);

        if (type == TYPE_SENDER) {
            PlayList playList = new PlayList();

            for (LocalMedia localMedia : MusicListActivity.sMusicList) {
                playList.addMedia(localMedia);
            }
            playList.setCurrentIndex(MusicListActivity.sMusicList.indexOf(mLocalMusic));

            mAudioPlayer.playMediaList(playList);
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
        Logger.d("state:",state);
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
    private void updateMediaInfo(LocalMedia media) {
        if (media != null) {
            if (media instanceof LocalMusic) {
                mBinding.toolbar.tvMusicName.setText(media.getTitle());
                mBinding.toolbar.tvArtist.setText(((LocalMusic) media).getArtist());
                if (type == TYPE_SENDER) {
                    applyRouletteAndBlurBackground(((LocalMusic) media).getAlbumId());
                } else if (type == TYPE_RECEIVER) {
                    applyRouletteAndBlurBackground(Uri.parse(((LocalMusic) media).getCoverPath()));
                }
                mHandler.post(mShowProgressTask);
            }
            mBinding.layoutSeekBar.tvCurrentTime.setText("00:00");
            mBinding.layoutSeekBar.tvDuration.setText(LocalMedia.millisecondToTimeString((int) media.getDuration()));
        }
    }

    private void applyRouletteAndBlurBackground(int albumId) {
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

                        saveAlbumBitmap(resource, albumId);

                        Glide.with(getApplicationContext())
                                .load(resource)
                                .into(mBinding.musicRoulette);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void applyRouletteAndBlurBackground(Uri uri) {
        Glide.with(this)
                .asBitmap()
                .load(uri)
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

                        Glide.with(getApplicationContext())
                                .load(resource)
                                .into(mBinding.musicRoulette);
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

    private void pauseAnimation() {
        if (mObjectAnimator != null && mObjectAnimator.isStarted()) {
            mObjectAnimator.cancel();
        }
    }

    private void stopAnimation() {
        if (mObjectAnimator != null) {
            if (mObjectAnimator.isStarted() || mObjectAnimator.isRunning()) {
                mObjectAnimator.end();
            }
            mCurrentRotation = 0.0f;
        }
    }

    private void saveAlbumBitmap(Bitmap bitmap, int albumId) {
        File file = new File(getExternalCacheDir(), albumId + ".png");
        StorageUtils.saveBitmap(bitmap, file);
    }
}
