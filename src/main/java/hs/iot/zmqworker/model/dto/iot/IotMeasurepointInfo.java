package hs.iot.zmqworker.model.dto.iot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 14:37
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IotMeasurepointInfo {
    private int pageNum;

    private int pageSize;
    private int size;
    private int total;
    private int pages;

    private List<IotMeasurePointCell> list;

}
