package com.absinthe.kage.media.audio;

import android.util.Log;

import com.absinthe.kage.connect.proxy.AudioProxy;
import com.absinthe.kage.device.model.AudioInfo;
import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.PlayList;
import com.absinthe.kage.media.Playback;

import java.util.ArrayList;
import java.util.List;

public class RemoteAudioPlayback implements Playback {
    private static final String TAG = RemoteAudioPlayback.class.getSimpleName();

    private Callback mCallback;
    private AudioProxy mAudioProxy = AudioProxy.getInstance();
    private PlayList mPlayList;
    private int mPlayState = 0;

    public RemoteAudioPlayback() {
        mAudioProxy.setOnPlayListener(new AudioProxy.OnPlayListener() {

            @Override
            public void onCurrentPositionChanged(int duration, int position) {

            }

            @Override
            public void onPlayStateChanged(int oldState, int newState) {
                Log.d(TAG, "onPlayStateChanged: " + newState);
                if (newState == -1) {
                    mPlayState = 0;
                } else if (newState == 11) {
                    mPlayState = 1;
                } else if (newState != 20) {
                    switch (newState) {
                        case 1:
                            mPlayState = 1;
                            break;
                        case 2:
                            mPlayState = 6;
                            break;
                        case 3:
                            mPlayState = 3;
                            break;
                        case 4:
                            mPlayState = 2;
                            break;
                    }
                } else {
                    mPlayState = 1;
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

            @Override
            public void onMusicPlayModeChanged(int mode) {

            }
        });
    }

    public void playMedia(LocalMedia localMedia) {
        if (localMedia != null) {
            int index = mPlayList.queryMediaIndex(localMedia);
            if (index >= 0) {
                mAudioProxy.setPlayIndex(index);
            }
            mPlayState = 6;
            if (mCallback != null) {
                mCallback.onMediaMetadataChanged(localMedia);
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
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
        mPlayState = 6;
        if (mCallback != null) {
            mCallback.onMediaMetadataChanged(mPlayList.getCurrentMedia());
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }

    public void play() {
        mPlayState = 3;
        handlePlayState(true);
    }

    public void pause() {
        mPlayState = 2;
        handlePlayState(true);
    }

    public void seekTo(int position) {
        mAudioProxy.seekTo(position);
    }

    public int getState() {
        return mPlayState;
    }

    public int getDuration() {
        return mAudioProxy.getDuration();
    }

    public int getBufferPosition() {
        return mAudioProxy.getDuration();
    }

    public int getCurrentPosition() {
        return mAudioProxy.getCurrentPosition();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void stop(boolean fromUser) {
        mPlayState = 1;
        if (fromUser && mCallback != null) {
            mAudioProxy.stop();
            mCallback.onPlaybackStateChanged(mPlayState);
        }
        mAudioProxy.recycle();
    }

    private void handlePlayState(boolean fromUser) {
        if (fromUser) {
            int i = mPlayState;
            if (i == 3) {
                mAudioProxy.start();
            } else if (i == 2) {
                mAudioProxy.pause();
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStateChanged(mPlayState);
        }
    }
}

