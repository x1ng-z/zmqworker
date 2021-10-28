package hs.iot.zmqworker.constant;

import java.util.Arrays;

import org.zeromq.ZFrame;
import org.zeromq.ZMQ;

/**
 * Majordomo Protocol definitions, Java version
 */
public enum MDP
{

    /**
     * This is the version of MDP/Client we implement
     * 连接的类型：客户端
     */
    C_CLIENT("MDPC"),

    /**
     * This is the version of MDP/Worker we implement
     * 连接的类型：服务器
     */
    W_WORKER("MDPW"),

    // MDP/Server commands, as byte values
    W_READY(1),
    W_REQUEST(2),
    W_REPLY(3),
    W_HEARTBEAT(4),
    W_DISCONNECT(5);

    private final byte[] data;

    MDP(String value)
    {
        this.data = value.getBytes(ZMQ.CHARSET);
    }

    MDP(int value)
    { //watch for ints>255, will be truncated
        byte b = (byte) (value & 0xFF);
        this.data = new byte[] { b };
    }

    public ZFrame newFrame()
    {
        return new ZFrame(data);
    }

    public boolean frameEquals(ZFrame frame)
    {
        return Arrays.equals(data, frame.getData());
    }
}
