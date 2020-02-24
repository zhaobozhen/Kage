package com.absinthe.kage.media.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.media.Playback;

public class LocalAudioPlayback implements Playback {
    private static final String TAG = LocalAudioPlayback.class.getSimpleName();

    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private int mAudioSession;
    private boolean mPlayOnFocusGain;
    private int mPlayState = 0;
    private AudioManager mAudioManager;
    private Callback mCallback;
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private AudioManager.OnAudioFocusChangeListener mFocusChangeListener;

    public LocalAudioPlayback(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        createAudioFocusChangeListener();
    }

    public void playMedia(LocalMedia media) {
        try {
            createMediaPlayerIfNeeded();
            if (mMediaPlayer == null || media == null || TextUtils.isEmpty(media.getFilePath())) {
                mPlayState = 7;
                if (mCallback != null) {
                    mCallback.onPlaybackStateChanged(mPlayState);
                }
                return;
            }

            if (mCallback != null) {
                mCallback.onMediaMetadataChanged(media);
            }

            if (mAudioSession != 0) {
                mMediaPlayer.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = mMediaPlayer.getAudioSessionId();
            }
            Log.d(TAG, "setDataSource: " + media.getFilePath());

            mPlayState = 6;
            Log.d(TAG, "PlayState: STATE_BUFFERING");

            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }

            mMediaPlayer.setAudioStreamType(3);
            mMediaPlayer.setDataSource(media.getFilePath());
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            mPlayState = 7;
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    public void play() {
        Log.d(TAG, "play");
        tryToGetAudioFocus();
        mPlayState = 3;
        handlePlayState();
    }

    public void pause() {
        mPlayState = 2;
        handlePlayState();
    }

    public void seekTo(int position) {
        Log.d(TAG, "SeekTo: " + position);
        if (position < 0) {
            position = 0;
        }
        if (mMediaPlayer != null && isPlayOrPause()) {
            int bufferPos = getDuration();
            if (position > bufferPos) {
                position = bufferPos;
            }
            mMediaPlayer.seekTo(position);
        }
    }

    public int getState() {
        return mPlayState;
    }

    public void stop(boolean fromUser) {
        Log.d(TAG, "stop");
        mPlayState = 1;
        if (fromUser) {
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        giveUpAudioFocus();
    }

    public int getDuration() {
        if (mMediaPlayer == null || !isPlayOrPause()) {
            return 0;
        }
        return mMediaPlayer.getDuration();
    }

    public int getBufferPosition() {
        if (isPlayOrPause()) {
            return getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mMediaPlayer == null || !isPlayOrPause()) {
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    private void play(boolean onAudioFocusGain) {
        if (!onAudioFocusGain || mPlayOnFocusGain) {
            play();
        }
    }

    private void pause(boolean onAudioFocusLost) {
        mPlayOnFocusGain = onAudioFocusLost;
        pause();
    }

    private void handlePlayState() {
        Log.d(TAG, "handlePlayState");
        if (mMediaPlayer != null) {
            if (mPlayState == 3 && !mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            } else if (mPlayState == 2 && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }

            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    private boolean isPlayOrPause() {
        int i = mPlayState;
        return i == 3 || i == 2;
    }

    private void createAudioFocusChangeListener() {
        mFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, "onAudioFocusChange: " + focusChange);
                if (focusChange == 1) {
                    mAudioFocus = AUDIO_FOCUSED;
                    if (mPlayState == 2) {
                        play(true);
                    }
                } else if (focusChange == -1 || focusChange == -2 || focusChange == -3) {
                    if (focusChange == -3) {
                        mAudioFocus = AUDIO_NO_FOCUS_CAN_DUCK;
                    }
                    if (mPlayState == 3) {
                        pause(true);
                    }
                } else {
                    Log.d(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
                }
            }
        };
    }

    private void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED && mAudioManager.requestAudioFocus(mFocusChangeListener, 3, 1) == 1) {
            mAudioFocus = AUDIO_FOCUSED;
            Log.d(TAG, "tryToGetAudioFocus success");
        }
    }

    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED && mAudioManager.abandonAudioFocus(mFocusChangeListener) == 1) {
            mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            Log.d(TAG, "giveUpAudioFocus success");
        }
    }

    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            setMediaPlayerListener();
            mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        } else {
            mMediaPlayer.reset();
        }
    }

    private void setMediaPlayerListener() {
        MediaPlayer mediaPlayer = mMediaPlayer;
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.d(TAG, "SetOnErrorListener, what: " + what + ", extra: " + extra);
                mPlayState = 7;
                return false;
            });
            mMediaPlayer.setOnSeekCompleteListener(mp -> {
            });
            mMediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
            });
            mMediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "MediaPlayer#onCompletion");
                if (mCallback != null) {
                    mCallback.onCompletion();
                }
            });
            mMediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "SetOnPreparedListener");
                play();
            });
            mMediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "SetOnInfoListener, what: " + what + ", extra: " + extra);
                return false;
            });
        }
    }
}
