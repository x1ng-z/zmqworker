package hs.iot.zmqworker.service.handle;

import hs.iot.zmqworker.constant.OperateType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/14 17:57
 */
@Service
@Slf4j
public class HandleManage  {
    @Autowired
    private List<Handle> handles;

    private Map<OperateType,Handle>handMapping=new HashMap<>();;

    public Map<OperateType,Handle> getHandleMapp()   {
        if(CollectionUtils.isEmpty(handMapping)){
            if(!CollectionUtils.isEmpty(handles)){
                handMapping= handles.stream().collect(Collectors.toMap(Handle::type,p->p,(o,n)->n));
            }
        }
        return handMapping;
    }

}
