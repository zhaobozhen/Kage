package com.absinthe.kage.media.audio;

import android.content.Context;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.PlayList;
import com.absinthe.kage.media.Playback;

import java.util.Observable;

public class AudioPlayer extends Observable implements Playback.Callback {
    public static final String TAG = AudioPlayer.class.getSimpleName();
    public static final String EXTRA_PLAY_MODE = "EXTRA_PLAY_MODE";
    public static final int NOT_REPEATING = 0;
    public static final int REPEAT_ONE = 1;
    public static final int SHUFFLED = 2;
    public static final int REPEAT_ALL = 3;

    private static AudioPlayer sInstance;
    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Playback mPlayback;
    private PlayList mPlaylist;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private int mBeforePosition = 0;
    private int mPlayMode = 3;
    private int mPlayState = 0;
    private int mPlayType;

    public static AudioPlayer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AudioPlayer(context);
        }
        return sInstance;
    }

    private AudioPlayer(Context context) {
        mContext = context;

        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
        }
    }

    public void release() {
        mPlayState = 0;
        Playback playback = mPlayback;
        if (playback != null) {
            playback.stop(false);
        }
        deleteObservers();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        mWifiLock = null;
        sInstance = null;
        mContext = null;
    }

    public void changePlayer(int type) {
        mPlayType = type;
        if (mPlayback != null) {
            mBeforePosition = mPlayback.getCurrentPosition();
            mPlayback.stop(true);
        }
        if (type == REPEAT_ONE) {
            mPlayback = new LocalAudioPlayback(mContext);
            mPlayback.setCallback(this);

            if (mPlaylist != null) {
                mPlayback.playMedia(mPlaylist.getCurrentMedia());
            }
        } else if (type == SHUFFLED) {
            RemoteAudioPlayback playback = new RemoteAudioPlayback();
            playback.setCallback(this);
            if (mPlaylist != null) {
                playback.playListMedia(mPlaylist);
            }
            mPlayback = playback;
        }
    }

    public void playMediaList(PlayList playList) {
        mBeforePosition = 0;
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }

        if (playList != null) {
            if (mPlaylist == null) {
                mPlaylist = new PlayList();
            }
            Log.d(TAG, "beforeIndex: " + mPlaylist.getCurrentIndex() + ", newIndex: " + playList.getCurrentIndex());
            mPlaylist.setList(playList.getList(), playList.getCurrentIndex());
            Log.d(TAG, "afterIndex: " + mPlaylist.getCurrentIndex());

            if (mPlayback instanceof RemoteAudioPlayback) {
                ((RemoteAudioPlayback) mPlayback).playListMedia(mPlaylist);
            } else if (mPlayback instanceof LocalAudioPlayback) {
                mPlayback.playMedia(mPlaylist.getCurrentMedia());
            }
        }
    }

    public void play() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }
        mPlayback.play();
    }

    public void pause() {
        mPlayback.pause();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void playNext() {
        if (mPlaylist != null) {
            int i = mPlayMode;
            boolean z = false;
            boolean z2 = i == 1 || i == 3;
            if (mPlayMode == 2) {
                z = true;
            }
            playMedia(mPlaylist.getNextMedia(z2, z));
        }
    }

    public void playPrevious() {
        if (mPlaylist != null) {
            int i = mPlayMode;
            boolean z = false;
            boolean z2 = i == 1 || i == 3;
            if (mPlayMode == 2) {
                z = true;
            }
            playMedia(mPlaylist.getPreviousMedia(z2, z));
        }
    }

    public void seekTo(int to) {
        mPlayback.seekTo(to);
    }

    public LocalMedia getCurrentMedia() {
        return mPlaylist == null ? null : mPlaylist.getCurrentMedia();
    }

    public int getPlaySate() {
        return mPlayState;
    }

    public PlaybackState getPlaybackState() {
        long actions;
        mPlayState = mPlayback.getState();
        Log.d(TAG, "getPlaybackState mPlayState: " + mPlayState);

        if (mPlayState == 3) {
            actions = 1 | 330;
        } else {
            actions = 1 | 4;
        }
        if (hasNext()) {
            actions |= 32;
        }
        if (hasPre()) {
            actions |= 16;
        }
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_PLAY_MODE, mPlayMode);
        PlaybackState.Builder builder = new PlaybackState.Builder();
        builder.setActions(actions);
        builder.setState(mPlayState, (long) mPlayback.getCurrentPosition(), 1.0f);
        builder.setExtras(extras);
        if (mPlaylist != null) {
            builder.setActiveQueueItemId((long) mPlaylist.getCurrentIndex());
        }
        return builder.build();
    }

    public int getDuration() {
        if (isPlayOrPause()) {
            return mPlayback.getDuration();
        }
        return 0;
    }

    public int getBufferPosition() {
        if (isPlayOrPause()) {
            return mPlayback.getBufferPosition();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (isPlayOrPause()) {
            return mPlayback.getCurrentPosition();
        }
        return 0;
    }

    public void onCompletion() {
        if (mPlayState == 3) {
            playNext();
        } else {
            Log.w(TAG, "mPlayState is not playing");
        }
    }

    public void onPlaybackStateChanged(int state) {
        Log.d(TAG, "onPlaybackStateChanged before state: " + mPlayState + ", after state: " + state);

        if (mPlayback != null && mPlayState == 6 && state == 3 && mBeforePosition > 0) {
            Log.d(TAG, "seekTo: " + mBeforePosition);
            mPlayback.seekTo(mBeforePosition);
            mBeforePosition = 0;
        }
        if (mPlayState != state) {
            updateMediaPlayState();
            mPlayState = state;
        }
    }

    public void onError(String error) {
        mPlayState = 7;
        updateMediaPlayState();
    }

    public void onMediaMetadataChanged(LocalMedia localMedia) {
        updateMediaMetadata(localMedia);
    }

    public int getPlayType() {
        return mPlayType;
    }

    private void updateMediaMetadata(final LocalMedia media) {
        mHandler.post(new Runnable() {
            public void run() {
                synchronized (this) {
                    setChanged();
                    notifyObservers(media);
                }
            }
        });
    }

    private void updateMediaPlayState() {
        mHandler.post(new Runnable() {
            public void run() {
                synchronized (this) {
                    setChanged();
                    notifyObservers(getPlaybackState());
                }
            }
        });
    }

    private boolean hasNext() {
        if (mPlayMode == 0) {
            return mPlaylist != null && mPlaylist.hasNextMedia();
        }
        return true;
    }

    private boolean hasPre() {
        if (mPlayMode == 0) {
            return mPlaylist != null && mPlaylist.hasPreviousMedia();
        }
        return true;
    }

    private boolean isPlayOrPause() {
        if (getCurrentMedia() != null) {
            int i = mPlayState;
            return i == 3 || i == 2;
        }
        return false;
    }

    private synchronized void playMedia(LocalMedia media) {
        mBeforePosition = 0;
        mPlayback.playMedia(media);
    }
}

