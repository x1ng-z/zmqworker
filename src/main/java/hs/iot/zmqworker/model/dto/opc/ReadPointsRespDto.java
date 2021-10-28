package hs.iot.zmqworker.model.dto.opc;

import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 23:38
 */
@Data
public class ReadPointsRespDto extends BasePointDto {
    private List<IotReadNodeInfo> points;
}
