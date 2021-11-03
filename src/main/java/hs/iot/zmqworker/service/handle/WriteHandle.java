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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
    public void handle(List<BaseMessage> baseMessages) {
        if(CollectionUtils.isEmpty(baseMessages)){
            return;
        }
//         if(!(baseMessage instanceof WriteMessage)){
//             return;
//         }
        /*按照node 分类*/
        Map<String, List<IotReadNodeInfo>> res =new HashMap<String, List<IotReadNodeInfo>>();
        for(BaseMessage msg:baseMessages){

            WriteMessage writeMessage=(WriteMessage)msg;

            if(!CollectionUtils.isEmpty(writeMessage.getPoints())){

                writeMessage.getPoints().forEach(this::converIotStyle);

                Map<String, List<IotReadNodeInfo>> nodepoints=writeMessage.getPoints().stream().filter(p->p.getDataType()!= DataType.DATA_TYPE_INVALIDE.getCode()).collect(Collectors.groupingBy(IotReadNodeInfo::getNode,Collectors.toList()));
                nodepoints.forEach((k,v)->{
                    if(!res.containsKey(k)){
                        res.put(k,new ArrayList<>());
                    }
                    res.get(k).addAll(v);
                });
            }
        }
        res.values().stream().forEach((v)->{
            iotService.write(v);
        });
    }

    @Override
    public OperateType type() {
        return OperateType.OPERATE_TYPE_WRITE;
    }

    private boolean converIotStyle(IotReadNodeInfo opcServeStylecell){
        if(StringUtils.isEmpty(opcServeStylecell.getMeasurePoint())){
            return false;
        }
        /**
         * [0]servename iot
         * [1]nodecode temp
         * [2] tag
         * */
        String[] partilePointInfos=opcServeStylecell.getMeasurePoint().split("/");
        if(partilePointInfos.length<2){
            return false;
        }else{
            opcServeStylecell.setNode(partilePointInfos[1]);
            opcServeStylecell.setMeasurePoint(opcServeStylecell.getMeasurePoint().substring((partilePointInfos[0]+"/"+partilePointInfos[1]+"/").length()));
            return true;
        }

    }
}
