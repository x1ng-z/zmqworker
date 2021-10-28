package hs.iot.zmqworker.service.zmq;

import com.alibaba.fastjson.JSON;
import hs.iot.zmqworker.config.ZeroMqConfig;
import hs.iot.zmqworker.constant.DataType;
import hs.iot.zmqworker.constant.OperateType;
import hs.iot.zmqworker.constant.ServeStatus;
import hs.iot.zmqworker.constant.SubServeName;
import hs.iot.zmqworker.model.dto.iot.IotMeasurePointCell;
import hs.iot.zmqworker.model.dto.iot.IotReadNodeInfo;
import hs.iot.zmqworker.model.dto.opc.*;
import hs.iot.zmqworker.service.iot.IotService;
import hs.iot.zmqworker.service.opc.OpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Majordomo Protocol worker example. Uses the mdwrk API to hide all MDP aspects
 */
@Slf4j
@Service
public class Mdworker{


    private ZeroMqConfig zeroMqConfig;

    private IotService iotService;

    private OpcService opcService;

    private ExecutorService executorService;

    public Mdworker(ZeroMqConfig zeroMqConfig,
                    IotService iotService,
                    OpcService opcService,
                    ExecutorService executorService) {
        this.zeroMqConfig = zeroMqConfig;

        this.iotService = iotService;
        this.opcService = opcService;
        this.executorService=executorService;

        //开启服务，处理代理转发的消息
        Arrays.stream(SubServeName.values())/*.filter((s)->(s.getName().equals("/init")||s.getName().equals("/register")))*/.forEach(s->{
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    buildService(s.getName());
                }
            });
        });

    }


    //服务子名称
    private void buildService(String subName) {
//        boolean verbose = (args.length > 0 && "-v".equals(args[0]));
        Mdwrkapi workerSession = new Mdwrkapi(iotService, zeroMqConfig.getBroker(), zeroMqConfig.getServename()+subName, zeroMqConfig.getVerbose(), zeroMqConfig.getIdentify()+subName,zeroMqConfig);
        ZMsg reply = null;
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg request = workerSession.receive(reply);
            if (request == null) {
                break; //Interrupted
            }
            /**
             * 获取消息内容
             * 消息格式
             *json context
             * */
            //打印下消息内容
            //log.debug(request.toString());
            ZFrame translationId = request.pop();
            ZFrame context = request.pop();
            String frameContext = context.getString(ZMQ.CHARSET);
            //log.info("frameContext={}", frameContext);
            BasePointDto basePointDto = JSON.parseObject(frameContext, BasePointDto.class);
            Map<Integer, OperateType> operateTypeMap = Arrays.stream(OperateType.values()).collect(Collectors.toMap(OperateType::getCode, (o) -> o, (o, n) -> n));
            if (operateTypeMap.containsKey(basePointDto.getOperate())) {

                switch (operateTypeMap.get(basePointDto.getOperate())) {
                    case OPERATE_TYPE_INVALIDE: {
                        log.warn("get a invalide operate!");
                        basePointDto.setStatus(ServeStatus.SERVE_STATUS_400.getCode());

                        request.addFirst(new ZFrame(JSON.toJSONString(basePointDto)));
                        request.addFirst(translationId);
                        break;
                    }
                    case OPERATE_TYPE_READ: {
                        ReadPointDto readPointDto = JSON.parseObject(frameContext, ReadPointDto.class);
                        log.debug("OPERATE_TYPE_READ");
                        List<IotReadNodeInfo> readRes = opcService.read();
                        //移除数据类型为无效的数据
                        List<IotReadNodeInfo> validReadRes=readRes.stream().filter(p->{return (p.getDataType()!=DataType.DATA_TYPE_INVALIDE.getCode());}).collect(Collectors.toList());
                        ReadPointsRespDto readPointsRespDto = new ReadPointsRespDto();
                        readPointsRespDto.setPoints(validReadRes);
                        readPointsRespDto.setId(readPointDto.getId());
                        readPointsRespDto.setOperate(OperateType.OPERATE_TYPE_READ.getCode());
                        readPointsRespDto.setStatus(ServeStatus.SERVE_STATUS_200.getCode());

                        request.addFirst(new ZFrame(JSON.toJSONString(readPointsRespDto)));
                       // log.debug("OPERATE_TYPE_READ={}",JSON.toJSONString(readPointsRespDto));
                        request.addFirst(translationId);
                        break;
                    }
                    case OPERATE_TYPE_WRITE: {
                        WritePointDto writePointDto = JSON.parseObject(frameContext, WritePointDto.class);
                        log.debug("OPERATE_TYPE_WRIT");
                        opcService.write(writePointDto.getPoints());
                        writePointDto.setStatus(ServeStatus.SERVE_STATUS_200.getCode());

                        request.addFirst(new ZFrame(JSON.toJSONString(writePointDto)));
                        request.addFirst(translationId);
                        break;
                    }

                    case OPERATE_TYPE_INIT:{
                        log.debug("OPERATE_TYPE_INIT={}",translationId);
                        List<IotMeasurePointCell> points=opcService.initDataResource();
                        RegisterPointDto dto=new RegisterPointDto();
                        dto.setPoints(points);
                        dto.setId(basePointDto.getId());
                        dto.setOperate(basePointDto.getOperate());
                        dto.setStatus(ServeStatus.SERVE_STATUS_200.getCode());
                        //log.debug("OPERATE_TYPE_INIT={}",JSON.toJSONString(dto));
                        request.addFirst(new ZFrame(JSON.toJSONString(dto)));
                        request.addFirst(translationId);
                        break;
                    }
                    case OPERATE_TYPE_REGISTER: {

                        RegisterPointDto registerPointDto = JSON.parseObject(frameContext, RegisterPointDto.class);
                        log.debug("OPERATE_TYPE_REGISTER");
                        registerPointDto = JSON.parseObject(frameContext, RegisterPointDto.class);
                        opcService.register(registerPointDto);
                        registerPointDto.setStatus(ServeStatus.SERVE_STATUS_200.getCode());
                        registerPointDto.getPoints().stream().filter(IotMeasurePointCell::isExist).forEach(p -> {
                            //设置存在的点位，但是没有设置数据类型，直接设置成double类型
                            if (p.getPointType() == DataType.DATA_TYPE_INVALIDE.getCode()) {
                                p.setPointType(DataType.DATA_TYPE_DOUBLE.getCode());
                            }
                        });

                        request.addFirst(new ZFrame(JSON.toJSONString(registerPointDto)));
                        request.addFirst(translationId);
//                        log.debug("snd msg: {}", JSON.toJSONString(registerPointDto));

                        break;
                    }
                    default: {
                        basePointDto.setStatus(ServeStatus.SERVE_STATUS_400.getCode());

                        request.addFirst(new ZFrame(JSON.toJSONString(basePointDto)));
                        request.addFirst(translationId);
                        log.warn("get a unknow operate!");
                    }
                }
            } else {
                basePointDto.setStatus(ServeStatus.SERVE_STATUS_400.getCode());

                request.addFirst(new ZFrame(JSON.toJSONString(basePointDto)));
                request.addFirst(translationId);
                //request.destroy();
                //request=null;
                log.warn("get a unknow operate={}", basePointDto.getOperate());
            }
            context.destroy();
            //translationId.destroy();
//            request.getFirst();

//            request.destroy();
            reply = request; //  Echo is complex :-)
        }
        workerSession.destroy();
    }

}
