package hs.iot.zmqworker.service.zmq;

import hs.iot.zmqworker.config.ZeroMqConfig;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import hs.iot.zmqworker.service.iot.IotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zeromq.ZMsg;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Majordomo Protocol worker example. Uses the mdwrk API to hide all MDP aspects
 */
@Slf4j
@Service
public class Mdworker {

    @Autowired
    private ZeroMqConfig zeroMqConfig;
    @Value("${spring.application.name:echo}")
    private String applicationName;

    @Autowired
    private IotService iotService;

    public void run() {
//        boolean verbose = (args.length > 0 && "-v".equals(args[0]));
        Mdwrkapi workerSession = new Mdwrkapi(zeroMqConfig.getBroker(), applicationName, zeroMqConfig.getVerbose());
        ZMsg reply = null;
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg request = workerSession.receive(reply);
            if (request == null) {
                break; //Interrupted
            }

            reply = request; //  Echo is complex :-)
        }
        workerSession.destroy();
    }
    
}
