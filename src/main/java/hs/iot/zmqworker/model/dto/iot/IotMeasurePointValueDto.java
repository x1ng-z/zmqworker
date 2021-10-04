package hs.iot.zmqworker.model.dto.iot;

import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 12:04
 */
@Data
public class IotMeasurePointValueDto {
    private String node;
    private String measurePoint;
    private List<CellValue> data;
}
