package hs.iot.zmqworker;

import com.alibaba.fastjson.JSON;
import hs.iot.zmqworker.model.dto.opc.BasePointDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

@SpringBootTest
class ZmqworkerApplicationTests {

    @Test
    void contextLoads() {

        byte[] test_="中文".getBytes(StandardCharsets.UTF_8);

        for(byte b:test_){
            System.out.println(String.format("Duke's Name: %x", b&0xff));

        }

        BasePointDto basePointDto=new BasePointDto();

        System.out.println(JSON.toJSONString(basePointDto));;

        try (ZContext context = new ZContext()) {
            // Socket to talk to clients
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:5555");

            while (!Thread.currentThread().isInterrupted()) {
                // Block until a message is received
                byte[] reply = socket.recv(0);

                // Print the message
                System.out.println(
                        "Received: [" + new String(reply, ZMQ.CHARSET) + "]"
                );

                // Send a response
                String response = "Hello, world!";
                socket.send(response.getBytes(ZMQ.CHARSET), 0);
            }
        }
    }

}
