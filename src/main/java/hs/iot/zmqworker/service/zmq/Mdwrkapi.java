package hs.iot.zmqworker.service.zmq;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import hs.iot.zmqworker.config.ZeroMqConfig;
import hs.iot.zmqworker.constant.MDP;
import hs.iot.zmqworker.service.iot.IotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zeromq.*;

/**
 * Majordomo Protocol Client API, Java version Implements the MDP/Worker spec at
 * http://rfc.zeromq.org/spec:7.
 */
@Slf4j
public class Mdwrkapi {

    private static final int HEARTBEAT_LIVENESS = 3; // 3-5 is reasonable

    private String broker;
    private ZContext ctx;
    private String service;

    private ZMQ.Socket worker;           // Socket to broker
    private long heartbeatAt;      // When to send HEARTBEAT
    private int liveness;         // How many attempts left
    private int heartbeat = 2500; // Heartbeat delay, msecs
    private int reconnect = 2500; // Reconnect delay, msecs

    // Internal state
    private boolean expectReply = false; // false only at start

    private long timeout = 2500;
    private boolean verbose=false;                            // Print activity to stdout
//    private Formatter log     = new Formatter(System.out);

    // Return address, if any
    private ZFrame replyTo;
    private ZFrame translationId;


    private IotService iotService;
    private ZeroMqConfig zeroMqConfig;
    private String identify;
    public Mdwrkapi(IotService iotService, String broker, String service, boolean verbose,String identify,ZeroMqConfig zeroMqConfig) {
        assert (broker != null);
        assert (service != null);
        this.broker = broker;
        this.service = service;
        this.identify=identify;
        this.verbose = verbose;
        this.iotService = iotService;
        this.zeroMqConfig=zeroMqConfig;
        liveness=zeroMqConfig.getLiveness();
        heartbeat=zeroMqConfig.getHeartbeat();
        timeout=zeroMqConfig.getTimeout();
        ctx = new ZContext();
        reconnectToBroker();
    }

    /**
     * Send message to broker If no msg is provided, creates one internally
     *
     * @param command
     * @param option
     * @param msg
     */
    void sendToBroker(MDP command, String option, ZMsg msg) {
        msg = msg != null ? msg.duplicate() : new ZMsg();

        // Stack protocol envelope to start of message
        if (option != null) {
            msg.addFirst(new ZFrame(option));
        }


        msg.addFirst(command.newFrame());
        msg.addFirst(MDP.W_WORKER.newFrame());
        msg.addFirst(new ZFrame(ZMQ.MESSAGE_SEPARATOR));

        if (verbose) {
            log.debug("I: sending {} to broker\n", command);
            msg.dump(System.out);
        }
        msg.send(worker);
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() {
        if (worker != null) {
            ctx.destroySocket(worker);
        }
        worker = ctx.createSocket(SocketType.DEALER);
        worker.setLinger(0);
        worker.setIdentity(identify.getBytes(StandardCharsets.UTF_8));
        worker.connect(broker);
        if (verbose) {
            log.debug("I: connecting to broker at {}\n", broker);
        }


        // Register service with broker
        sendToBroker(MDP.W_READY, service, null);

        // If liveness hits zero, queue is considered disconnected
        liveness = zeroMqConfig.getLiveness();//HEARTBEAT_LIVENESS; by zzx
        heartbeatAt = System.currentTimeMillis() + heartbeat;

    }

    /**
     * Send reply, if any, to broker and wait for next request.
     */
    public ZMsg receive(ZMsg reply) {

        // Format and send the reply if we were provided one
        assert (reply != null || !expectReply);

        if (reply != null) {
            assert (replyTo != null);
            reply.wrap(replyTo);
            sendToBroker(MDP.W_REPLY, null, reply);
            reply.destroy();
        }
        expectReply = true;

        while (!Thread.currentThread().isInterrupted()) {
            // Poll socket for a reply, with timeout
            ZMQ.Poller items = ctx.createPoller(1);
            items.register(worker, ZMQ.Poller.POLLIN);
            if (items.poll(timeout) == -1) {
                break; // Interrupted
            }

            if (items.pollin(0)) {
                ZMsg msg = ZMsg.recvMsg(worker);
                if (msg == null) {
                    break; // Interrupted
                }

                if (verbose) {
                    log.debug("I: received message from broker: \n");
                    msg.dump(System.out);
                }
                liveness = zeroMqConfig.getLiveness();//HEARTBEAT_LIVENESS;
                // Don't try to handle errors, just assert noisily
                /**
                * 接受到的格式
                 * [0]分隔符
                 * [1]服务/客户端标识（此处无意义）
                 * [2]command【心跳】【请求】【断开链接】等
                 * [3]
                * */
                assert (msg != null && msg.size() >= 3);

                ZFrame empty = msg.pop();
                assert (empty.getData().length == 0);
                empty.destroy();

                ZFrame header = msg.pop();
                assert (MDP.W_WORKER.frameEquals(header));
                header.destroy();

                ZFrame command = msg.pop();
                if (MDP.W_REQUEST.frameEquals(command)) {
                    // We should pop and save as many addresses as there are
                    // up to a null part, but for now, just save one
                    replyTo = msg.unwrap();
                    command.destroy();
                    return msg; // We have a request to process
                } else if (MDP.W_HEARTBEAT.frameEquals(command)) {
                    // Do nothing for heartbeats
                } else if (MDP.W_DISCONNECT.frameEquals(command)) {
                    reconnectToBroker();
                } else {
                    log.error("E: invalid input message: \n");
                    msg.dump(System.out);
                }
                command.destroy();
                msg.destroy();
            } else if (--liveness == 0) {
                if (verbose) {
                    log.debug("W: disconnected from broker - retrying\n");
                }
                try {
                    Thread.sleep(reconnect);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the
                    // interrupted status
                    break;
                }
                reconnectToBroker();

            }
            // Send HEARTBEAT if it's time
            if (System.currentTimeMillis() > heartbeatAt) {
                sendToBroker(MDP.W_HEARTBEAT, null, null);
                heartbeatAt = System.currentTimeMillis() + heartbeat;
            }
            items.close();
        }
        if (Thread.currentThread().isInterrupted()) {
            log.debug("W: interrupt received, killing worker\n");
        }
        return null;
    }

    public void destroy() {
        ctx.destroy();
    }

    // ==============   getters and setters =================
    public int getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }

    public int getReconnect() {
        return reconnect;
    }

    public void setReconnect(int reconnect) {
        this.reconnect = reconnect;
    }

}
