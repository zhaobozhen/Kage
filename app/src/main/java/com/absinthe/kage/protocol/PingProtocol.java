package com.absinthe.kage.protocol;

import com.absinthe.kage.utils.SocketUtil;

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;

public class PingProtocol extends BaseProtocol {

    public static final int PROTOCOL_TYPE = 2;

    private static final int PING_ID_LEN = 4;

    private int pingId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + PING_ID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getPingId() {
        return pingId;
    }

    public void setPingId(int pingId) {
        this.pingId = pingId;
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
        byte[] pingId = SocketUtil.int2ByteArrays(this.pingId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);          //protocol version + data type + data length + msgId
        baos.write(pingId, 0, PING_ID_LEN);        //msgId
        baos.write(unused, 0, unused.length);      //unused
        return baos.toByteArray();
    }

    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //parse pingId
        pingId = SocketUtil.byteArrayToInt(data, pos, PING_ID_LEN);
        pos += PING_ID_LEN;

        unused = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);

        return pos;
    }
}
