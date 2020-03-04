package com.absinthe.kage.media.audio;

import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.util.Log;

import com.absinthe.kage.KageApplication;
import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.proxy.AudioProxy;
import com.absinthe.kage.device.model.AudioInfo;
import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.PlayList;
import com.absinthe.kage.media.Playback;
import com.absinthe.kage.utils.NetUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RemoteAudioPlayback implements Playback {
    private static final String TAG = RemoteAudioPlayback.class.getSimpleName();

    private Callback mCallback;
    private AudioProxy mAudioProxy = AudioProxy.getInstance();
    private PlayList mPlayList;
    private int mPlayState = PlaybackState.STATE_NONE;

    RemoteAudioPlayback() {
        mAudioProxy.setOnPlayListener(new AudioProxy.OnPlayListener() {

            @Override
            public void onCurrentPositionChanged(int duration, int position) {

            }

            @Override
            public void onPlayStateChanged(int oldState, int newState) {
                Log.d(TAG, "onPlayStateChanged: " + newState);
                if (newState == AudioProxy.PlayStatue.INVALIDATE) {
                    mPlayState = PlaybackState.STATE_NONE;
                } else if (newState == AudioProxy.PlayStatue.PLAYER_EXIT) {
                    mPlayState = PlaybackState.STATE_STOPPED;
                } else if (newState != AudioProxy.PlayStatue.DISCONNECT) {
                    switch (newState) {
                        case AudioProxy.PlayStatue.STOPPED:
                            mPlayState = PlaybackState.STATE_STOPPED;
                            break;
                        case AudioProxy.PlayStatue.TRANSITIONING:
                            mPlayState = PlaybackState.STATE_BUFFERING;
                            break;
                        case AudioProxy.PlayStatue.PLAYING:
                            mPlayState = PlaybackState.STATE_PLAYING;
                            break;
                        case AudioProxy.PlayStatue.PAUSED:
                            mPlayState = PlaybackState.STATE_PAUSED;
                            break;
                    }
                } else {
                    mPlayState = PlaybackState.STATE_STOPPED;
                }
                handlePlayState(false);
            }

            @Override
            public void onPlayIndexChanged(int index) {
                Log.d(TAG, "onPlayIndexChanged: " + index);
                mPlayList.setCurrentIndex(index);
                if (mCallback != null) {
                    mCallback.onMediaMetadataChanged(mPlayList.getCurrentMedia());
                }
            }
        });
    }

    public void playListMedia(PlayList playlist) {
        mPlayList = playlist;
        List<AudioInfo> audioInfos = new ArrayList<>();

        for (LocalMedia media : playlist.getList()) {
            if (media instanceof LocalMusic) {
                AudioInfo audioInfo = new AudioInfo();
                audioInfo.setName(media.getTitle());
                audioInfo.setUrl(media.getUrl());
                audioInfo.setArtist(((LocalMusic) media).getArtist());
                audioInfo.setAlbum(((LocalMusic) media).getAlbum());
                audioInfos.add(audioInfo);
            }
        }

        mAudioProxy.playList(mPlayList.getCurrentIndex(), audioInfos);
        mPlayState = PlaybackState.STATE_BUFFERING;

        if (mCallback != null) {
            mCallback.onMediaMetadataChanged(mPlayList.getCurrentMedia());
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }

    @Override
    public void playMedia(LocalMedia localMedia) {
        if (localMedia instanceof LocalMusic) {
            AudioInfo info = new AudioInfo();
            info.setName(localMedia.getTitle());
            info.setUrl(localMedia.getUrl());
            info.setArtist(((LocalMusic) localMedia).getArtist());
            info.setAlbum(((LocalMusic) localMedia).getAlbum());

            String ip = NetUtils.getLocalAddress();
            if (!TextUtils.isEmpty(ip)) {
                info.setCoverPath(String.format(Const.HTTP_SERVER_FORMAT, ip)
                        + KageApplication.sContext.getExternalCacheDir()
                        + File.separator + ((LocalMusic) localMedia).getAlbumId() + ".png");
            }

            mAudioProxy.play(info);
            mPlayState = PlaybackState.STATE_BUFFERING;

            if (mCallback != null) {
                mCallback.onMediaMetadataChanged(localMedia);
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    @Override
    public void play() {
        mPlayState = PlaybackState.STATE_PLAYING;
        handlePlayState(true);
    }

    @Override
    public void pause() {
        mPlayState = PlaybackState.STATE_PAUSED;
        handlePlayState(true);
    }

    @Override
    public void seekTo(int position) {
        mAudioProxy.seekTo(position);
    }

    @Override
    public int getState() {
        return mPlayState;
    }

    @Override
    public int getDuration() {
        return mAudioProxy.getDuration();
    }

    @Override
    public int getBufferPosition() {
        return mAudioProxy.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mAudioProxy.getCurrentPosition();
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void stop(boolean fromUser) {
        mPlayState = PlaybackState.STATE_STOPPED;

        if (fromUser && mCallback != null) {
            mAudioProxy.stop();
            mCallback.onPlaybackStateChanged(mPlayState);
        }

        mAudioProxy.recycle();
    }

    private void handlePlayState(boolean fromUser) {
        if (fromUser) {
            if (mPlayState == PlaybackState.STATE_PLAYING) {
                mAudioProxy.start();
            } else if (mPlayState == PlaybackState.STATE_PAUSED) {
                mAudioProxy.pause();
            }
        }

        if (mCallback != null) {
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }
}

