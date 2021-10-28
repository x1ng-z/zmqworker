package hs.iot.zmqworker.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/6 11:49
 */
@Getter
public enum ServeStatus {
    SERVE_STATUS_500(500,"服务器内部错误"),
    SERVE_STATUS_400(400,"服务未找到"),
    SERVE_STATUS_200(200,"服务正常"),
    ;
    private int code;
    private String desc;

    ServeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
