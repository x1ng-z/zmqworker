package hs.iot.zmqworker.service.iot;

import com.alibaba.fastjson.JSONObject;
import hs.iot.zmqworker.config.DataResourceConfig;
import hs.iot.zmqworker.config.ZeroMqConfig;
import hs.iot.zmqworker.constant.DataType;
import hs.iot.zmqworker.constant.WriteType;
import hs.iot.zmqworker.model.dto.iot.*;
import hs.iot.zmqworker.model.dto.iot.node.IotNodeDto;
import hs.iot.zmqworker.model.dto.iot.node.NodeInfoDto;
import hs.iot.zmqworker.service.handle.HandleManage;
import hs.iot.zmqworker.utils.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 9:31
 */
@Service(value="iotService")
@Slf4j
public class IotService implements DataResource, InitializingBean {
    @Autowired
    private DataResourceConfig dataResourceConfig;

    @Autowired
    private ZeroMqConfig zeroMqConfig;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ExecutorService executorService;



    /*缓存节点和位号信息*/
    private Map<String/*nodecode*/, Map<String/*code <iot style>*/, IotReadNodeInfo>> cacheForPoints = new ConcurrentHashMap<>();




    /**项目启动去iot获取点位数据,并开启线程进行数据*/
    @Override
    public void afterPropertiesSet() throws Exception {
        //第一次直接自动刷新
        flushPointCache();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                flushDataCache();
            }
        });
    }

    /**
     * iot点位信息刷新
     * 每隔15分钟刷新iot点位信息一次，首次启动延期10分钟
     * */
    @Scheduled(fixedRate = 1000*60*15,initialDelay = 1000*60*10)
    public void cycleFlushPointsCache(){
        flushPointCache();
    }


    /**数据刷新*/
    public void flushDataCache() {
        while (!Thread.interrupted()){
            log.debug("try to flush data");
            try {
                if(!CollectionUtils.isEmpty(cacheForPoints)){
                    cacheForPoints.keySet().stream().forEach(node->{
                        readLastValue(cacheForPoints.get(node).values());
                    });
                }
                TimeUnit.MILLISECONDS.sleep(dataResourceConfig.getFlush()<=0?5000:dataResourceConfig.getFlush());
            } catch (InterruptedException e) {
                return;
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }

        }
    }

    /**获取iot点位信息，并刷新*/
    private void flushPointCache(){
        //获取iot节点信息
        List<String> nodeList = new ArrayList<>();

        ResponseEntity<IotNodeDto> responseEntity = restTemplate.getForEntity(dataResourceConfig.getUrl() + dataResourceConfig.getUrliotnodelist(), IotNodeDto.class);


        if (!ObjectUtils.isEmpty(responseEntity.getBody())) {
            IotNodeDto iotNodeDto =responseEntity.getBody(); //JSONObject.parseObject(nodeinfocontext, IotNodeDto.class);
            if (iotNodeDto.getStatus() == 200) {
                if (!CollectionUtils.isEmpty(iotNodeDto.getData())) {
                    iotNodeDto.getData().stream().filter(NodeInfoDto::get_switch).forEach(node -> {
                        nodeList.add(node.getCode());
                    });
                }

            }
        }

        log.info("parse node info complete={}",nodeList.toString());

        log.info("parse points info.....");
        //将关闭的节点数据点位数据移除
        List<String> oldNodes=new ArrayList<>(cacheForPoints.keySet());
        oldNodes.removeAll(nodeList);//减去现存节点，剩余就是已经关闭的节点
        if(!CollectionUtils.isEmpty(oldNodes)){
            oldNodes.forEach(on->{
                Map<String/*code <iot style>*/, IotReadNodeInfo> discardNodepoints=cacheForPoints.remove(on);
                if(!CollectionUtils.isEmpty(discardNodepoints)){
                    discardNodepoints.clear();//help gc
                }
            });

        }
        for(String node:nodeList){
            log.info("parse points info......node={}",node);
            int pageNum = 1;
            int total = 0;
            do {
                MeasurePointsPage measurePointsPage = MeasurePointsPage.builder()
                        .pointType(1)
                        .pageNum(pageNum)
                        .nodeCode(node)
                        .build();
                ResponseEntity<IotMeasurePointDto> iotMeasurePointDto =restTemplate.postForEntity(dataResourceConfig.getUrl() + dataResourceConfig.getUrlPointPage(), measurePointsPage, IotMeasurePointDto.class);
                pageNum++;
                if (!ObjectUtils.isEmpty(iotMeasurePointDto.getBody())&&!ObjectUtils.isEmpty(iotMeasurePointDto.getBody().getData())) {
                    if (!CollectionUtils.isEmpty(iotMeasurePointDto.getBody().getData().getList())) {
                        //加入缓存
                        if (!cacheForPoints.containsKey(node)) {
                            cacheForPoints.put(node, new ConcurrentHashMap<>());
                        }
                        if(!CollectionUtils.isEmpty(iotMeasurePointDto.getBody().getData().getList())){
                            //将缓存数据缓存进点位缓存；
                            cacheForPoints.get(node).putAll(iotMeasurePointDto.getBody().getData().getList().stream().collect(Collectors.toMap(IotMeasurePointCell::getCode,p->{
                                return IotReadNodeInfo.builder()
                                        .measurePoint(p.getCode())
                                        .name(p.getName())
                                        .id(p.getId())
                                        .node(p.getNodeCode())
                                        .exist(true).build();
                            },(o,n)->n)));
                        }

                    }
                    total = iotMeasurePointDto.getBody().getData().getPages();
                }

            } while (pageNum <= total);

        }

    }



    /*判断点位是否存在于iot中*/
    @Override
    public boolean isPointExist(IotMeasurePointCell point) {
        boolean res = false;
        //转化为iot格式
        if(!converIotStyle(point)){
            return false;
        }

        if (cacheForPoints.containsKey(point.getNodeCode()) && cacheForPoints.get(point.getNodeCode()).containsKey(point.getCode())) {
            point.setName(cacheForPoints.get(point.getNodeCode()).get(point.getCode()).getName());
            point.setId(cacheForPoints.get(point.getNodeCode()).get(point.getCode()).getId());
            res = true;
        } else {
            res= false;
        }
        //转化回opcserve格式
        converOPCServeStyle(zeroMqConfig.getServename(),point);
        return res;
    }

    /*读取iot中的最新数据*/
    @Override
    public void readLastValue(Collection<IotReadNodeInfo> points) {
//        if(CollectionUtils.isEmpty(points)){
//            return;
//        }
        //组装提交的数据
        IotDcsReadDto iotDcsReadDto=new IotDcsReadDto();
        iotDcsReadDto.setSample(1);
        List<IotReadNodeInfo> tmppoints=new ArrayList<>();
        tmppoints.addAll(points);
        iotDcsReadDto.setPoints(tmppoints);
        ResponseEntity<IotResponse> responseEntity=null;
        //请求iot最新数据
        responseEntity=restTemplate.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlRead(),iotDcsReadDto,IotResponse.class);
        //将待更新的点位重新组合成map，key=tag，value=类本身
        Map<String,IotReadNodeInfo> pendingupdtaPoints=points.stream().collect(Collectors.toMap(IotReadNodeInfo::getMeasurePoint,p->p,(o,n)->n));

        if(responseEntity!=null&&responseEntity.getStatusCode().equals(HttpStatus.OK)){
            ;
            if (!ObjectUtils.isEmpty(responseEntity.getBody())&&!CollectionUtils.isEmpty(responseEntity.getBody().getData())) {
                //将数据更新到对应的点位上去。
                responseEntity.getBody().getData().forEach(p->{
                    if(pendingupdtaPoints.containsKey(p.getMeasurePoint())){
                        if(!CollectionUtils.isEmpty(p.getData())){
                            //数据更新
                            pendingupdtaPoints.get(p.getMeasurePoint()).setValue(p.getData().get(0).getValue());
                            //时间更新
                            pendingupdtaPoints.get(p.getMeasurePoint()).setTime(p.getData().get(0).getTime());
                        }

                    }
                });
            }
        }

    }

    /*将数据返回至iot中*/
    @Override
    public void write(List<IotReadNodeInfo> points) {

        List<IotReadNodeInfo> point_s=points.stream().filter(p->{return converIotStyle(p);}).collect(Collectors.toList());

        IotDcsWriteDto iotDcsWriteDto=IotDcsWriteDto.builder()
                .points(point_s)
                .build();
        if(!CollectionUtils.isEmpty(points) && !StringUtils.isEmpty(points.get(0).getNode())){
            if(dataResourceConfig.getNodeWriteMapping().containsKey(points.get(0).getNode())){
                if(dataResourceConfig.getNodeWriteMapping().get(points.get(0).getNode()).equals(WriteType.WRITE_TYPE_WRITE.getCode())){
                    restTemplate.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlWrite(),iotDcsWriteDto,String.class);
                }else if (dataResourceConfig.getNodeWriteMapping().get(points.get(0).getNode()).equals(WriteType.WRITE_TYPE_UPLOAD.getCode())){
                    restTemplate.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlUpload(),iotDcsWriteDto,String.class);
                }
            }else{
                restTemplate.postForEntity(dataResourceConfig.getUrl()+dataResourceConfig.getUrlUpload(),iotDcsWriteDto,String.class);
            }
        }


        point_s.forEach(p->{converOPCServeStyle(zeroMqConfig.getServename(),p);});
    }

    public  void read(List<IotReadNodeInfo> needUpdateTags){
        needUpdateTags.forEach(p->{
            if(converIotStyle(p)){
                if(cacheForPoints.containsKey(p.getNode())){
                    if(cacheForPoints.get(p.getNode()).containsKey(p.getMeasurePoint())){
                        p.setValue(cacheForPoints.get(p.getNode()).get(p.getMeasurePoint()).getValue());
                        p.setTime(cacheForPoints.get(p.getNode()).get(p.getMeasurePoint()).getTime());
                    }
                }
                converOPCServeStyle(zeroMqConfig.getServename(),p);
            }

        });
    }

    public List<IotMeasurePointCell> initDataResource(){
        List<IotMeasurePointCell> res =new ArrayList<>();
        cacheForPoints.values().forEach((cellmapp)->{
            cellmapp.values().forEach(p->{
                IotMeasurePointCell iotReadNodeInfo=new IotMeasurePointCell();
                iotReadNodeInfo.setPointType(DataType.DATA_TYPE_DOUBLE.getCode());
                iotReadNodeInfo.setName(p.getName());
                iotReadNodeInfo.setCode(zeroMqConfig.getServename()+"/"+p.getNode()+"/"+p.getMeasurePoint());
                iotReadNodeInfo.setExist(true);
                iotReadNodeInfo.setId(p.getId());
                res.add(iotReadNodeInfo);
            });
        });

        return res;
    }


    /**
     * @param opcServeStylecell
     * @return false 转化失败
     * */
    private boolean converIotStyle(IotMeasurePointCell opcServeStylecell){
        if(StringUtils.isEmpty(opcServeStylecell.getCode())){
            return false;
        }
        /**
         * [0]servename iot
         * [1]nodecode temp
         * [2] tag
         * */
        String[] partilePointInfos=opcServeStylecell.getCode().split("/");
        if(partilePointInfos.length<2){
            return false;
        }else{
            opcServeStylecell.setNodeCode(partilePointInfos[1]);
            opcServeStylecell.setCode(opcServeStylecell.getCode().substring((partilePointInfos[0]+"/"+partilePointInfos[1]+"/").length()));
            return true;
        }

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


    private boolean converOPCServeStyle(String serveName,IotMeasurePointCell iotStylecell){
        if(StringUtils.isEmpty(iotStylecell.getCode())){
            return false;
        }
        /**
         * [0]servename iot
         * [1]nodecode temp
         * [2] tag
         * */
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append(serveName);
        stringBuilder.append("/");
        stringBuilder.append(iotStylecell.getNodeCode());
        stringBuilder.append("/");
        stringBuilder.append(iotStylecell.getCode());
        iotStylecell.setCode(stringBuilder.toString());
        return true;

    }

    private boolean converOPCServeStyle(String serveName,IotReadNodeInfo iotStylecell){
        if(StringUtils.isEmpty(iotStylecell.getMeasurePoint())){
            return false;
        }
        /**
         * [0]servename iot
         * [1]nodecode temp
         * [2] tag
         * */
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append(serveName);
        stringBuilder.append("/");
        stringBuilder.append(iotStylecell.getNode());
        stringBuilder.append("/");
        stringBuilder.append(iotStylecell.getMeasurePoint());
        iotStylecell.setMeasurePoint(stringBuilder.toString());
        return true;

    }
}
