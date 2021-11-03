package hs.iot.zmqworker.service.handle;

import hs.iot.zmqworker.constant.OperateType;
import hs.iot.zmqworker.model.bean.BaseMessage;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/14 17:40
 */
public interface Handle {
    void handle(List<BaseMessage> data);
    OperateType type();
}
