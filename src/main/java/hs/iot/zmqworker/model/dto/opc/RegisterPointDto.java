package hs.iot.zmqworker.model.dto.opc;

import hs.iot.zmqworker.model.dto.iot.IotMeasurePointCell;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 22:47
 */
@Data
public class RegisterPointDto extends BasePointDto {
    private List<IotMeasurePointCell> points;
}
