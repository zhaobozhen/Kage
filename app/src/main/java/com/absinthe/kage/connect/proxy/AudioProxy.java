package com.absinthe.kage.connect.proxy;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Device;
import com.absinthe.kage.device.cmd.AudioInfoCommand;
import com.absinthe.kage.device.cmd.InquiryDurationCommand;
import com.absinthe.kage.device.cmd.InquiryPlayStateCommand;
import com.absinthe.kage.device.cmd.InquiryPlayStatusCommand;
import com.absinthe.kage.device.cmd.InquiryPlayingPositionCommand;
import com.absinthe.kage.device.cmd.MediaPausePlayingCommand;
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand;
import com.absinthe.kage.device.cmd.PlayAudioListCommand;
import com.absinthe.kage.device.cmd.PlayNextCommand;
import com.absinthe.kage.device.cmd.PlayPreviousCommand;
import com.absinthe.kage.device.cmd.SeekToCommand;
import com.absinthe.kage.device.cmd.SetAudioModeCommand;
import com.absinthe.kage.device.cmd.SetPlayIndexCommand;
import com.absinthe.kage.device.cmd.StopCommand;
import com.absinthe.kage.device.model.AudioInfo;
import com.google.gson.Gson;

import java.util.List;

public class AudioProxy extends BaseProxy {
    private static final String TAG = AudioProxy.class.getSimpleName();
    private static AudioProxy sInstance;

    private InquiryCurrentPositionThread mInquiryCurrentPositionThread;
    private InquiryPlayStateThread mInquiryPlayStateThread;
    private InquiryDurationThread mInquiryDurationThread;
    private OnPlayListener mOnPlayListener;
    private Device.OnReceiveMsgListener mOnReceiveMsgListener;
    private PlayInfo mCurrentPlayInfo = new PlayInfo();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mPlayPositionInquiryPeriod = 1000;

    private AudioProxy() {
        mOnReceiveMsgListener = this::parserMsgAndNotifyIfNeed;
    }

    public static AudioProxy getInstance() {
        if (sInstance == null) {
            synchronized (AudioProxy.class) {
                if (sInstance == null) {
                    sInstance = new AudioProxy();
                }
            }
        }
        return sInstance;
    }

    public void play(AudioInfo audioInfo) {
        if (mDevice != null && mDevice.isConnected()
                && audioInfo != null && audioInfo.getUrl() != null) {
            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
            cancelInquiryPlayState();
            cancelInquiryCurrentPosition();
            cancelInquiryDuration();
            resetCurrentPlayInfo();
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);

            StopCommand stopCmd = new StopCommand();
            mDevice.sendCommand(stopCmd);
            MediaPreparePlayCommand preparePlayCmd = new MediaPreparePlayCommand();
            preparePlayCmd.type = MediaPreparePlayCommand.TYPE_MUSIC;
            mDevice.sendCommand(preparePlayCmd);

            AudioInfoCommand audioInfoCommand = new AudioInfoCommand();
            audioInfoCommand.url = audioInfo.getUrl();
            audioInfoCommand.name = audioInfo.getName();
            audioInfoCommand.artist = audioInfo.getArtist();
            audioInfoCommand.album = audioInfo.getAlbum();
            audioInfoCommand.coverPath = audioInfo.getCoverPath();
            mDevice.sendCommand(audioInfoCommand);
            mCurrentPlayInfo.isPlayListMode = false;
            scheduleInquiryPlayState(1000);
        }
    }

    public void start() {
        if (mCurrentPlayInfo.currentPlayState == PLAY_STATUS.PAUSED_PLAYBACK.getStatus()
                && null != mDevice && mDevice.isConnected()) {
            AudioInfoCommand audioInfoCommand = new AudioInfoCommand();
            mDevice.sendCommand(audioInfoCommand);
        }

    }

    public void pause() {
        if (mCurrentPlayInfo.currentPlayState == PLAY_STATUS.PLAYING.getStatus()
                && mDevice != null && mDevice.isConnected()) {
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

    public void seekTo(final int position) {
        if (mCurrentPlayInfo.currentPlayState != PLAY_STATUS.STOPPED.getStatus()
                && null != mDevice && mDevice.isConnected()) {
            SeekToCommand seekToCmd = new SeekToCommand();
            seekToCmd.position = position;
            mDevice.sendCommand(seekToCmd);
            mCurrentPlayInfo.position = position;
        }
    }

    public synchronized void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }

    public int getPlayState() {
        int currentPlayState = PlayStatue.INVALIDATE;
        if (mCurrentPlayInfo != null) {
            currentPlayState = mCurrentPlayInfo.currentPlayState;
        }
        return currentPlayState;
    }

    public int getDuration() {
        if (mCurrentPlayInfo != null) {
            return mCurrentPlayInfo.duration;
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mCurrentPlayInfo != null) {
            return mCurrentPlayInfo.position;
        }
        return 0;
    }

    public void recycle() {
        mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
        cancelInquiryCurrentPosition();
        cancelInquiryDuration();
        cancelInquiryPlayState();
    }

    public void playPrevious() {
        if (null != mDevice && mDevice.isConnected()) {
            if (!mCurrentPlayInfo.isPlayListMode) {
                return;
            }

            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
            cancelInquiryPlayState();
            cancelInquiryCurrentPosition();
            cancelInquiryDuration();
            resetCurrentPlayInfo();
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);

            PlayPreviousCommand playPreCmd = new PlayPreviousCommand();
            mDevice.sendCommand(playPreCmd);

            scheduleInquiryPlayState(1000);
        }
    }

    public void playNext() {
        if (null != mDevice && mDevice.isConnected()) {
            if (!mCurrentPlayInfo.isPlayListMode) {
                return;
            }

            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
            cancelInquiryPlayState();
            cancelInquiryCurrentPosition();
            cancelInquiryDuration();
            resetCurrentPlayInfo();
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);

            PlayNextCommand playNextCmd = new PlayNextCommand();
            mDevice.sendCommand(playNextCmd);

            scheduleInquiryPlayState(1000);
        }
    }

    public void playList(int index, List<AudioInfo> list) {
        if (null != mDevice && mDevice.isConnected() && null != list && list.size() > 0) {
            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
            cancelInquiryPlayState();
            cancelInquiryCurrentPosition();
            cancelInquiryDuration();
            resetCurrentPlayInfo();
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);

            StopCommand stopCmd = new StopCommand();
            mDevice.sendCommand(stopCmd);

            MediaPreparePlayCommand preparePlayCmd = new MediaPreparePlayCommand();
            preparePlayCmd.type = MediaPreparePlayCommand.TYPE_MUSIC;
            mDevice.sendCommand(preparePlayCmd);

            PlayAudioListCommand playListCmd = new PlayAudioListCommand();
            playListCmd.index = index;
            playListCmd.size = list.size();
            playListCmd.listInfo = new Gson().toJson(list);
            mDevice.sendCommand(playListCmd);

            mCurrentPlayInfo.isPlayListMode = true;
            scheduleInquiryPlayState(1000);
        }
    }

    public void setPlayAudioMode(int mode) {
        if (null != mDevice && mDevice.isConnected()) {
            SetAudioModeCommand setAudioModeCmd = new SetAudioModeCommand();
            setAudioModeCmd.mode = mode;
            mDevice.sendCommand(setAudioModeCmd);
        }
    }

    public void setPlayIndex(int index) {
        if (null != mDevice && mDevice.isConnected()) {
            if (!mCurrentPlayInfo.isPlayListMode) {
                return;
            }
            if (index < 0) {
                return;
            }
            SetPlayIndexCommand setPlayIndexCommand = new SetPlayIndexCommand();
            setPlayIndexCommand.index = index;
            mDevice.sendCommand(setPlayIndexCommand);
            resetCurrentPlayInfo();
        }
    }

    public int getPlayPositionInquiryPeriod() {
        return mPlayPositionInquiryPeriod;
    }

    /**
     * 设置同步remote播放进度时间间隔，在play之前设置有效
     *
     * @param inquiryPeriod mill seconds
     */
    public void setPlayPositionInquiryPeriod(int inquiryPeriod) {
        this.mPlayPositionInquiryPeriod = inquiryPeriod;
    }

    private void parserMsgAndNotifyIfNeed(String msg) {
        if (msg != null) {
            Log.d(TAG, "msg = " + msg);
            String[] split = msg.split(IpMessageProtocol.DELIMITER);
            if (split.length < 2) {
                return;
            }

            try {
                int cmd = Integer.parseInt(split[0]);
                switch (cmd) {
                    case IpMessageConst.RESPONSE_SET_PLAYBACK_PROCESS:
                        int position = Integer.parseInt(split[1]);
                        if (mCurrentPlayInfo == null || mCurrentPlayInfo.duration <= 0) {
                            break;
                        }
                        mCurrentPlayInfo.position = position;
                        notifyOnCurrentPositionChanged(mCurrentPlayInfo);
                        break;
                    case IpMessageConst.RESPONSE_SET_AUDIO_DURATION:
                        int duration = Integer.parseInt(split[1]);
                        if (duration <= 0 || mCurrentPlayInfo == null) {
                            break;
                        }
                        mCurrentPlayInfo.duration = duration;
                        cancelInquiryDuration();
                        notifyOnCurrentPositionChanged(mCurrentPlayInfo);
                        scheduleInquiryCurrentPosition();//获取到总长度后询问当前播放进度
                        break;
                    case IpMessageConst.MEDIA_SET_PLAYING_STATUS:
                        int playerState = PLAYER_STATUS.valueOf(split[1]).getStatus();
                        int playOldState = mCurrentPlayInfo.currentPlayState;
                        if (PlayStatue.PLAYER_EXIT == playerState) {
                            onPlayStopped();
                        }
                        notifyOnPlayStateChanged(playOldState, playerState);
                        break;
                    case IpMessageConst.MEDIA_SET_PLAYING_STATE:
                        int newState = PLAY_STATUS.valueOf(split[1]).getStatus();
                        int oldState = mCurrentPlayInfo.currentPlayState;
                        if (oldState == newState) {
                            break;
                        }
                        if (PlayStatue.PLAYING == newState) {
                            scheduleInquiryDuration();//定时询问长度，直到获取到合法长度停止询问。
                        } else {
                            cancelInquiryCurrentPosition();
                            cancelInquiryDuration();
                        }
                        if (PlayStatue.STOPPED == newState) {
                            onPlayStopped();
                        }
                        if (mCurrentPlayInfo != null) {
                            mCurrentPlayInfo.currentPlayState = newState;
                        }
                        notifyOnPlayStateChanged(oldState, newState);
                        break;
                    case IpMessageConst.RESPONSE_PLAYING_INDEX:
                        resetCurrentPlayInfo();
                        int index = Integer.parseInt(split[1]);
                        notifyOnPlayIndexChanged(index);
                        break;
                    default:
                }
            } catch (Exception e) {
                Log.e(TAG, "protocol invalid:" + e.getMessage());
            }
        }
    }

    private void onPlayStopped() {
        resetCurrentPlayInfo();
        cancelInquiryPlayState();
    }


    private void notifyOnCurrentPositionChanged(PlayInfo playInfo) {
        mHandler.post(() -> {
            if (mOnPlayListener != null) {
                Log.d(TAG, "notifyOnCurrentPositionChanged: " + "duration = " + playInfo.duration + ", position = " + playInfo.position);
                mOnPlayListener.onCurrentPositionChanged(playInfo.duration, playInfo.duration);
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

    private void notifyOnPlayIndexChanged(final int index) {
        mHandler.post(() -> {
            if (mOnPlayListener != null) {
                mOnPlayListener.onPlayIndexChanged(index);
            }

        });
    }

    private void resetCurrentPlayInfo() {
        mCurrentPlayInfo.duration = 0;
        mCurrentPlayInfo.position = 0;
        mCurrentPlayInfo.currentPlayState = PlayStatue.STOPPED;
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

    private void scheduleInquiryDuration() {
        cancelInquiryDuration();
        mInquiryDurationThread = new InquiryDurationThread(mDevice, mCurrentPlayInfo);
        mInquiryDurationThread.start();
    }

    private void cancelInquiryDuration() {
        if (null != mInquiryDurationThread) {
            mInquiryDurationThread.interrupt();
            mInquiryDurationThread = null;
        }
    }

    private static class InquiryDurationThread extends Thread {
        private InquiryDurationCommand inquiryDurationCmd = new InquiryDurationCommand();
        private Device mDevice;
        private PlayInfo mPlayInfo;

        public InquiryDurationThread(Device device, PlayInfo playInfo) {
            mDevice = device;
            mPlayInfo = playInfo;
        }

        @Override
        public void run() {
            while (true) {
                if (isInterrupted() || mPlayInfo == null) {
                    break;
                }
                if (mDevice == null || !mDevice.isConnected()) {
                    return;
                }
                mDevice.sendCommand(inquiryDurationCmd);

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "InquiryDurationThread is interrupted");
                    break;
                }
            }
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
        mInquiryCurrentPositionThread = new InquiryCurrentPositionThread(
                mDevice, updatePeriod, mPlayPositionInquiryPeriod, mCurrentPlayInfo);
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
        private int mUpdatePeriod;
        private int mInquiryPeriod;
        private int mNoInquiryMills;
        private PlayInfo mPlayInfo;
        private InquiryPlayingPositionCommand positionCommand = new InquiryPlayingPositionCommand();

        InquiryCurrentPositionThread(Device device, int updatePeriod, int inquiryPeriod, PlayInfo playInfo) {
            this.mDevice = device;
            this.mUpdatePeriod = updatePeriod;
            this.mInquiryPeriod = inquiryPeriod;
            this.mPlayInfo = playInfo;
            this.mNoInquiryMills = inquiryPeriod;
        }

        @Override
        public void run() {
            while (true) {
                if (isInterrupted() || mPlayInfo == null) {
                    break;
                }
                if (mDevice == null || !mDevice.isConnected()) {
                    return;
                }
                if (mNoInquiryMills >= mInquiryPeriod) {
                    mDevice.sendCommand(positionCommand);
                    mNoInquiryMills = 0;
                } else {
                    //simulate update
                    if (mPlayInfo.position + mUpdatePeriod >= mPlayInfo.duration) {
                        mPlayInfo.position = mPlayInfo.duration;
                    } else {
                        mPlayInfo.position += mUpdatePeriod;
                    }
                    notifyOnCurrentPositionChanged(mPlayInfo);
                }
                try {
                    sleep(mUpdatePeriod);
                    mNoInquiryMills += mUpdatePeriod;
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

        InquiryPlayStateThread(Device device) {
            this.mDevice = device;
        }

        @Override
        public void run() {
            while (true) {
                if (isInterrupted() || mDevice == null || !mDevice.isConnected()) {
                    break;
                }
                InquiryPlayStateCommand inquiryPlayStateCmd = new InquiryPlayStateCommand();
                mDevice.sendCommand(inquiryPlayStateCmd);
                InquiryPlayStatusCommand inquiryPlayStatusCmd = new InquiryPlayStatusCommand();
                mDevice.sendCommand(inquiryPlayStatusCmd);

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

    private static class PlayInfo {
        int duration;
        int position;
        int currentPlayState;
        boolean isPlayListMode;
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
    public void onDeviceConnected(Device device) {
        if (this.mDevice != device) {
            if (this.mDevice != null) {
                mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
                cancelInquiryPlayState();
                cancelInquiryCurrentPosition();
                cancelInquiryDuration();
            }
            this.mDevice = device;
        }
    }

    @Override
    public void onDeviceDisconnected(Device device) {
        super.onDeviceDisconnected(device);
        int playerState = PlayStatue.DISCONNECT;
        int playOldState = mCurrentPlayInfo.currentPlayState;
        onPlayStopped();
        notifyOnPlayStateChanged(playOldState, playerState);
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

    public interface OnPlayListener {
        void onCurrentPositionChanged(int duration, int position);

        void onPlayStateChanged(int oldState, int newState);

        void onPlayIndexChanged(int index);
    }
}