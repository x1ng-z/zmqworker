package hs.iot.zmqworker.model.dto.iot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 14:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeasurePointsPage {
    private int pageNum;
    private String nodeCode;
    private int pointType;
}
