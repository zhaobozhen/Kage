package com.absinthe.kage.connect.proxy;

import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Device;
import com.absinthe.kage.device.cmd.InquiryDurationCommand;
import com.absinthe.kage.device.cmd.InquiryPlayStateCommand;
import com.absinthe.kage.device.cmd.InquiryPlayerStatusCommand;
import com.absinthe.kage.device.cmd.InquiryPlayingPositionCommand;
import com.absinthe.kage.device.cmd.MediaPausePlayingCommand;
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand;
import com.absinthe.kage.device.cmd.SeekToCommand;
import com.absinthe.kage.device.cmd.StopCommand;
import com.absinthe.kage.device.cmd.VideoInfoCommand;
import com.absinthe.kage.device.model.VideoInfo;

public class VideoProxy extends BaseProxy {

    private static final String TAG = VideoProxy.class.getSimpleName();
    private static VideoProxy sInstance;

    private InquiryCurrentPositionThread mInquiryCurrentPositionThread;
    private InquiryPlayStateThread mInquiryPlayStateThread;
    private OnPlayListener mOnPlayListener;
    private Device.OnReceiveMsgListener mOnReceiveMsgListener;
    private PlayInfo mPlayInfo = new PlayInfo();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mPlayPositionInquiryPeriod = 1000;

    private VideoProxy() {
        mOnReceiveMsgListener = this::parserMsgAndNotifyIfNeed;
    }

    public static VideoProxy getInstance() {
        if (sInstance == null) {
            synchronized (VideoProxy.class) {
                if (sInstance == null) {
                    sInstance = new VideoProxy();
                }
            }
        }
        return sInstance;
    }

    public void play(VideoInfo videoInfo) {
        if (null != videoInfo && null != videoInfo.getUrl()
                && null != mDevice && mDevice.isConnected()) {
            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
            cancelInquiryPlayState();
            cancelInquiryCurrentPosition();
            resetCurrentPlayInfo();
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);
            StopCommand stopCmd = new StopCommand();
            mDevice.sendCommand(stopCmd);
            MediaPreparePlayCommand preparePlayCmd = new MediaPreparePlayCommand();
            preparePlayCmd.type = MediaPreparePlayCommand.TYPE_VIDEO;
            mDevice.sendCommand(preparePlayCmd);

            VideoInfoCommand videoInfoCommand = new VideoInfoCommand();
            videoInfoCommand.title = videoInfo.getTitle();
            videoInfoCommand.url = videoInfo.getUrl();
            mDevice.sendCommand(videoInfoCommand);
            scheduleInquiryPlayState(1000);
        }
    }

    public void start() {
        if (mPlayInfo.currentPlayState == PLAY_STATUS.PAUSED_PLAYBACK.getStatus()
                && null != mDevice && mDevice.isConnected()) {
            VideoInfoCommand videoInfoCommand = new VideoInfoCommand();
            mDevice.sendCommand(videoInfoCommand);
        }

    }

    public void pause() {
        if (mPlayInfo.currentPlayState == PLAY_STATUS.PLAYING.getStatus()
                && null != mDevice && mDevice.isConnected()) {
            MediaPausePlayingCommand pauseCmd = new MediaPausePlayingCommand();
            mDevice.sendCommand(pauseCmd);
        }
    }

    public void stop() {
        if (null != mDevice && mDevice.isConnected()) {
            StopCommand stopCmd = new StopCommand();
            mDevice.sendCommand(stopCmd);
        }
    }

    public void seekTo(int position) {
        if (null != mDevice && mDevice.isConnected()) {
            SeekToCommand seekToCmd = new SeekToCommand();
            seekToCmd.position = position;
            mDevice.sendCommand(seekToCmd);

            mPlayInfo.position = position;
        }
    }

    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }

    public int getPlayState() {
        int currentPlayState = PlayStatue.INVALIDATE;
        if (mPlayInfo != null) {
            currentPlayState = mPlayInfo.currentPlayState;
        }
        return currentPlayState;
    }

    public int getDuration() {
        if (mPlayInfo != null) {
            return mPlayInfo.duration;
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mPlayInfo != null) {
            return mPlayInfo.position;
        }
        return 0;
    }

    public void recycle() {
        mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
        cancelInquiryCurrentPosition();
        cancelInquiryPlayState();
    }

    private void parserMsgAndNotifyIfNeed(String msg) {
        if (msg != null) {
            String[] split = msg.split(IpMessageProtocol.DELIMITER);
            if (split.length < 2) {
                return;
            }

            try {
                int cmd = Integer.parseInt(split[0]);
                switch (cmd) {
                    case IpMessageConst.RESPONSE_SET_PLAYBACK_PROGRESS:
                        int position = Integer.parseInt(split[1]);
                        if (isPlayerWorking()) {
                            mPlayInfo.position = position;
                        } else {
                            break;
                        }
                        notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position);
                        break;
                    case IpMessageConst.RESPONSE_SET_MEDIA_DURATION:
                        int duration = Integer.parseInt(split[1]);
                        if (isPlayerWorking()) {
                            mPlayInfo.duration = duration;
                        } else {
                            break;
                        }
                        notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position);
                        scheduleInquiryCurrentPosition();
                        break;
                    case IpMessageConst.MEDIA_SET_PLAYER_STATUS:
                        int playerState = PLAYER_STATUS.valueOf(split[1]).getStatus();
                        int playOldState = mPlayInfo.currentPlayState;
                        if (PlayStatue.PLAYER_EXIT == playerState) {
                            Log.i(TAG, "Receive Exit");
                            onPlayStopped();
                        }
                        notifyOnPlayStateChanged(playOldState, playerState);
                        break;
                    case IpMessageConst.MEDIA_SET_PLAYING_STATE:
                        String state = split[1];

                        int newState = PLAY_STATUS.valueOf(state).getStatus();
                        int oldState = mPlayInfo.currentPlayState;

                        Log.i(TAG, "newState:" + newState + "-oldState:" + oldState);
                        if (oldState == newState) {
                            break;
                        }
                        if (PlayStatue.PLAYING == newState) {
                            inquiryDuration();
                        } else {
                            cancelInquiryCurrentPosition();
                        }
                        if (PlayStatue.STOPPED == newState) {
                            Log.i(TAG, "Receive STOP");
                            onPlayStopped();
                        }
                        if (mPlayInfo != null) {
                            mPlayInfo.currentPlayState = newState;
                        }
                        notifyOnPlayStateChanged(oldState, newState);
                        break;
                    default:
                }
            } catch (Exception e) {
                Log.e(TAG, "protocol invalid:" + e.getMessage());
            }
        }
    }

    private boolean isPlayerWorking() {
        return mPlayInfo != null
                && (mPlayInfo.currentPlayState == PlayStatue.PLAYING || mPlayInfo.currentPlayState == PlayStatue.PAUSED);
    }

    private void onPlayStopped() {
        resetCurrentPlayInfo();
        cancelInquiryPlayState();
    }


    private void notifyOnCurrentPositionChanged(final int duration, final int position) {
        mHandler.post(() -> {
            if (mOnPlayListener != null) {
                mOnPlayListener.onCurrentPositionChanged(duration, position);
            }
        });

    }

    private void notifyOnPlayStateChanged(final int oldState, final int newState) {
        mHandler.post(() -> {
            if (mOnPlayListener != null) {
                mOnPlayListener.onPlayStateChanged(oldState, newState);
            }
        });

    }

    private void resetCurrentPlayInfo() {
        mPlayInfo.duration = 0;
        mPlayInfo.position = 0;
        mPlayInfo.currentPlayState = PlayStatue.STOPPED;
    }

    private void scheduleInquiryPlayState(int period) {
        cancelInquiryPlayState();
        mInquiryPlayStateThread = new InquiryPlayStateThread(mDevice);
        mInquiryPlayStateThread.setPeriod(period);
        mInquiryPlayStateThread.start();
    }

    private void cancelInquiryPlayState() {
        if (mInquiryPlayStateThread != null) {
            mInquiryPlayStateThread.interrupt();
            mInquiryPlayStateThread = null;
        }
    }

    private void inquiryDuration() {
        InquiryDurationCommand inquiryDurationCmd = new InquiryDurationCommand();
        mDevice.sendCommand(inquiryDurationCmd);
    }

    private void scheduleInquiryCurrentPosition() {
        cancelInquiryCurrentPosition();
        int updatePeriod = 1000;
        if (mPlayPositionInquiryPeriod < updatePeriod) {
            mPlayPositionInquiryPeriod = updatePeriod;
        }
        mInquiryCurrentPositionThread = new InquiryCurrentPositionThread(mDevice, updatePeriod, mPlayPositionInquiryPeriod, mPlayInfo);
        mInquiryCurrentPositionThread.start();
    }

    private void cancelInquiryCurrentPosition() {
        if (mInquiryCurrentPositionThread != null) {
            mInquiryCurrentPositionThread.interrupt();
            mInquiryCurrentPositionThread = null;
        }
    }

    private class InquiryCurrentPositionThread extends Thread {

        private Device mDevice;
        private int mUpdatePeriod = 2000;
        private int mInquiryPeriod = 4000;
        private int mNoInquiryMills = 0;
        private PlayInfo mPlayInfo;
        private InquiryPlayingPositionCommand inquiryCurrentPositionCmd = new InquiryPlayingPositionCommand();

        public InquiryCurrentPositionThread(Device device, int updatePeriod, int inquiryPeriod, PlayInfo playInfo) {
            mDevice = device;
            mUpdatePeriod = updatePeriod;
            mInquiryPeriod = inquiryPeriod;
            mPlayInfo = playInfo;
            mNoInquiryMills = inquiryPeriod;
        }

        @Override
        public void run() {
            while (true) {
                if (isInterrupted()) {
                    break;
                }
                if (mDevice == null || !mDevice.isConnected()) {
                    break;
                }
                if (mPlayInfo == null) {
                    break;
                }
                int updatePeriodMillis = mUpdatePeriod;
                int inquiryPeriodMillis = mInquiryPeriod;
                if (mNoInquiryMills >= inquiryPeriodMillis) {
                    //do inquiry remote
                    mDevice.sendCommand(inquiryCurrentPositionCmd);
                    mNoInquiryMills = 0;
                } else {
                    //simulate update
                    if (mPlayInfo.position + updatePeriodMillis >= mPlayInfo.duration) {
                        mPlayInfo.position = mPlayInfo.duration;
                    } else {
                        mPlayInfo.position += updatePeriodMillis;
                    }
                    notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position);
                }
                mNoInquiryMills += updatePeriodMillis;
                try {
                    sleep(updatePeriodMillis);
                } catch (InterruptedException e) {
                    Log.e(TAG, "InquiryCurrentPositionThread is interrupted");
                    break;
                }
            }
        }
    }

    private static class InquiryPlayStateThread extends Thread {

        private Device mDevice;
        private int period = 2000;

        public InquiryPlayStateThread(Device device) {
            mDevice = device;
        }

        @Override
        public void run() {
            while (true) {
                if (isInterrupted()) {
                    break;
                }
                if (mDevice == null || !mDevice.isConnected()) {
                    break;
                }
                InquiryPlayStateCommand inquiryPlayStateCmd = new InquiryPlayStateCommand();
                mDevice.sendCommand(inquiryPlayStateCmd);
                InquiryPlayerStatusCommand inquiryPlayerStatusCmd = new InquiryPlayerStatusCommand();
                mDevice.sendCommand(inquiryPlayerStatusCmd);
                long inquiryPeriod = period;
                try {
                    sleep(inquiryPeriod);
                } catch (InterruptedException e) {
                    Log.e(TAG, "InquiryCurrentPositionThread is interrupted");
                    break;
                }
            }
        }

        public void setPeriod(int period) {
            this.period = period;
        }
    }

    public interface OnPlayListener {

        void onCurrentPositionChanged(int duration, int position);

        void onPlayStateChanged(int oldState, int newState);

    }

    static class PlayInfo {
        int duration;
        int position;
        int currentPlayState;
    }

    public interface PlayStatue {
        int INVALIDATE = -1;
        int STOPPED = 1;
        int TRANSITIONING = 2;
        int PLAYING = 3;
        int PAUSED = 4;

        int OK = 10;
        int PLAYER_EXIT = 11;
        int ERROR_OCCURRED = 12;

        int DISCONNECT = 20;
    }

    //  播放器状态
    private enum PLAYER_STATUS {
        OK(PlayStatue.OK),
        PLAYER_EXIT(PlayStatue.PLAYER_EXIT),
        ERROR_OCCURRED(PlayStatue.ERROR_OCCURRED);

        private int status;

        PLAYER_STATUS(int value) {
            this.status = value;
        }

        private int getStatus() {
            return status;
        }
    }

    //播放状态
    private enum PLAY_STATUS {
        STOPPED(PlayStatue.STOPPED),
        PLAYING(PlayStatue.PLAYING),
        PAUSED_PLAYBACK(PlayStatue.PAUSED),
        TRANSITIONING(PlayStatue.TRANSITIONING);

        private int status;

        PLAY_STATUS(int value) {
            this.status = value;
        }

        private int getStatus() {
            return status;
        }
    }


    @Override
    public void onDeviceConnected(@NonNull Device device) {
        if (mDevice != device) {
            if (mDevice != null) {
                mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
                cancelInquiryPlayState();
                cancelInquiryCurrentPosition();
            }
            mDevice = device;
        }
    }

    @Override
    public void onDeviceDisconnected(@NonNull Device device) {
        super.onDeviceDisconnected(device);

        int playerState = PlayStatue.DISCONNECT;
        int playOldState = mPlayInfo.currentPlayState;
        onPlayStopped();
        notifyOnPlayStateChanged(playOldState, playerState);
    }
}