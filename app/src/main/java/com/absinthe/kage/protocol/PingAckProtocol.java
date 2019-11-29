package com.absinthe.kage.protocol;

import com.absinthe.kage.utils.SocketUtil;

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;

public class PingAckProtocol extends BaseProtocol {

    public static final int PROTOCOL_TYPE = 3;

    private static final int ACK_PING_ID_LEN = 4;

    private int ackPingId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + ACK_PING_ID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getAckPingId() {
        return ackPingId;
    }

    public void setAckPingId(int ackPingId) {
        this.ackPingId = ackPingId;
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
        byte[] ackPingId = SocketUtil.int2ByteArrays(this.ackPingId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);                //protocol version + data type + data length + msgId
        baos.write(ackPingId, 0, ACK_PING_ID_LEN);         //msgId
        baos.write(unused, 0, unused.length);            //unused
        return baos.toByteArray();
    }

    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //parse ackPingId
        ackPingId = SocketUtil.byteArrayToInt(data, pos, ACK_PING_ID_LEN);
        pos += ACK_PING_ID_LEN;

        //parse unused
        unused = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);

        return pos;
    }
}
