package com.absinthe.kage.media.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Build;
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

    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_REMOTE = 2;

    @SuppressLint("StaticFieldLeak")
    private static AudioPlayer sInstance;
    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Playback mPlayback;
    private PlayList mPlaylist;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private int mBeforePosition = 0;
    private int mPlayMode = REPEAT_ALL;
    private int mPlayState = PlaybackState.STATE_NONE;
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
        mPlayState = PlaybackState.STATE_NONE;
        if (mPlayback != null) {
            mPlayback.stop(false);
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

    public void setPlayerType(int type) {
        mPlayType = type;

        if (mPlayback != null) {
            mBeforePosition = mPlayback.getCurrentPosition();
            mPlayback.stop(true);
        }

        if (type == TYPE_LOCAL) {
            mPlayback = new LocalAudioPlayback(mContext);
            mPlayback.setCallback(this);

            if (mPlaylist != null) {
                mPlayback.playMedia(mPlaylist.getCurrentMedia());
            }
        } else if (type == TYPE_REMOTE) {
            mPlayback = new RemoteAudioPlayback();
            mPlayback.setCallback(this);

            if (mPlaylist != null) {
                ((RemoteAudioPlayback) mPlayback).playListMedia(mPlaylist);
            }
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

            mPlaylist.setList(playList.getList(), playList.getCurrentIndex());

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
            playMedia(mPlaylist.getNextMedia(
                    mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                    mPlayMode == SHUFFLED));
        }
    }

    public void playPrevious() {
        if (mPlaylist != null) {
            playMedia(mPlaylist.getPreviousMedia(
                    mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                    mPlayMode == SHUFFLED));
        }
    }

    public void seekTo(int to) {
        mPlayback.seekTo(to);
    }

    public LocalMedia getCurrentMedia() {
        return mPlaylist == null ? null : mPlaylist.getCurrentMedia();
    }

    public int getPlayState() {
        return mPlayState;
    }

    public PlaybackState getPlaybackState() {
        long actions;
        mPlayState = mPlayback.getState();

        if (mPlayState == PlaybackState.STATE_PLAYING) {
            actions = PlaybackState.ACTION_STOP | PlaybackState.ACTION_PAUSE
                    | PlaybackState.ACTION_REWIND | PlaybackState.ACTION_FAST_FORWARD
                    | PlaybackState.ACTION_SEEK_TO;
        } else {
            actions = PlaybackState.ACTION_STOP | PlaybackState.ACTION_PLAY;
        }

        if (hasNext()) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        if (hasPre()) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_PLAY_MODE, mPlayMode);

        PlaybackState.Builder builder = new PlaybackState.Builder();
        builder.setActions(actions);
        builder.setState(mPlayState, mPlayback.getCurrentPosition(), 1.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            builder.setExtras(extras);
        }
        if (mPlaylist != null) {
            builder.setActiveQueueItemId(mPlaylist.getCurrentIndex());
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
        if (mPlayState == PlaybackState.STATE_PLAYING) {
            playNext();
        } else {
            Log.w(TAG, "mPlayState is not playing");
        }
    }

    public void onPlaybackStateChanged(int state) {
        if (mPlayback != null
                && mPlayState == PlaybackState.STATE_BUFFERING
                && state == PlaybackState.STATE_PLAYING
                && mBeforePosition > 0) {
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
        mPlayState = PlaybackState.STATE_ERROR;
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
        if (mPlayMode == NOT_REPEATING) {
            return mPlaylist != null && mPlaylist.hasNextMedia();
        }
        return true;
    }

    private boolean hasPre() {
        if (mPlayMode == NOT_REPEATING) {
            return mPlaylist != null && mPlaylist.hasPreviousMedia();
        }
        return true;
    }

    private boolean isPlayOrPause() {
        if (getCurrentMedia() != null) {
            return mPlayState == PlaybackState.STATE_PLAYING || mPlayState == PlaybackState.STATE_PAUSED;
        }
        return false;
    }

    private synchronized void playMedia(LocalMedia media) {
        mBeforePosition = 0;
        mPlayback.playMedia(media);
    }
}

