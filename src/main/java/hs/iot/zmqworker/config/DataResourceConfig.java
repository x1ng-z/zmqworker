package hs.iot.zmqworker.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 0:37
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "dataresource")
public class DataResourceConfig implements InitializingBean {
    private String url;
    private String[] nodes;
    private String[] writeType;
    private Long flush;
    private int batchWrite;

    private Map<String, String> nodeWriteMapping = new HashMap();

    private String urlUpload = "/api/measure-point/upload";

    private String urlRead = "/api/measure-point/read/latest";

    private String urlWrite = "/api/measure-point/write";

    private String urlPointPage = "/api/measure-point/page";

    private String urliotnodelist = "/api/node/list";
    @Override
    public void afterPropertiesSet() throws Exception {
        if ((!isEmpty(nodes))&&(!isEmpty(writeType))) {
            if (nodes.length==writeType.length){
                for(int index=0;index<nodes.length;index++){
                    nodeWriteMapping.put(nodes[index],writeType[index]);
                }

            }
        }
    }


    private <T> boolean isEmpty(T[] value) {
        if (value == null || value.length == 0) {
            return true;
        }
        return false;
    }
}
