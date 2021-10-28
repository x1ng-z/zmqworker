package hs.iot.zmqworker.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 22:47
 */
@Getter
public enum OperateType {
    OPERATE_TYPE_INVALIDE(0,"无效操作"),
    OPERATE_TYPE_REGISTER(1,"注册点位"),
    OPERATE_TYPE_READ(2,"读取点位"),
    OPERATE_TYPE_WRITE(3,"写点位"),
    OPERATE_TYPE_INIT(4,"初始化")
    ;
    private int code;
    private String desc;

    OperateType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
