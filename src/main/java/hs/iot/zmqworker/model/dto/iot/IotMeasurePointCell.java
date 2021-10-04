package hs.iot.zmqworker.model.dto.iot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 14:39
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class IotMeasurePointCell {
    private Long id;
    private String name;
    private String lineCode;
    private String code;
    private String nodeCode;
    private int pointType;
    private boolean isExist;
}
