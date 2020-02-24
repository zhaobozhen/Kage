package com.absinthe.kage.media;

public interface Playback {

    interface Callback {
        void onCompletion();

        void onError(String str);

        void onMediaMetadataChanged(LocalMedia tCastLocalMedia);

        void onPlaybackStateChanged(int i);
    }

    int getBufferPosition();

    int getCurrentPosition();

    int getDuration();

    int getState();

    void pause();

    void play();

    void playMedia(LocalMedia tCastLocalMedia);

    void seekTo(int position);

    void setCallback(Callback callback);

    void stop(boolean isStop);
}
