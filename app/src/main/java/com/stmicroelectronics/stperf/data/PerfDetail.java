package com.stmicroelectronics.stperf.data;

import android.os.Parcel;
import android.os.Parcelable;

public class PerfDetail implements Parcelable {

    public final static int BAR_MAX_FPS = 60;
    public final static int BAR_MAX_PERCENT = 100;
    final static String CPU_NO_INFO = "no info";

    private String mCpuInfo;
    private float mCpu;
    private float mCpu0;
    private float mCpu1;
    private float mGpu;
    private int mFps;

    PerfDetail() {
        mCpuInfo = CPU_NO_INFO;
        mCpu = 0;
        mCpu0 = 0;
        mCpu1 = 0;
        mGpu = 0;
        mFps = 0;
    }

    private PerfDetail(Parcel in) {
        mCpuInfo = in.readString();
        mCpu = in.readFloat();
        mCpu0 = in.readFloat();
        mCpu1 = in.readFloat();
        mGpu = in.readFloat();
        mFps = in.readInt();
    }

    public static final Creator<PerfDetail> CREATOR = new Creator<PerfDetail>() {
        @Override
        public PerfDetail createFromParcel(Parcel in) {
            return new PerfDetail(in);
        }

        @Override
        public PerfDetail[] newArray(int size) {
            return new PerfDetail[size];
        }
    };

    void setCpuInfo(String cpuInfo) {
        this.mCpuInfo = cpuInfo;
    }

    public String getCpuInfo() {
        return this.mCpuInfo;
    }

    void setCpu(float cpu) {
        if (cpu < BAR_MAX_PERCENT) {
            this.mCpu = cpu;
        } else {
            this.mCpu = BAR_MAX_PERCENT;
        }
    }

    public float getCpu() {
        return mCpu;
    }

    void setCpu0(float cpu0) {
        if (cpu0 < BAR_MAX_PERCENT) {
            this.mCpu0 = cpu0;
        } else {
            this.mCpu0 = BAR_MAX_PERCENT;
        }
    }

    public float getCpu0() {
        return mCpu0;
    }

    void setCpu1(float cpu1) {
        if (cpu1 < BAR_MAX_PERCENT) {
            this.mCpu1 = cpu1;
        } else {
            this.mCpu1 = BAR_MAX_PERCENT;
        }
    }

    public float getCpu1() {
        return mCpu1;
    }

    void setGpu(float gpu) {
        if (gpu < BAR_MAX_PERCENT) {
            this.mGpu = gpu;
        } else {
            this.mGpu = BAR_MAX_PERCENT;
        }
    }

    public float getGpu() {
        return mGpu;
    }

    void setFps(int fps) {
        this.mFps = Math.min(fps, BAR_MAX_FPS);
    }

    public int getFps() {
        return mFps;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCpuInfo);
        dest.writeFloat(mCpu);
        dest.writeFloat(mCpu0);
        dest.writeFloat(mCpu1);
        dest.writeFloat(mGpu);
        dest.writeInt(mFps);
    }
}
