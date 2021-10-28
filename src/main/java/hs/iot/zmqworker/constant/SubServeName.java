package hs.iot.zmqworker.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/10/15 10:21
 */
@Getter
public enum SubServeName {
    SUB_SERVE_NAME_ADD("/register","添加点位"),
    SUB_SERVE_NAME_READ("/read","读取数据"),
    SUB_SERVE_NAME_WRITE("/write","写点位"),
    SUB_SERVE_NAME_("/init","初始化");
    ;

    private String name;
    private String desc;

    SubServeName(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
