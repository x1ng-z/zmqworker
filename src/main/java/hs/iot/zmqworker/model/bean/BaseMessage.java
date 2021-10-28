package hs.iot.zmqworker.model.bean;

import hs.iot.zmqworker.constant.OperateType;
import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/14 17:36
 */
@Data
public class BaseMessage {
    private OperateType operateType;
}
