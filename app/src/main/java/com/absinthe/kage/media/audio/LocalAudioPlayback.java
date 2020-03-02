package com.absinthe.kage.media.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Build;
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
    private int mPlayState = PlaybackState.STATE_NONE;
    private boolean mPlayOnFocusGain;

    private AudioManager mAudioManager;
    private Callback mCallback;
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private AudioManager.OnAudioFocusChangeListener mFocusChangeListener;

    LocalAudioPlayback(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        createAudioFocusChangeListener();
    }

    @Override
    public void playMedia(LocalMedia media) {
        try {
            createMediaPlayerIfNeeded();
            if (mMediaPlayer == null || media == null || TextUtils.isEmpty(media.getFilePath())) {
                mPlayState = PlaybackState.STATE_ERROR;
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

            mPlayState = PlaybackState.STATE_BUFFERING;

            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes mAudioAttributes =
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();
                mMediaPlayer.setAudioAttributes(mAudioAttributes);
            } else {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mMediaPlayer.setDataSource(media.getFilePath());
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            mPlayState = PlaybackState.STATE_ERROR;
            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    @Override
    public void play() {
        if (mPlayOnFocusGain) {
            tryToGetAudioFocus();
        }

        mPlayState = PlaybackState.STATE_PLAYING;
        handlePlayState();
    }

    @Override
    public void pause() {
        mPlayOnFocusGain = true;
        mPlayState = PlaybackState.STATE_PAUSED;
        handlePlayState();
    }

    @Override
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

    @Override
    public int getState() {
        return mPlayState;
    }

    @Override
    public void stop(boolean fromUser) {
        Log.d(TAG, "stop");
        mPlayState = PlaybackState.STATE_STOPPED;
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
        abandonAudioFocus();
    }

    @Override
    public int getDuration() {
        if (mMediaPlayer == null || !isPlayOrPause()) {
            return 0;
        }
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getBufferPosition() {
        if (isPlayOrPause()) {
            return getDuration();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mMediaPlayer == null || !isPlayOrPause()) {
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    private void handlePlayState() {
        if (mMediaPlayer != null) {
            if (mPlayState == PlaybackState.STATE_PLAYING && !mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            } else if (mPlayState == PlaybackState.STATE_PAUSED && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }

            if (mCallback != null) {
                mCallback.onPlaybackStateChanged(mPlayState);
            }
        }
    }

    private boolean isPlayOrPause() {
        return mPlayState == PlaybackState.STATE_PLAYING || mPlayState == PlaybackState.STATE_PAUSED;
    }

    private void createAudioFocusChangeListener() {
        mFocusChangeListener = focusChange -> {
            Log.d(TAG, "onAudioFocusChange: " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                mAudioFocus = AUDIO_FOCUSED;
                if (mPlayState == PlaybackState.STATE_PAUSED) {
                    play();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mAudioFocus = AUDIO_NO_FOCUS_CAN_DUCK;
                }
                if (mPlayState == PlaybackState.STATE_PLAYING) {
                    pause();
                }
            } else {
                Log.d(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
            }
        };
    }

    private void tryToGetAudioFocus() {
        Log.d(TAG, "Try to get AudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes mAudioAttributes =
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();
                AudioFocusRequest mAudioFocusRequest =
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                .setAudioAttributes(mAudioAttributes)
                                .setAcceptsDelayedFocusGain(true)
                                .setOnAudioFocusChangeListener(mFocusChangeListener)
                                .build();
                result = mAudioManager.requestAudioFocus(mAudioFocusRequest);
            } else {
                result = mAudioManager.requestAudioFocus(mFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
                Log.d(TAG, "Try to get AudioFocus success");
            }
        }
    }

    private void abandonAudioFocus() {
        Log.d(TAG, "abandonAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            int result;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes mAudioAttributes =
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();
                AudioFocusRequest mAudioFocusRequest =
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                .setAudioAttributes(mAudioAttributes)
                                .setAcceptsDelayedFocusGain(true)
                                .setOnAudioFocusChangeListener(mFocusChangeListener)
                                .build();
                result = mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
            } else {
                result = mAudioManager.abandonAudioFocus(mFocusChangeListener);
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
                Log.d(TAG, "abandonAudioFocus success");
            }
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
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.d(TAG, "SetOnErrorListener, what: " + what + ", extra: " + extra);
                mPlayState = PlaybackState.STATE_ERROR;
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
