package hs.iot.zmqworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZmqworkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZmqworkerApplication.class, args);
    }

}
