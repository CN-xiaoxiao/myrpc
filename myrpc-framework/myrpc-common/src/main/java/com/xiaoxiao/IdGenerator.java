package com.xiaoxiao;


import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

/**
 * 请求id生成器
 * 雪花算法
 * 机房号：5bit
 * 机器号：5bit
 * 时间戳：42bit
 * 序列号：12bit
 * id: 时间戳 机房号 机器号 序列号
 */
public class IdGenerator {
    // 起始时间戳
    public static final long START_STAMP = DateUtil.get("2023-1-1").getTime();

    public static final long DATA_CENTER_BIT = 5L;
    public static final long MACHINE_BIT = 5L;
    public static final long SEQUENCE_BIT = 12L;

    // 最大值
    public static final long DATA_CENTER_MAX = ~(-1L << DATA_CENTER_BIT);
    public static final long MACHINE_MAX = ~(-1L << MACHINE_BIT);
    public static final long SEQUENCE_MAX = ~(-1L << SEQUENCE_BIT);

    public static final long TIMESTAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;
    public static final long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;
    public static final long MACHINE_LEFT = SEQUENCE_BIT;

    private long dataCenterId;
    private long machineId;
    private LongAdder sequenceId = new LongAdder();
    private long lastTimeStamp = -1;

    public IdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > DATA_CENTER_MAX || machineId > MACHINE_MAX) {
            throw new IllegalArgumentException("传入的机房号或机器号不合法");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long getId() {
        long currentTime = System.currentTimeMillis();

        long timeStamp = currentTime - START_STAMP;

        if (timeStamp < lastTimeStamp) {
            throw new RuntimeException("服务器进行了时钟回拨");
        }

        if (timeStamp == lastTimeStamp) {
            sequenceId.increment();

            if (sequenceId.sum() >= SEQUENCE_MAX) {
                timeStamp = getNextTimeStamp();
                sequenceId.reset();
            }

        } else {
            sequenceId.reset();
        }

        lastTimeStamp = timeStamp;

        long sequence = sequenceId.sum();

        return timeStamp << TIMESTAMP_LEFT
                | dataCenterId << DATA_CENTER_LEFT
                | machineId << MACHINE_LEFT
                | sequence;
    }

    private long getNextTimeStamp() {
        long current = System.currentTimeMillis() - START_STAMP;

        while (current == lastTimeStamp) {
            current = System.currentTimeMillis() - START_STAMP;
        }

        return current;
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new IdGenerator(1,2);
        for (int i = 0; i < 1000; i++) {
            new Thread(()-> System.out.println(idGenerator.getId())).start();
        }
    }
}
