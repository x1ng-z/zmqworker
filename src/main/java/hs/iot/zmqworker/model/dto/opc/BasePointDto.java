package hs.iot.zmqworker.model.dto.opc;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 22:55
 */
@Data
public class BasePointDto implements Serializable {
    private int operate;//操作
    private int status;//执行状态，200执行成功
    private String id;//客户端id
}
