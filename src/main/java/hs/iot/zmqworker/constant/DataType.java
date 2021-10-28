package hs.iot.zmqworker.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/5 12:56
 */
@Getter
public enum DataType {
    DATA_TYPE_INVALIDE(0,"invalid data type",Object.class),
    DATA_TYPE_INT(1,"int",Integer.class),
    DATA_TYPE_LONG(2,"long",Long.class),
    DATA_TYPE_STRING(3,"string",String.class),
    DATA_TYPE_FLOAT(4,"float",Float.class),
    DATA_TYPE_DOUBLE(5,"double",Double.class),
    DATA_TYPE_BOOL(6,"bool",Boolean.class),
    ;
    private int code;
    private String desc;
    private Class clazz;

    DataType(int code, String desc,Class<?> clazz) {
        this.code = code;
        this.desc = desc;
        this.clazz=clazz;
    }
}
