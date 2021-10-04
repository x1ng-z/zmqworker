package hs.iot.zmqworker.model.dto.iot;

import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 12:02
 */
@Data
public class IotResponse {
    private Integer status;
    private String message;
    private List<IotMeasurePointValueDto> data;
}
