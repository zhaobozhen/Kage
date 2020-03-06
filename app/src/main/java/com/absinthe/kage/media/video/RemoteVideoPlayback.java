package com.absinthe.kage.media.video;

import android.media.session.PlaybackState;
import android.util.Log;

import com.absinthe.kage.connect.proxy.VideoProxy;
import com.absinthe.kage.device.model.VideoInfo;
import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.Playback;

public class RemoteVideoPlayback implements Playback {
    private static final String TAG = RemoteVideoPlayback.class.getSimpleName();
    private Callback mCallback;
    private VideoProxy mVideoProxy = VideoProxy.getInstance();
    private int mPlayState = PlaybackState.STATE_NONE;

    public RemoteVideoPlayback() {

        mVideoProxy.setOnPlayListener(new VideoProxy.OnPlayListener() {

            @Override
            public void onCurrentPositionChanged(int duration, int position) {

            }

            @Override
            public void onPlayStateChanged(int oldState, int newState) {
                if (newState == VideoProxy.PlayStatue.INVALIDATE) {
                    mPlayState = PlaybackState.STATE_NONE;
                } else if (newState == VideoProxy.PlayStatue.PLAYER_EXIT) {
                    mPlayState = PlaybackState.STATE_STOPPED;
                } else if (newState != VideoProxy.PlayStatue.DISCONNECT) {
                    switch (newState) {
                        case VideoProxy.PlayStatue.STOPPED:
                            mPlayState = PlaybackState.STATE_STOPPED;
                            break;
                        case VideoProxy.PlayStatue.TRANSITIONING:
                            mPlayState = PlaybackState.STATE_BUFFERING;
                            break;
                        case VideoProxy.PlayStatue.PLAYING:
                            mPlayState = PlaybackState.STATE_PLAYING;
                            break;
                        case VideoProxy.PlayStatue.PAUSED:
                            mPlayState = PlaybackState.STATE_PAUSED;
                            break;
                    }
                } else {
                    mPlayState = PlaybackState.STATE_STOPPED;
                }
                handlePlayState(false);
            }
        });
    }

    public void playMedia(LocalMedia localMedia) {
        if (localMedia != null) {
            VideoInfo mediaInfo = new VideoInfo();
            mediaInfo.setUrl(localMedia.getUrl());
            mediaInfo.setTitle(localMedia.getTitle());
            mVideoProxy.play(mediaInfo);
            Log.i(TAG, "playMedia");
            mPlayState = PlaybackState.STATE_BUFFERING;

            if (mCallback != null) {
                mCallback.onMediaMetadataChanged(localMedia);
                mCallback.onPlaybackStateChanged(mPlayState);
                return;
            }
            return;
        }
        mPlayState = PlaybackState.STATE_ERROR;
        if (mCallback != null) {
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }

    public void play() {
        mPlayState = PlaybackState.STATE_PLAYING;
        handlePlayState(true);
    }

    public void pause() {
        mPlayState = PlaybackState.STATE_PAUSED;
        handlePlayState(true);
    }

    public void seekTo(int position) {
        mVideoProxy.seekTo(position);
    }

    public int getState() {
        return mPlayState;
    }

    public int getDuration() {
        return mVideoProxy.getDuration();
    }

    public int getBufferPosition() {
        return 100;
    }

    public int getCurrentPosition() {
        return mVideoProxy.getCurrentPosition();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void stop(boolean fromUser) {
        mPlayState = PlaybackState.STATE_STOPPED;
        if (fromUser && mCallback != null) {
            mVideoProxy.stop();
        }
        mVideoProxy.recycle();
    }

    private void handlePlayState(boolean formUser) {
        if (formUser) {
            if (mPlayState == PlaybackState.STATE_PLAYING) {
                mVideoProxy.start();
            } else if (mPlayState == PlaybackState.STATE_PAUSED) {
                mVideoProxy.pause();
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }
}

