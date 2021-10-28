package hs.iot.zmqworker.service.iot;

import hs.iot.zmqworker.model.dto.iot.IotMeasurePointCell;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 0:32
 */
public interface DataResource {
    /**
     * 是否存在点位
     * */
    boolean isPointExist(IotMeasurePointCell point);

    /**
     *点位数据读取
     * */
    void readLastValue(Collection<IotReadNodeInfo> points);

    /**
     * 数据反写
     * */
    void write(List<IotReadNodeInfo> points);


}
