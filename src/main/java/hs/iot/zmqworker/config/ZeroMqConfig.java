package hs.iot.zmqworker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 0:16
 */
@Configuration
@ConfigurationProperties(prefix = "zmq")
@Data
public class ZeroMqConfig {
    private String broker;
    private Boolean verbose;
    private String identify;
    private String servename;
    private int liveness;
    private int heartbeat;
    private int timeout;
}
