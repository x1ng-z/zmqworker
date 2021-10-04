package hs.iot.zmqworker.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 1:04
 */
@Getter
public enum WriteType {
    WRITE_TYPE_UPLOAD("upload","influx"),
    WRITE_TYPE_WRITE("write","dcs");

    private String code;
    private String desc;

    WriteType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
