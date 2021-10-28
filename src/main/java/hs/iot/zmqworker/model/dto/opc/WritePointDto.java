package hs.iot.zmqworker.model.dto.opc;

import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 22:54
 */
@Data
public class WritePointDto extends BasePointDto {
    private List<IotReadNodeInfo> points;
}
