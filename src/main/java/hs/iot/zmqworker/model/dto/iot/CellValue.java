package hs.iot.zmqworker.model.dto.iot;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 12:05
 */
@Data
public class CellValue {
    private Object value;
    private Long time;
}
