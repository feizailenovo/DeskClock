package com.feizai.deskclock.data;

/**
 * @Author: chenhao
 * @Date: 2022/3/28-0028 下午 05:48:40
 * @Describe:
 */
public class SnoozeAlarm {
    private int id;
    private long time;

    public SnoozeAlarm() {
    }

    public SnoozeAlarm(int id, long time) {
        this.id = id;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "SnoozeAlarm{" +
                "id=" + id +
                ", time=" + time +
                '}';
    }
}
