package com.xiaoxiao;

import com.xiaoxiao.annotation.TryTimes;

public interface HelloMyrpc {

    /**
     * 通用接口，server和client都需要依赖
     * @param msg 发生的具体的消息
     * @return 返回的信息
     */
    @TryTimes(tryTimes = 3, intervalTime = 3000)
    String sayHi(String msg);
}
