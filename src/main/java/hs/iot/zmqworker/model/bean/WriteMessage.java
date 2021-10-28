package hs.iot.zmqworker.model.bean;

import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/14 17:44
 */
@Data
public class WriteMessage extends BaseMessage{
    private List<IotReadNodeInfo> points;
}
