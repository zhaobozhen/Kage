package com.absinthe.kage.protocol;

import androidx.annotation.NonNull;

import com.absinthe.kage.utils.SocketUtil;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;

public class DataProtocol extends BaseProtocol implements Serializable {

    public static final int PROTOCOL_TYPE = 0;

    private static final int PATTION_LEN = 1;
    private static final int DTYPE_LEN = 1;
    private static final int MSGID_LEN = 4;

    private int pattion;
    private int dtype;
    private int msgId;

    private String data;

    @Override
    public int getLength() {
        return super.getLength() + PATTION_LEN + DTYPE_LEN + MSGID_LEN + data.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getPattion() {
        return pattion;
    }

    public void setPattion(int pattion) {
        this.pattion = pattion;
    }

    public int getDtype() {
        return dtype;
    }

    public void setDtype(int dtype) {
        this.dtype = dtype;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public int getMsgId() {
        return msgId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    /**
     * Splice sending data
     *
     * @return Content data
     */
    @Override
    public byte[] genContentData() {
        byte[] base = super.genContentData();
        byte[] pattion = {(byte) this.pattion};
        byte[] dtype = {(byte) this.dtype};
        byte[] msgId = SocketUtil.int2ByteArrays(this.msgId);
        byte[] data = this.data.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);          //协议版本＋数据类型＋数据长度＋消息id
        baos.write(pattion, 0, PATTION_LEN);       //业务类型
        baos.write(dtype, 0, DTYPE_LEN);           //业务数据格式
        baos.write(msgId, 0, MSGID_LEN);           //消息id
        baos.write(data, 0, data.length);          //业务数据
        return baos.toByteArray();
    }

    /**
     * Parse receiving data in order
     *
     * @param data Data
     * @return Content data
     * @throws ProtocolException Exception
     */
    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //parse pattion
        pattion = data[pos] & 0xFF;
        pos += PATTION_LEN;

        //parse dtype
        dtype = data[pos] & 0xFF;
        pos += DTYPE_LEN;

        //parse msgId
        msgId = SocketUtil.byteArrayToInt(data, pos, MSGID_LEN);
        pos += MSGID_LEN;

        //parse data
        this.data = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);

        return pos;
    }

    @NonNull
    @Override
    public String toString() {
        return "Data: " + data;
    }
}
