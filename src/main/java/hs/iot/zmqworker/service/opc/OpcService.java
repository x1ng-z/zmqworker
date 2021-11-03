package hs.iot.zmqworker.service.opc;


import hs.iot.zmqworker.config.DataResourceConfig;
import hs.iot.zmqworker.config.ZeroMqConfig;
import hs.iot.zmqworker.constant.DataType;
import hs.iot.zmqworker.constant.OperateType;
import hs.iot.zmqworker.model.bean.BaseMessage;
import hs.iot.zmqworker.model.bean.WriteMessage;
import hs.iot.zmqworker.model.dto.iot.IotMeasurePointCell;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import hs.iot.zmqworker.model.dto.opc.RegisterPointDto;
import hs.iot.zmqworker.service.handle.Handle;
import hs.iot.zmqworker.service.handle.HandleManage;
import hs.iot.zmqworker.service.iot.IotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static hs.iot.zmqworker.constant.OperateType.OPERATE_TYPE_WRITE;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 2:32
 */
@Service
@Slf4j
@DependsOn(value="iotService")
public class OpcService{

    /*客户端注册完成以后的点位,换成下注册点位，用于读取数据时提交至iot*/
    //主要用于位号数据缓存
    private Map<String/*tag <opcserve style>*/,IotReadNodeInfo> clientRegisterPointCache=new ConcurrentHashMap<>();

    private LinkedBlockingQueue<BaseMessage> writeMessageQueue =new LinkedBlockingQueue();

    private IotService iotService;
    private ExecutorService appExecutorService;
    private ExecutorService writeExecutorService;
    private ZeroMqConfig zeroMqConfig;
    private HandleManage handleManage;
    private DataResourceConfig dataResourceConfig;
    @Autowired
    public OpcService(IotService iotService,
                      @Qualifier("app-thread")
                      ExecutorService appExecutorService,
                      @Qualifier("write-thread")
                      ExecutorService writeExecutorService,
                      ZeroMqConfig zeroMqConfig,
                      DataResourceConfig dataResourceConfig,
                      HandleManage handleManage) {
        this.iotService=iotService;
        this.appExecutorService = appExecutorService;
        this.writeExecutorService=writeExecutorService;
        this.zeroMqConfig=zeroMqConfig;
        this.dataResourceConfig=dataResourceConfig;
        this.handleManage=handleManage;


        //第一次先将数据缓冲到这里面
        List<IotMeasurePointCell> iotMeasurePointCells=iotService.initDataResource();
        iotMeasurePointCells.forEach(i->{
            IotReadNodeInfo iotReadNodeInfo=IotReadNodeInfo.builder()
                    .node(i.getNodeCode())//zeroMqConfig.getServename()+"/"+p.getNode()
                    .name(i.getName())
                    .id(i.getId())
                    .exist(true)
                    .measurePoint(i.getCode())
                    .build();
            clientRegisterPointCache.put(i.getCode(),iotReadNodeInfo);
        });

        //处理写消息的处理线程
        appExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()){
                    try {
                        BaseMessage baseMessage= writeMessageQueue.take();
                        //判断messagequeue内的数据大小
                        List<BaseMessage> batchMessage=new ArrayList<>();
                        int batchSize= writeMessageQueue.drainTo(batchMessage,dataResourceConfig.getBatchWrite()<=0?500:dataResourceConfig.getBatchWrite());
                        batchMessage.add(baseMessage);
                        Long bengin=System.currentTimeMillis();
                        log.debug("WRITE BATCH SIZE={}",batchSize+1);
                        //采用写进程进行处理
//                        writeExecutorService.execute(new Runnable() {
//                            @Override
//                            public void run() {
                                Handle handle=handleManage.getHandleMapp().get(OperateType.OPERATE_TYPE_WRITE);
                                handle.handle(batchMessage);
//                            }
//                        });
                        log.debug("WRITE BATCH SIZE={} write complete,cost={}",batchSize+1,System.currentTimeMillis()-bengin);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(),e);
                    }
                }

            }
        });


    }


    /**
     * @param registerPointDto  opcserve style
     * */
    public void register(RegisterPointDto registerPointDto){
        List<IotMeasurePointCell> points=registerPointDto.getPoints();
        if(!CollectionUtils.isEmpty(points)){
            for(IotMeasurePointCell point :points){
                if(iotService.isPointExist(point)){
                    point.setExist(true);
                    IotReadNodeInfo iotReadNodeInfo=new IotReadNodeInfo();
                    iotReadNodeInfo.setNode(point.getNodeCode());
                    iotReadNodeInfo.setMeasurePoint(point.getCode());
                    iotReadNodeInfo.setDataType(point.getPointType());
                    if(!clientRegisterPointCache.containsKey(iotReadNodeInfo.getMeasurePoint())){
                        clientRegisterPointCache.put(iotReadNodeInfo.getMeasurePoint(),iotReadNodeInfo);
                    }
                }else{
                    point.setExist(false);
                }

            }
        }
    }

    public List<IotReadNodeInfo> read(){
        List<IotReadNodeInfo> res=new ArrayList<>(clientRegisterPointCache.values());;
            if(!CollectionUtils.isEmpty(res)){
                iotService.read(new ArrayList<>(clientRegisterPointCache.values()));
                //更新改为去缓存中获取数据
                List<IotReadNodeInfo> changepoints=res.stream().filter(p->{
                        p.setDataType(0);//初始化成无效数据类型，后面会进行判断赋值
                    //数据类型判断
                    if(!ObjectUtils.isEmpty(p.getValue())){
                        Arrays.stream(DataType.values()).forEach(c->{
                            if(c.getCode()!=DataType.DATA_TYPE_INVALIDE.getCode()&&c.getClazz().isInstance(p.getValue())){
                                p.setDataType(c.getCode());
                            }
                        });
                        if(p.getValue().toString().contains(".")){
                            try {
                                Double aDouble=Double.valueOf(p.getValue().toString());
                                if(aDouble<=Float.MIN_VALUE || aDouble>=Float.MAX_VALUE){
                                    p.setDataType(DataType.DATA_TYPE_DOUBLE.getCode());
                                }else{
                                    p.setDataType(DataType.DATA_TYPE_FLOAT.getCode());
                                }
                            } catch (NumberFormatException e) {

                            }
                        }else{
                            try {
                                Long aLong=Long.valueOf(p.getValue().toString());
                                if(aLong<=Integer.MIN_VALUE||aLong>=Float.MAX_VALUE){
                                    p.setDataType(DataType.DATA_TYPE_LONG.getCode());
                                }else{
                                    p.setDataType(DataType.DATA_TYPE_INT.getCode());
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                        //数据是否发生变化了
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
                    }else {
                        return false;
                    }

                }).collect(Collectors.toList());

                return changepoints;
            }
        return res;
    }

    public void write(List<IotReadNodeInfo> points){
        if(!CollectionUtils.isEmpty(points)){
            WriteMessage writeMessage=new WriteMessage();
            writeMessage.setPoints(points);
            writeMessage.setOperateType(OPERATE_TYPE_WRITE);
            writeMessageQueue.offer(writeMessage);
            //这里默认反写成功了
            points.forEach(p->{
                p.setIswrite(true);
            });
        }
    }

    public List<IotMeasurePointCell> initDataResource(){
        //reset lastvalue
        clientRegisterPointCache.values().forEach((p)->{p.setLastValue(null);});
        return iotService.initDataResource();
    }


}
