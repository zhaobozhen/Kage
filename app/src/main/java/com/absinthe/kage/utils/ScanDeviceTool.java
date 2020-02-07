package com.absinthe.kage.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ScanDeviceTool
 * 局域网扫描设备工具类
 */
public class ScanDeviceTool implements LifecycleObserver {

    private static final String TAG = ScanDeviceTool.class.getSimpleName();

    /**
     * 核心池大小
     **/
    private static final int CORE_POOL_SIZE = 1;
    /**
     * 线程池最大线程数
     **/
    private static final int MAX_IMUM_POOL_SIZE = 255;

    //匹配C类地址的IP
    private static final String REGEX_C_IP =
            "^192\\.168\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";
    //匹配A类地址
    private static final String REGEX_A_IP =
            "^10\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";
    //匹配B类地址
    private static final String REGEX_B_IP =
            "^172\\.(1[6-9]|2\\d|3[0-1])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";

    private static final String DEFAULT_IP = "192.168.1.100";

    private String mDevAddress;// 本机 IP 地址-完整
    private String mLocAddress;// 局域网 IP 地址头,如：192.168.1.
    private Runtime mRun = Runtime.getRuntime();// 获取当前运行环境来执行 ping
    private Process mProcess = null;// 进程
    private String mPing = "ping -c 1 -W 3 ";// 其中 -c 1 为发送的次数，-w 表示发送后等待响应的时间
    private List<String> mIpList = new ArrayList<>();// ping 成功的 IP 地址
    private ThreadPoolExecutor mExecutor;// 线程池对象

    /**
     * 扫描局域网内ip，找到对应服务器
     */
    public void scan() {
        mDevAddress = getLocAddress();// 获取本机 IP 地址
        mLocAddress = getLocAddrIndex(mDevAddress);// 获取本地 IP 前缀
        Log.d(TAG, "开始扫描设备,本机Ip为：" + mDevAddress);

        if (TextUtils.isEmpty(mLocAddress)) {
            Log.e(TAG, "扫描失败，请检查wifi网络");
            return;
        }

        /**
         * 1.核心池大小 2.线程池最大线程数 3.表示线程没有任务执行时最多保持多久时间会终止
         * 4.参数keepAliveTime的时间单位，有7种取值,当前为毫秒
         * 5.一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，会对线程池的运行过程产生重大影响
         * ，一般来说，这里的阻塞队列有以下几种选择：
         */
        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_IMUM_POOL_SIZE,
                2000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(CORE_POOL_SIZE));

        // 新建线程池
        for (int i = 1; i < 255; i++) {// 创建 256 个线程分别去 ping
            final int lastAddress = i;// 存放 IP 最后一位地址 1-255

            Runnable run = () -> {
                String ping = ScanDeviceTool.this.mPing + mLocAddress + lastAddress;
                String currNetIp = mLocAddress + lastAddress;
                if (mDevAddress.equals(currNetIp)) // 如果与本机 IP 地址相同,跳过
                    return;

                try {
                    mProcess = mRun.exec(ping);

                    int result = mProcess.waitFor();
                    if (result == 0) {
                        Log.d(TAG, "扫描成功,Ip地址为：" + currNetIp);
                        mIpList.add(currNetIp);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "扫描异常" + e.toString());
                } finally {
                    if (mProcess != null)
                        mProcess.destroy();
                }
            };

            mExecutor.execute(run);
        }

        mExecutor.shutdown();

        while (true) {
            try {
                if (mExecutor.isTerminated()) {// 扫描结束,开始验证
                    Log.d(TAG, "扫描结束,总共成功扫描到" + mIpList.size() + "个设备.");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 销毁正在执行的线程池
     *
     * @return void
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
        }
    }

    /**
     * 获取本地ip地址
     *
     * @return String
     */
    public  @NonNull String getLocAddress() {
        String hostIp;
        Pattern ip = Pattern.compile("(" + REGEX_A_IP + ")|" + "(" + REGEX_B_IP + ")|" + "(" + REGEX_C_IP + ")");
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress address;
        if (networkInterfaces != null) {
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    address = inetAddresses.nextElement();
                    String hostAddress = address.getHostAddress();
                    Matcher matcher = ip.matcher(hostAddress);
                    if (matcher.matches()) {
                        hostIp = hostAddress;
                        Log.i(TAG, "本机 IP：" + hostIp);
                        return hostIp;
                    }
                }
            }
        }
        return DEFAULT_IP;
    }

    /**
     * 获取本机IP前缀
     *
     * @param devAddress // 本机IP地址
     * @return String
     */
    private String getLocAddrIndex(String devAddress) {
        if (!devAddress.equals("")) {
            return devAddress.substring(0, devAddress.lastIndexOf(".") + 1);
        }
        return null;
    }

}
