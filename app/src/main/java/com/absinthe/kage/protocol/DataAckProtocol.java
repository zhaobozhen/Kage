package com.absinthe.kage.protocol;

import com.absinthe.kage.utils.SocketUtil;

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;

public class DataAckProtocol extends BaseProtocol {

    public static final int PROTOCOL_TYPE = 1;

    private static final int ACKMSGID_LEN = 4;

    private int ackMsgId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + ACKMSGID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getAckMsgId() {
        return ackMsgId;
    }

    public void setAckMsgId(int ackMsgId) {
        this.ackMsgId = ackMsgId;
    }

    public String getUnused() {
        return unused;
    }

    public void setUnused(String unused) {
        this.unused = unused;
    }

    /**
     * Splice sending data
     *
     * @return Content data
     */
    @Override
    public byte[] genContentData() {
        byte[] base = super.genContentData();
        byte[] ackMsgId = SocketUtil.int2ByteArrays(this.ackMsgId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);              //protocol version + data type + data length + msgId
        baos.write(ackMsgId, 0, ACKMSGID_LEN);         //msgId
        baos.write(unused, 0, unused.length);          //unused
        return baos.toByteArray();
    }

    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //parse ackMsgId
        ackMsgId = SocketUtil.byteArrayToInt(data, pos, ACKMSGID_LEN);
        pos += ACKMSGID_LEN;

        //parse unused
        unused = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);

        return pos;
    }
}