package hs.iot.zmqworker.service.handle;

import hs.iot.zmqworker.constant.DataType;
import hs.iot.zmqworker.constant.OperateType;
import hs.iot.zmqworker.model.bean.BaseMessage;
import hs.iot.zmqworker.model.bean.WriteMessage;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import hs.iot.zmqworker.service.iot.IotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/14 17:41
 */
@Service
@Slf4j
public class WriteHandle implements Handle{
    @Autowired
    private IotService iotService;
    @Override
    public void handle(BaseMessage baseMessage) {
         if(!(baseMessage instanceof WriteMessage)){
             return;
         }
        WriteMessage writeMessage=(WriteMessage)baseMessage;
        /*按照node 分类*/
        if(!CollectionUtils.isEmpty(writeMessage.getPoints())){
            Map<String, List<IotReadNodeInfo>> nodepoints=writeMessage.getPoints().stream().filter(p->p.getDataType()!= DataType.DATA_TYPE_INVALIDE.getCode()).collect(Collectors.groupingBy(IotReadNodeInfo::getNode,Collectors.toList()));
            nodepoints.values().stream().forEach((v)->{
                iotService.write(v);
            });
        }

    }

    @Override
    public OperateType type() {
        return OperateType.OPERATE_TYPE_WRITE;
    }
}
