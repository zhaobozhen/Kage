package com.absinthe.kage.media.video;

import android.media.session.PlaybackState;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.Playback;

public class LocalVideoPlayback implements Playback {

    private static final String TAG = LocalVideoPlayback.class.getSimpleName();
    private Callback mCallback;
    private VideoView mVideoView;
    private int mPlayState = PlaybackState.STATE_NONE;

    public LocalVideoPlayback(VideoView videoView) {
        mVideoView = videoView;
        mVideoView.setOnPreparedListener(mp -> LocalVideoPlayback.this.play());
        mVideoView.setOnCompletionListener(mp -> {
            Log.i(TAG, "complete");
            mPlayState = PlaybackState.STATE_PAUSED;
            if (mCallback != null) {
                mCallback.onCompletion();
            }
        });
    }

    public void playMedia(LocalMedia localMedia) {
        this.mPlayState = PlaybackState.STATE_BUFFERING;
        if (mVideoView != null && mVideoView.getVisibility() == View.VISIBLE) {
            mVideoView.setVideoPath(localMedia.getFilePath());
        }
        if (mCallback != null) {
            mCallback.onMediaMetadataChanged(localMedia);
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }

    public void play() {
        this.mPlayState = PlaybackState.STATE_PLAYING;
        handlePlayState();
    }

    public void pause() {
        this.mPlayState = PlaybackState.STATE_PAUSED;
        handlePlayState();
    }

    public void seekTo(int position) {
        if (mVideoView != null) {
            mVideoView.seekTo(position);
        }
    }

    public int getState() {
        return mPlayState;
    }

    public int getDuration() {
        return mVideoView.getDuration();
    }

    public int getBufferPosition() {
        return mVideoView.getBufferPercentage();
    }

    public int getCurrentPosition() {
        return mVideoView.getCurrentPosition();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void stop(boolean fromUser) {
        if (fromUser) {
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    private void handlePlayState() {
        Log.i(TAG, "handlePlayState");
        if (mVideoView != null) {
            if (mPlayState == PlaybackState.STATE_PLAYING && !mVideoView.isPlaying()) {
                mVideoView.start();
            } else if (mPlayState == PlaybackState.STATE_PAUSED && mVideoView.isPlaying()) {
                mVideoView.pause();
            }
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }
}

