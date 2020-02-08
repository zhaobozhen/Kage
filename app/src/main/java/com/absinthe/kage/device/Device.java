package com.absinthe.kage.device;

import android.text.TextUtils;
import android.util.Log;

import com.absinthe.kage.connect.tcp.KageSocket;
import com.absinthe.kage.connect.tcp.Packet;
import com.absinthe.kage.device.heartbeat.HeartbeatSender;
import com.absinthe.kage.device.model.DeviceConfig;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.protocol.Config;
import com.absinthe.kage.protocol.IProtocolHandler;
import com.absinthe.kage.protocol.ProtocolHandler;

import java.util.ArrayList;
import java.util.List;

public class Device {
    private static final String TAG = Device.class.getSimpleName();
    private static final int HEARTBEAT_DEFAULT_TIMEOUT = 20 * 1000;

    private DeviceInfo mDeviceInfo;
    private IConnectCallback mConnectCallback;
    private List<OnReceiveMsgListener> mOnReceiveMsgListeners = new ArrayList<>();
    private DeviceConfig mConfig;
    private IProtocolHandler mProtocolHandler;
    private HeartbeatSender mHeartbeatSender;
    private KageSocket mSocket;

    private long onlineTime;

    public Device(DeviceConfig config, String protocolVersionString) {
        this.mConfig = config;
        this.mDeviceInfo = new DeviceInfo();
        this.mDeviceInfo.setProtocolVersion(protocolVersionString);
        IProtocolHandler.IProtocolHandleCallback mProtocolHandlerCallback = new IProtocolHandler.IProtocolHandleCallback() {
            @Override
            public void onProtocolConnected() {
                if (null != mDeviceInfo) {
                    mDeviceInfo.setConnected(true);
                }
                if (null != mConnectCallback) {
                    mConnectCallback.onConnected();
                }
            }

            @Override
            public void onProtocolDisConnect() {
                if (null != mDeviceInfo) {
                    mDeviceInfo.setConnected(false);
                }
                if (null != mConnectCallback) {
                    mConnectCallback.onDisConnect();
                }
            }

            @Override
            public void onProtocolConnectedFailed(int errorCode, Exception e) {
                if (null != mDeviceInfo) {
                    mDeviceInfo.setConnected(false);
                }
                if (null != mConnectCallback) {
                    mConnectCallback.onConnectedFailed(errorCode, e);
                }
            }

            @Override
            public void onProtocolSendOrReceiveError() {
                mSocket.disConnect();
            }
        };
        int protocolVersion = 0;
        if (!TextUtils.isEmpty(protocolVersionString)) {
            try {
                protocolVersion = Integer.parseInt(protocolVersionString);
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }

        mProtocolHandler = new ProtocolHandler(this, this.mConfig, mProtocolHandlerCallback);
        this.mSocket = new KageSocket();
        this.mSocket.setSocketCallback(new KageSocket.ISocketCallback() {
            @Override
            public void onConnected() {
                if (mProtocolHandler != null) {
                    mProtocolHandler.handleSocketConnectedEvent();
                }
            }

            @Override
            public void onDisConnected() {
                if (mProtocolHandler != null) {
                    mProtocolHandler.handleSocketDisConnectEvent();
                }
            }

            @Override
            public void onReceiveMsg(String msg) {
                if (msg != null) {
                    if (mProtocolHandler != null) {
                        mProtocolHandler.handleSocketMassage(msg);
                    }
                    if (!mDeviceInfo.isConnected()) {
                        return;
                    }
                    synchronized (Device.this) {
                        int size = mOnReceiveMsgListeners.size();
                        for (int i = 0; i < size; i++) {
                            mOnReceiveMsgListeners.get(i).onReceiveMsg(msg);
                        }
                    }
                }
            }

            @Override
            public void onConnectError(int errorCode, Exception e) {
                if (mProtocolHandler != null) {
                    mProtocolHandler.handleSocketConnectFail(errorCode, e);
                }
            }

            @Override
            public void onReadAndWriteError(int errorCode) {
                if (mProtocolHandler != null) {
                    mProtocolHandler.handleSocketSendOrReceiveError();
                }
            }

            @Override
            public void onWriterIdle() {
                //heartbeat
                heartbeat();
            }

            @Override
            public void onReaderIdle() {

            }
        });
        mHeartbeatSender = new HeartbeatSender(mSocket);
    }

    private void heartbeat() {
        String heartbeatId = System.currentTimeMillis() + "";
        mHeartbeatSender.beat(heartbeatId, HEARTBEAT_DEFAULT_TIMEOUT, new HeartbeatSender.IHeartbeatCallback() {
            @Override
            public void onBeatSuccess(String heartbeatId) {
                Log.e(TAG, "onBeatSuccess,heartbeatId = " + heartbeatId);
            }

            @Override
            public void onBeatTimeout(String heartbeatId) {
                Log.e(TAG, "onBeatTimeout,heartbeatId = " + heartbeatId);
                disConnect();
            }

            @Override
            public void onBeatCancel(String heartbeatId) {
                Log.e(TAG, "onBeatCancel,heartbeatId = " + heartbeatId);
            }
        });
    }

    public boolean connect(int timeout) {
        boolean isConnect = mDeviceInfo.isConnected();
        boolean isInit = mConfig != null && mProtocolHandler != null;
        boolean b = !isConnect && isInit;

        if (b) {
            mHeartbeatSender.init();
            mDeviceInfo.setStateConnecting();
            final String ip = mDeviceInfo.getIp();
            int port = Config.PORT;
            mSocket.connect(ip, port, timeout);
            return true;
        } else {
            return false;
        }
    }

    public void disConnect() {
        if (null != mHeartbeatSender) {
            mHeartbeatSender.release();
        }
        if (null != mSocket) {
            mSocket.disConnect();
        }
    }

    private void sendMsg(String data) {
        Packet packet = new Packet();
        packet.setData(data);
        if (mSocket != null) {
            mSocket.send(packet);
        }
    }

    public void sendCommand(Command cmd) {
        String data = cmd.pack();
        if (data != null) {
            sendMsg(data);
        }
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public boolean isConnected() {
        return mDeviceInfo.isConnected();
    }

    public int getState() {
        return mDeviceInfo.getState();
    }

    public long getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(long onlineTime) {
        this.onlineTime = onlineTime;
    }

    public void setIp(String ip) {
        mDeviceInfo.setIp(ip);
    }

    public String getIp() {
        return mDeviceInfo.getIp();
    }

    public void setName(String name) {
        mDeviceInfo.setName(name);
    }

    public String getName() {
        return mDeviceInfo.getName();
    }

    public String getProtocolVersion() {
        return mDeviceInfo.getProtocolVersion();
    }

    public String getFunctionCode() {
        return mDeviceInfo.getFunctionCode();
    }

    public void setFunctionCode(String functionCode) {
        mDeviceInfo.setFunctionCode(functionCode);
    }

    public void setConnectCallback(IConnectCallback connectCallback) {
        this.mConnectCallback = connectCallback;
    }

    public abstract static class Command {
        protected Command() { }

        protected int cmd;

        public abstract String pack();
    }

    public interface IConnectCallback {
        void onConnectedFailed(int errorCode, Exception e);

        void onConnected();

        void onDisConnect();
    }

    public interface OnReceiveMsgListener {
        void onReceiveMsg(String msg);
    }
}
