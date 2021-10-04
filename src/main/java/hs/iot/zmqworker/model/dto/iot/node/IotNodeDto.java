package hs.iot.zmqworker.model.dto.iot.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/8/13 14:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IotNodeDto {
    private List<NodeInfoDto> data;
    private Integer status;
    private String messgae;
}
