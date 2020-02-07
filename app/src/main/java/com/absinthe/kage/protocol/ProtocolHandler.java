package com.absinthe.kage.protocol;

import android.util.Log;

import com.absinthe.kage.connect.tcp.KageSocket;
import com.absinthe.kage.device.Device;
import com.absinthe.kage.device.cmd.InquiryDeviceInfoCommand;
import com.absinthe.kage.device.model.DeviceConfig;

public class ProtocolHandler implements IProtocolHandler {
    private static final String TAG = ProtocolHandler.class.getSimpleName();
    private Device mDevice;
    private DeviceConfig mConfig;
    private IProtocolHandleCallback mCallback;
    private boolean hasHandShake = false;

    public ProtocolHandler(Device device, DeviceConfig config, IProtocolHandleCallback callback) {
        this.mDevice = device;
        this.mConfig = config;
        this.mCallback = callback;
    }

    @Override
    public void handleSocketConnectedEvent() {
        InquiryDeviceInfoCommand inquiryDeviceInfoCommand = new InquiryDeviceInfoCommand();
        inquiryDeviceInfoCommand.phoneName = mConfig.name;
        mDevice.sendCommand(inquiryDeviceInfoCommand);
    }

    @Override
    public void handleSocketMassage(String msg) {
        if (msg != null) {
            String[] split = msg.split(IpMessageProtocol.DELIMITER);
            try {
                int cmd = Integer.parseInt(split[0]);
                switch (cmd) {
                    case IpMessageConst.GET_CLIENTTYPE:
                        String clientType = split[1];
                        String appVersionName = "";
                        String appVersionCode = "";
                        String softwareVersion = "";
                        String tvDeviceNum = "";
                        String macAddress = "";
                        String secret = "";
                        String bluetoothMac = "";
                        String TVType = "";
                        String TVStore = "";
                        String functionCode = "";
                        String tvLanguage = "";
                        String snCode = "";
                        String clientCode = "";
                        String countryCode = "";
                        if (split.length > 2) {
                            String appVersion = split[2];
                            String[] appVersions = appVersion.split(":");
                            if (appVersions.length == 2) {
                                appVersionCode = appVersions[0];
                                appVersionName = appVersions[1];
                            }
                            if (split.length > 3) {
                                softwareVersion = split[3];
                            }
                            if (split.length > 4) {
                                tvDeviceNum = split[4];
                            }
                            if (split.length > 5) {
                                macAddress = split[5];
                            }
                            if (split.length > 6) {
                                secret = split[6];
                            }
                            if (split.length > 7) {
                                bluetoothMac = split[7].toUpperCase();
                            }
                            if (split.length > 8) {
                                TVType = split[8].toUpperCase();
                            }
                            if (split.length > 9) {
                                TVStore = split[9].toUpperCase();
                            }
                            if (split.length > 10) {
                                functionCode = split[10].toUpperCase();
                            }
                            if (split.length > 11) {
                                tvLanguage = split[11];
                            }
                            if (split.length > 12) {
                                snCode = split[12];
                            }
                            if (split.length > 13) {
                                clientCode = split[13];
                            }
                            if (split.length > 14) {
                                countryCode = split[14];
                            }
                        }
                        hasHandShake = true;
                        KageProtocolThreadHandler.getInstance().post(() -> {
                            if (null != mCallback) {
                                mCallback.onProtocolConnected();
                            }
                        });
                        break;
                    default:
                }
            } catch (Exception e) {
                Log.e(TAG, "protocol invalid:" + e.getMessage());
            }
        }
    }

    @Override
    public void handleSocketDisConnectEvent() {
        if (!hasHandShake) {// 握手期间断开了连接判定为连接失败
            KageProtocolThreadHandler.getInstance().post(() -> {
                if (null != mCallback) {
                    mCallback.onProtocolConnectedFailed(KageSocket.ISocketCallback.CONNECT_ERROR_CODE_HAND_SHAKE_UNCOMPLETE, null);
                }
            });
            return;
        }
        hasHandShake = false;
        KageProtocolThreadHandler.getInstance().post(() -> {
            if (null != mCallback) {
                mCallback.onProtocolDisConnect();
            }
        });
    }

    @Override
    public void handleSocketConnectFail(final int errorCode, final Exception e) {
        KageProtocolThreadHandler.getInstance().post(() -> {
            if (null != mCallback) {
                mCallback.onProtocolConnectedFailed(errorCode, e);
            }
        });
    }

    @Override
    public void handleSocketSendOrReceiveError() {
        KageProtocolThreadHandler.getInstance().post(() -> {
            if (null != mCallback) {
                mCallback.onProtocolSendOrReceiveError();
            }
        });
    }
}

