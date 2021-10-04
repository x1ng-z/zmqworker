package hs.iot.zmqworker.model.dto.iot;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/7/27 9:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IotReadNodeInfo implements Serializable {
     private String node;
     private String measurePoint;
     private Object value;
     private Long time;
     @JSONField(serialize = false)
     private boolean isExist;
     @JSONField(serialize = false)
     private Object lastValue;/*上一次的值*/
}
