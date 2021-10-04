package hs.iot.zmqworker.service.opc;

import hs.iot.zmqworker.model.dto.iot.IotMeasurePointCell;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import hs.iot.zmqworker.service.iot.IotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 2:32
 */
@Service
@Slf4j
public class OpcService {

    /*客户端注册完成以后的点位*/
    private Map<String/*clientid*/, List<IotReadNodeInfo>> clientRegisterPointCache=new ConcurrentHashMap<>();

    @Autowired
    private IotService iotService;


    public void register(List<IotMeasurePointCell> points){
        if(!CollectionUtils.isEmpty(points)){
            for(IotMeasurePointCell point :points){
                point.setExist(iotService.isPointExist(point));
            }
        }
    }

    public List<IotReadNodeInfo> read(String clientid){
        if(clientRegisterPointCache.containsKey(clientid)){
            if(!CollectionUtils.isEmpty(clientRegisterPointCache.get(clientid))){
                iotService.readLastValue(clientRegisterPointCache.get(clientid));
                List<IotReadNodeInfo> changepoints=clientRegisterPointCache.get(clientid).stream().filter(p->{
                    if(p.getLastValue()==null){
                        p.setLastValue(p.getValue());
                        return true;
                    }else if(!p.getValue().equals(p.getLastValue())){
                        p.setLastValue(p.getValue());
                            return true;
                    }else{
                        p.setLastValue(p.getValue());
                        return false;
                    }
                }).collect(Collectors.toList());
                return changepoints;
            }
        }
        return new ArrayList<>();
    }

    public void write(List<IotReadNodeInfo> points){
        if(!CollectionUtils.isEmpty(points)){
            /*按照node 分类*/
            Map<String,List<IotReadNodeInfo>> nodepoints=points.stream().collect(Collectors.groupingBy(IotReadNodeInfo::getNode,Collectors.toList()));
            nodepoints.forEach((k,v)->{
                iotService.write(v);
            });
        }
    }
}
