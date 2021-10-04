package hs.iot.zmqworker.model.dto.iot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/7/27 9:42
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IotDcsReadDto implements Serializable {
    private int sample=1;
    private List<IotReadNodeInfo> points;
}
