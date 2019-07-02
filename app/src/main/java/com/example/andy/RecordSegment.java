package com.example.andy;

/**
 * 回放时间和长度
 */

public class RecordSegment {

    private String mBeginTime;

    private String mEndTime;

    private long mSize;

    public RecordSegment() {
    }

    public String getBeginTime() {
        return this.mBeginTime;
    }

    public void setBeginTime(String beginTime) {
        this.mBeginTime = beginTime;
    }

    public String getEndTime() {
        return this.mEndTime;
    }

    public void setEndTime(String endTime) {
        this.mEndTime = endTime;
    }

    public long getSize() {
        return this.mSize;
    }

    public void setSize(long size) {
        this.mSize = size;
    }

}
