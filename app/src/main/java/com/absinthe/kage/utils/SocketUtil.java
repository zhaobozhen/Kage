package com.absinthe.kage.utils;

import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.DataAckProtocol;
import com.absinthe.kage.protocol.DataProtocol;
import com.absinthe.kage.protocol.PingAckProtocol;
import com.absinthe.kage.protocol.PingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class SocketUtil {

    private static Map<Integer, String> msgImp = new HashMap<>();

    static {
        msgImp.put(DataProtocol.PROTOCOL_TYPE, "com.absinthe.kage.protocol.DataProtocol");       //0
        msgImp.put(DataAckProtocol.PROTOCOL_TYPE, "com.absinthe.kage.protocol.DataAckProtocol"); //1
        msgImp.put(PingProtocol.PROTOCOL_TYPE, "com.absinthe.kage.protocol.PingProtocol");       //2
        msgImp.put(PingAckProtocol.PROTOCOL_TYPE, "com.absinthe.kage.protocol.PingAckProtocol"); //3
    }

    /**
     * Parse content data
     *
     * @param data Data
     * @return Protocol
     */
    public static BaseProtocol parseContentMsg(byte[] data) {
        int protocolType = BaseProtocol.parseType(data);
        String className = msgImp.get(protocolType);
        BaseProtocol BaseProtocol;
        try {
            BaseProtocol = (BaseProtocol) Class.forName(className).newInstance();
            BaseProtocol.parseContentData(data);
        } catch (Exception e) {
            BaseProtocol = null;
            e.printStackTrace();
        }
        return BaseProtocol;
    }

    /**
     * Read data
     *
     * @param inputStream InputStream
     * @return Protocol
     */
    public static BaseProtocol readFromStream(InputStream inputStream) {
        BaseProtocol protocol;
        BufferedInputStream bis;

        //header中保存的是整个数据的长度值，4个字节表示。在下述write2Stream方法中，会先写入header
        byte[] header = new byte[BaseProtocol.LENGTH_LEN];

        try {
            bis = new BufferedInputStream(inputStream);

            int temp;
            int len = 0;
            while (len < header.length) {
                temp = bis.read(header, len, header.length - len);
                if (temp > 0) {
                    len += temp;
                } else if (temp == -1) {
                    bis.close();
                    return null;
                }
            }

            len = 0;
            int length = byteArrayToInt(header);    //data length
            byte[] content = new byte[length];
            while (len < length) {
                temp = bis.read(content, len, length - len);

                if (temp > 0) {
                    len += temp;
                }
            }

            protocol = parseContentMsg(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return protocol;
    }

    /**
     * Write data
     *
     * @param protocol Protocol
     * @param outputStream OutputStream
     */
    public static void write2Stream(BaseProtocol protocol, OutputStream outputStream) {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        byte[] buffData = protocol.genContentData();
        byte[] header = int2ByteArrays(buffData.length);
        try {
            bufferedOutputStream.write(header);
            bufferedOutputStream.write(buffData);
            bufferedOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close InputStream
     *
     * @param is InputStream
     */
    public static void closeInputStream(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close OutputStream
     *
     * @param os OutputStream
     */
    public static void closeOutputStream(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] int2ByteArrays(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] b) {
        int intValue = 0;
        for (int i = 0; i < b.length; i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - i)); //Integer takes 4 bytes
        }
        return intValue;
    }

    public static int byteArrayToInt(byte[] b, int byteOffset, int byteCount) {
        int intValue = 0;
        for (int i = byteOffset; i < (byteOffset + byteCount); i++) {
            intValue += (b[i] & 0xFF) << (8 * (3 - (i - byteOffset)));
        }
        return intValue;
    }

    public static int bytes2Int(byte[] b, int byteOffset) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        byteBuffer.put(b, byteOffset, 4);   //Integer takes 4 bytes
        byteBuffer.flip();
        return byteBuffer.getInt();
    }
}