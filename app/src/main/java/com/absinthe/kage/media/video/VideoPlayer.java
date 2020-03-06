package com.absinthe.kage.media.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.session.PlaybackState;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.kage.R;
import com.absinthe.kage.databinding.ViewVideoPlayerBinding;
import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.Playback;

import java.util.Formatter;
import java.util.Locale;

public class VideoPlayer extends FrameLayout implements Playback.Callback {

    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_REMOTE = 2;

    private static final String TAG = VideoPlayer.class.getSimpleName();

    private Context mContext;
    private ViewVideoPlayerBinding mBinding;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private ImageView mIVCover;
    private ImageView mIVPause;
    private Playback mPlayback;
    private View mRoot;
    private SeekBar mSeekBar;

    private final OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            removeCallbacks(mShowProgress);
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mPlayback != null) {
                long newPosition = (((long) progress) * ((long) mPlayback.getDuration())) / 1000;
                mPlayback.seekTo((int) newPosition);
                if (mTVCurrentTime != null) {
                    mTVCurrentTime.setText(stringForTime((int) newPosition));
                }
            }
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            post(mShowProgress);
        }
    };
    private final OnClickListener mPauseListener = v -> doPauseResume();

    private boolean isLoaded;
    private boolean mDragging;
    private int mBeforePosition = 0;
    private int mPlayState = 0;

    private final Runnable mShowProgress = new Runnable() {
        public void run() {
            Log.d(TAG, "mShowProgress run");
            int pos = setProgress();
            if (!mDragging && isPlaying()) {
                postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };

    private LocalMedia mLocalMedia;
    private TextView mTVCurrentTime;
    private TextView mTVDurationTime;
    private VideoPlayCallback mVideoPlayCallback;
    private VideoView mVideoView;

    public interface VideoPlayCallback {
        void changeState(int i);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        LayoutParams frameParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        removeAllViews();
        addView(makeControllerView(), frameParams);
    }

    protected View makeControllerView() {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mBinding = ViewVideoPlayerBinding.inflate(mInflater);
        mRoot = mBinding.getRoot();
        initView();
        return mRoot;
    }

    private void initView() {
        mVideoView = mBinding.videoView;
        mIVCover = mBinding.ivCover;
        mSeekBar = mBinding.layoutSeekbar.seekbar.seekBar;
        mTVDurationTime = mBinding.layoutSeekbar.seekbar.tvDuration;
        mTVCurrentTime = mBinding.layoutSeekbar.seekbar.tvCurrentTime;
        mIVPause = mBinding.layoutSeekbar.ivPlay;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mIVPause.setOnClickListener(mPauseListener);
        mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mVideoView.setOnErrorListener((mp, what, extra) -> what == Integer.MIN_VALUE || what == -38);
    }

    public void playMedia(LocalMedia localMedia) {
        removeCallbacks(mShowProgress);
        mBeforePosition = 0;
        mLocalMedia = localMedia;
        if (mPlayback != null) {
            mPlayback.playMedia(localMedia);
        }
    }

    public void changePlayer(int type) {
        if (mPlayback != null) {
            mBeforePosition = mPlayback.getCurrentPosition();
            mPlayback.stop(true);
            mPlayback.setCallback(null);
            mPlayback = null;
        }
        if (type == TYPE_LOCAL) {
            mIVCover.setVisibility(GONE);
            mVideoView.setVisibility(VISIBLE);
            mPlayback = new LocalVideoPlayback(mVideoView);
            mPlayback.setCallback(this);
        } else if (type == TYPE_REMOTE) {
            mIVCover.setVisibility(VISIBLE);
            mVideoView.setVisibility(GONE);
            mPlayback = new RemoteVideoPlayback();
            mPlayback.setCallback(this);
        }
        if (mPlayback != null) {
            if (mLocalMedia != null) {
                mPlayback.playMedia(mLocalMedia);
            }
        }
    }

    public void release() {
        Log.d(TAG, "release");
        mPlayState = 0;
        Playback playback = mPlayback;
        if (playback != null) {
            playback.stop(false);
        }
    }

    public void setVideoPlayCallback(VideoPlayCallback callback) {
        mVideoPlayCallback = callback;
    }

    private void updatePausePlay() {
        if (mRoot != null && mIVPause != null) {
            if (isPlaying()) {
                Log.i(TAG, "updatePausePlay playing");
                post(mShowProgress);
                mIVPause.setImageResource(R.drawable.ic_pause);
            } else {
                Log.i(TAG, "updatePausePlay pause");
                removeCallbacks(mShowProgress);
                mIVPause.setImageResource(R.drawable.ic_play_arrow);
            }
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isLoaded) {
            requestCoverImage();
        }
    }

    private void requestCoverImage() {
        ImageView imageView = mIVCover;
        if (imageView != null && imageView.getWidth() > 0 && mLocalMedia != null) {
            Log.d(TAG, "mIVCover start load");
            Bitmap b = VideoHelper.getVideoCoverImage(mLocalMedia.getFilePath());
            isLoaded = true;
            mIVCover.setImageBitmap(b);
        }
    }

    public void onCompletion() {
        Log.d(TAG, "onCompletion");
        SeekBar seekBar = mSeekBar;
        seekBar.setProgress(seekBar.getMax());
        updatePausePlay();
    }

    public void onPlaybackStateChanged(int state) {
        if (mPlayback != null && mPlayState == PlaybackState.STATE_BUFFERING
                && state == PlaybackState.STATE_PLAYING && mBeforePosition > 0) {
            Log.d(TAG, "Seek to: " + mBeforePosition);
            mPlayback.seekTo(mBeforePosition);
        }
        if (mPlayState != state) {
            updatePausePlay();
            VideoPlayCallback videoPlayCallback = mVideoPlayCallback;
            if (videoPlayCallback != null) {
                videoPlayCallback.changeState(state);
            }
            mPlayState = state;
        }
    }

    public void onError(String error) {
    }

    public void onMediaMetadataChanged(LocalMedia localMedia) {
        updatePausePlay();
        post(mShowProgress);
    }

    private int setProgress() {
        int position = mPlayback.getCurrentPosition();
        if (position == 0) {
            return 0;
        }

        int duration = mPlayback.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                mSeekBar.setProgress((int) ((((long) position) * 1000) / ((long) duration)));
            }
            mSeekBar.setSecondaryProgress(mPlayback.getBufferPosition() * 10);
        }
        TextView textView = mTVDurationTime;
        if (textView != null) {
            textView.setText(stringForTime(duration));
        }
        textView = mTVCurrentTime;
        if (textView != null) {
            textView.setText(stringForTime(position));
        }
        return position;
    }

    private void doPauseResume() {
        if (mPlayback != null) {
            if (isPlaying()) {
                mPlayback.pause();
            } else {
                mPlayback.play();
            }
            updatePausePlay();
            post(mShowProgress);
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        mFormatBuilder.setLength(0);

        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        }
        return mFormatter.format("%02d:%02d", minutes, seconds).toString();
    }

    private boolean isPlaying() {
        return mPlayback != null && mPlayback.getState() == PlaybackState.STATE_PLAYING;
    }
}

