package com.absinthe.kage.connect.proxy;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Device;
import com.absinthe.kage.device.cmd.ImageInfoCommand;
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand;
import com.absinthe.kage.device.cmd.StopCommand;

public class ImageProxy extends BaseProxy {
    private static final String TAG = ImageProxy.class.getSimpleName();
    private static final byte[] lock = new byte[0];
    private static ImageProxy sInstance;

    private Device.OnReceiveMsgListener mOnReceiveMsgListener;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private OnPlayListener mOnPlayListener;
    private PlayInfo mPlayInfo = new PlayInfo();
    private InquiryPlayStateThread mInquiryPlayStateThread;

    private ImageProxy() {
        mOnReceiveMsgListener = this::parserMsgAndNotifyIfNeed;
    }

    public static ImageProxy getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new ImageProxy();
                }
            }
        }
        return sInstance;
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
                    case IpMessageConst.MEDIA_SET_PLAYING_STATUS:
                        final int playerState = PlayerStatus.valueOf(split[1]).getStatus();
                        final int playOldState = mPlayInfo.playState;
                        if (playerState == playOldState) {
                            break;
                        }
                        if (PlayStatue.PLAYER_EXIT == playerState) {
                            onPlayExit();
                        }
                        mPlayInfo.playState = playerState;
                        if (mOnPlayListener != null) {
                            mHandler.post(() -> {
                                if (mOnPlayListener != null) {
                                    mOnPlayListener.onPlayStateChanged(playOldState, mPlayInfo.playState);
                                }
                            });
                        }
                        break;
                    case IpMessageConst.MEDIA_SET_PLAYING_STATE:
                        final int newState = PLAY_STATUS.valueOf(split[1]).getStatus();
                        final int oldState = mPlayInfo.playState;
                        if (oldState == newState) {
                            break;
                        }

                        if (PlayStatue.STOPPED == newState) {
                            onPlayExit();
                            mPlayInfo.playState = PlayStatue.PLAYER_EXIT;
                        } else {
                            mPlayInfo.playState = newState;
                        }

                        if (mOnPlayListener != null) {
                            mHandler.post(() -> {
                                if (mOnPlayListener != null) {
                                    mOnPlayListener.onPlayStateChanged(oldState, mPlayInfo.playState);
                                }
                            });
                        }
                        mPlayInfo.playState = newState;
                        break;
                    case IpMessageConst.MEDIA_PLAY_PREVIOUS:
                        if (mOnPlayListener != null) {
                            mHandler.post(() -> {
                                if (mOnPlayListener != null) {
                                    mOnPlayListener.onRemotePreview();
                                }
                            });
                        }
                        break;
                    case IpMessageConst.MEDIA_PLAY_NEXT:
                        if (mOnPlayListener != null) {
                            mHandler.post(() -> {
                                if (mOnPlayListener != null) {
                                    mOnPlayListener.onRemoteNext();
                                }
                            });
                        }
                        break;
                    default:
                }
            } catch (Exception e) {
                Log.e(TAG, "Protocol invalid: " + e.getMessage());
            }
        }
    }

    private void onPlayExit() {

    }

    public void close() {
        if (mDevice != null && mOnReceiveMsgListener != null) {
            mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
        }
    }

    public void cast(String url) {
        cast(url, false);
    }

    public void cast(String url, boolean needStop) {
        if (mDevice != null && mDevice.isConnected()) {
            mPlayInfo.playState = PlayStatue.TRANSITIONING;
            mDevice.registerOnReceiveMsgListener(mOnReceiveMsgListener);
            if (needStop) {
                StopCommand stopCmd = new StopCommand();
                mDevice.sendCommand(stopCmd);
            }

            MediaPreparePlayCommand preparePlayCommand = new MediaPreparePlayCommand();
            preparePlayCommand.type = MediaPreparePlayCommand.TYPE_IMAGE;
            mDevice.sendCommand(preparePlayCommand);

            ImageInfoCommand imageInfoCommand = new ImageInfoCommand();
            imageInfoCommand.info = url;
            mDevice.sendCommand(imageInfoCommand);

            scheduleInquiryPlayState(1000);
        }
    }

    public void stop() {
        if (null != mDevice && mDevice.isConnected()) {
            StopCommand stopCmd = new StopCommand();
            mDevice.sendCommand(stopCmd);
        }
    }

    public void setOnPlayListener(OnPlayListener onPlayListener) {
        mOnPlayListener = onPlayListener;
    }

    public interface OnPlayListener {

        void onPlayStateChanged(int playOldState, int playerState);

        void onRemoteNext();

        void onRemotePreview();
    }

    private static class PlayInfo {
        int direction;
        float scale;
        float[] centerPoint = new float[2];
        int playState;
    }

    public interface PlayStatue {
        int INVALIDATE = -1;
        int STOPPED = 1;
        int TRANSITIONING = 2;
        int PLAYING = 3;

        int OK = 10;
        int PLAYER_EXIT = 11;
        int ERROR_OCCURRED = 12;

        int DISCONNECT = 20;
    }

    //  播放器状态
    private enum PlayerStatus {
        OK(PlayStatue.OK),
        PLAYER_EXIT(PlayStatue.PLAYER_EXIT),
        ERROR_OCCURRED(PlayStatue.ERROR_OCCURRED);

        private int status;

        PlayerStatus(int value) {
            this.status = value;
        }

        private int getStatus() {
            return status;
        }
    }

    private enum PLAY_STATUS {
        STOPPED(PlayStatue.STOPPED),
        PLAYING(PlayStatue.PLAYING),
        TRANSITIONING(PlayStatue.TRANSITIONING);

        private int status;

        PLAY_STATUS(int value) {
            this.status = value;
        }

        private int getStatus() {
            return status;
        }
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

    private static class InquiryPlayStateThread extends Thread {

        private Device mDevice;
        private int period = 2000;

        InquiryPlayStateThread(Device device) {
            this.mDevice = device;
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
//                InquiryPlayStateCommand inquiryPlayStateCmd = new InquiryPlayStateCommand();
//                mDevice.sendCommand(inquiryPlayStateCmd);
//                InquiryPlayStatusCommand inquiryPlayStatusCmd = new InquiryPlayStatusCommand();
//                mDevice.sendCommand(inquiryPlayStatusCmd);
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

    @Override
    public void onDeviceConnected(Device device) {
        if (mDevice != device) {
            if (mDevice != null) {
                mDevice.unregisterOnReceiveMsgListener(mOnReceiveMsgListener);
                cancelInquiryPlayState();
            }
            mDevice = device;
        }
    }

    @Override
    public void onDeviceDisconnected(Device device) {
        super.onDeviceDisconnected(device);
        int playerState = PlayStatue.DISCONNECT;
        int playOldState = mPlayInfo.playState;
        onPlayExit();
        if (mOnPlayListener != null) {
            mOnPlayListener.onPlayStateChanged(playOldState, playerState);
        }
        mPlayInfo.playState = playerState;
    }
}

