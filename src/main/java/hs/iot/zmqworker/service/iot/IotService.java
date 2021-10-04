package hs.iot.zmqworker.service.iot;

import hs.iot.zmqworker.config.DataResourceConfig;
import hs.iot.zmqworker.constant.WriteType;
import hs.iot.zmqworker.model.dto.iot.*;
import hs.iot.zmqworker.utils.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 9:31
 */
@Service
@Slf4j
public class IotService implements DataResource {
    @Autowired
    private DataResourceConfig dataResourceConfig;
    /*缓存节点和位号信息*/
    private Map<String/*node*/, Map<String/*code*/, IotMeasurePointCell>> cacheForPoints = new ConcurrentHashMap<>();

    @Override
    public boolean isPointExist(IotMeasurePointCell point) {
        boolean res = false;
        if (cacheForPoints.containsKey(point.getNodeCode()) && cacheForPoints.get(point.getNodeCode()).containsKey(point.getCode())) {
            res = true;
        } else {

            int pageNum = 1;
            int total = 0;
            do {
                MeasurePointsPage measurePointsPage = MeasurePointsPage.builder()
                        .pointType(1)
                        .pageNum(pageNum)
                        .nodeCode(point.getNodeCode())
                        .build();
                IotMeasurePointDto iotMeasurePointDto = HttpClient.postForEntity(dataResourceConfig.getUrl() + dataResourceConfig.getUrlPointPage(), measurePointsPage, IotMeasurePointDto.class);
                pageNum++;
                if (!ObjectUtils.isEmpty(iotMeasurePointDto.getData())) {
                    if (!CollectionUtils.isEmpty(iotMeasurePointDto.getData().getList())) {
                        //加入缓存
                        if (!cacheForPoints.containsKey(point.getNodeCode())) {
                            cacheForPoints.put(point.getNodeCode(), new ConcurrentHashMap<>());
                        }
                        Map<String, IotMeasurePointCell> lastnodecache = iotMeasurePointDto.getData().getList().stream().collect(Collectors.toMap(IotMeasurePointCell::getName, (p) -> p, (o, n) -> n));
                        cacheForPoints.get(point.getNodeCode()).putAll(lastnodecache);
                    }
                    total = iotMeasurePointDto.getData().getPages();
                }

            } while (pageNum <= total);
            if ((cacheForPoints.containsKey(point.getNodeCode()) && cacheForPoints.get(point.getNodeCode()).containsKey(point.getCode())))
            {
                res = true;
            }
        }
        return res;
    }

    @Override
    public void readLastValue(List<IotReadNodeInfo> points) {
        if(CollectionUtils.isEmpty(points)){
            return;
        }
        IotDcsReadDto iotDcsReadDto=new IotDcsReadDto();
        iotDcsReadDto.setSample(1);
        iotDcsReadDto.setPoints(points);
        IotResponse responseEntity=null;
        responseEntity=HttpClient.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlRead(),iotDcsReadDto,responseEntity.getClass());
        Map<String,IotReadNodeInfo> pendingupdtaPoints=points.stream().collect(Collectors.toMap(IotReadNodeInfo::getNode,p->p,(o,n)->n));
        if(responseEntity!=null&&responseEntity.getStatus().equals(200)){
            if (!CollectionUtils.isEmpty(responseEntity.getData())) {

                responseEntity.getData().forEach(p->{
                    if(pendingupdtaPoints.containsKey(p.getMeasurePoint())){
                        pendingupdtaPoints.get(p.getMeasurePoint()).setValue(p.getData().get(0).getValue());;
                    }
                });
            }
        }
    }

    @Override
    public void write(List<IotReadNodeInfo> points) {
        IotDcsWriteDto iotDcsWriteDto=IotDcsWriteDto.builder()
                .points(points)
                .build();
        if(!CollectionUtils.isEmpty(points) && !StringUtils.isEmpty(points.get(0).getNode())){
            if(dataResourceConfig.getNodeWriteMapping().containsKey(points.get(0).getNode())){
                if(dataResourceConfig.getNodeWriteMapping().get(points.get(0).getNode()).equals(WriteType.WRITE_TYPE_WRITE.getCode())){
                    HttpClient.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlWrite(),iotDcsWriteDto,String.class);
                }else if (dataResourceConfig.getNodeWriteMapping().get(points.get(0).getNode()).equals(WriteType.WRITE_TYPE_UPLOAD.getCode())){
                    HttpClient.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlUpload(),iotDcsWriteDto,String.class);
                }
            }else{
                HttpClient.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlUpload(),iotDcsWriteDto,String.class);
            }
        }

    }
}
