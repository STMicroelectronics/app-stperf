package com.stmicroelectronics.stperf.data;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.stmicroelectronics.stperf.service.PerfLayout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class PerfData implements Handler.Callback {

    private static final String CPU_INFO_PATH = "/proc/stat";
    private static final String GPU_INFO_PATH = "/sys/devices/platform/soc@0/42080000.bus/48280000.gpu/idle";
    private static final String FPS_INFO_PATH = "/sys/kernel/debug/dri/0/state";

    private static final String GPU_VERSION_PATH = "/sys/devices/platform/soc@0/42080000.bus/48280000.gpu/version";
    private static final String GPU_VERSION_OLD = "6.2.4";
    private static boolean mGpuNewVersion = false;

    private static final String CPU_FREQ_GET_AVAILABLE_SPEED_PATH = "/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies";
    private static final String CPU_FREQ_GET_SPEED_PATH = "/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq";
    private static final String CPU_FREQ_SET_SPEED_PATH = "/sys/devices/system/cpu/cpufreq/policy0/scaling_setspeed";

    private static final String PERF_DETAIL = "PerfDetail";

    private static float mIdleCpu = 0.0F;
    private static float mIdleCpu0 = 0.0F;
    private static float mIdleCpu1 = 0.0F;
    private static float mTotalCpu = 0.0F;
    private static float mTotalCpu0 = 0.0F;
    private static float mTotalCpu1 = 0.0F;

    private static float mGpuPrevOn = 0.0F;
    private static float mGpuPrevTotal = 0.0F;

    private static List<String> mFrequencies;
    private static String mCurFrequency;
    private static int mToken = 0;

    private PerfDetail mPerfDetail;
    private final PerfLayout mPerfLayout;

    private boolean mLog;

    private final Handler mHandler;
    private final Thread mThread;

    private boolean mCpuInfoUpdated = true;

    private long mPeriod;
    private final AtomicBoolean mSchedule = new AtomicBoolean(false);

    public PerfData(PerfLayout layout) {
        mPerfLayout = layout;
        mPerfDetail = new PerfDetail();

        mFrequencies = getAvailableCpuFrequencies();
        mCurFrequency = getCpuFrequency();
        if ((mFrequencies.get(mToken).equals(mCurFrequency)) && (mFrequencies.size() > 1)) {
            mToken++;
        }
        Timber.d("Current frequency: %s", mCurFrequency);
        int i = 0;
        for (String freq:mFrequencies) {
            Timber.d("Available frequency (%d): %s", i, freq);
            i++;
        }
        updateGpuVersion();

        Runnable runnable = new Runnable() {
            final Bundle perfBundle = new Bundle();
            Message message;

            @Override
            public void run() {
                try {
                    while (mSchedule.get()) {
                        Thread.sleep(mPeriod);

                        PerfDetail perfDetail = new PerfDetail();
                        updateCpu(perfDetail);
                        if (mGpuNewVersion) {
                            updateGpuNew(perfDetail);
                        } else {
                            updateGpu(perfDetail);
                        }
                        updateFps(perfDetail);
                        if ((mCpuInfoUpdated) || (perfDetail.getCpuInfo().contains(PerfDetail.CPU_NO_INFO))) {
                            mCpuInfoUpdated = false;
                            perfDetail.setCpuInfo(formatFrequency(mCurFrequency));
                        }
                        perfBundle.putParcelable(PERF_DETAIL, perfDetail);
                        message = mHandler.obtainMessage();
                        message.setData(perfBundle);
                        mHandler.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    Timber.e(e.toString());
                }
            }
        };
        mThread = new Thread(runnable);
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    public void startDataUpdate(long period) {
        mSchedule.set(true);
        mPeriod = period;
        mThread.start();
    }

    public void stopDataUpdate() {
        mSchedule.set(false);
    }

    public void setPeriod(long period) {
        mPeriod = period;
    }

    public void setLog(boolean log) {
        mLog = log;
    }

    private String getCpuFrequency() {
        String str = PerfDetail.CPU_NO_INFO;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(CPU_FREQ_GET_SPEED_PATH))), 32);
            str = localBufferedReader.readLine();
            localBufferedReader.close();
        } catch (IOException e) {
            Timber.e(e.toString());
        }
        return str;
    }

    private String formatFrequency(String value) {
        value = value.substring(0, value.length() - 3);
        value = value.concat("MHz");
        return value;
    }

    private List<String> getAvailableCpuFrequencies() {
        String str = PerfDetail.CPU_NO_INFO;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(CPU_FREQ_GET_AVAILABLE_SPEED_PATH))), 64);
            str = localBufferedReader.readLine();
            localBufferedReader.close();
        } catch (IOException e) {
            Timber.e(e.toString());
        }
        return Arrays.asList(str.split("\\s"));
    }

    public void setNextCpuFrequency() {
        String speed;
        if (! mFrequencies.isEmpty()) {
            if (mFrequencies.size() == 1) {
                // only one frequency available, no need to switch
                return;
            }
            speed = mFrequencies.get(mToken);
            mToken++;
            if (mToken >= mFrequencies.size()) {
                mToken = 0;
            }
            try {
                FileOutputStream fop = new FileOutputStream(CPU_FREQ_SET_SPEED_PATH, false);
                try {
                    fop.write(speed.getBytes());
                    fop.flush();
                    fop.close();
                    mCurFrequency = speed;
                    mCpuInfoUpdated = true;
                } catch (IOException e) {
                    Timber.e("Not possible to write value to %s", CPU_FREQ_SET_SPEED_PATH);
                }
            } catch (FileNotFoundException e) {
                Timber.e("Not possible to open %s", CPU_FREQ_SET_SPEED_PATH);
            }
        }
    }

    private void updateCpu(PerfDetail perfDetail) {
        float value;
        long total, idle;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(CPU_INFO_PATH))), 800);
            String str = localBufferedReader.readLine() + localBufferedReader.readLine() + localBufferedReader.readLine();
            localBufferedReader.close();
            String[] arrayOfString = str.split(" ");

            // total = user + nice + system + idle + iowait + irq + softirq + steal
            total = Long.parseLong(arrayOfString[2]) + Long.parseLong(arrayOfString[3]) + Long.parseLong(arrayOfString[4])
                    + Long.parseLong(arrayOfString[5]) + Long.parseLong(arrayOfString[6]) + Long.parseLong(arrayOfString[7])
                    + Long.parseLong(arrayOfString[8]) + Long.parseLong(arrayOfString[9]);
            // idle = idle + iowait
            idle = Long.parseLong(arrayOfString[5]) + Long.parseLong(arrayOfString[6]);
            // cpu usage = (total - oldTotal) - (idle - oldIdle) / (total - oldTotal)
            value = 100.0F * (((float)total - mTotalCpu) - ((float)idle - mIdleCpu)) / ((float)total - mTotalCpu);
            perfDetail.setCpu(value);
            mTotalCpu = (float)total;
            mIdleCpu = (float)idle;

            // total = user + nice + system + idle + iowait + irq + softirq + steal
            total = Long.parseLong(arrayOfString[12]) + Long.parseLong(arrayOfString[13]) + Long.parseLong(arrayOfString[14])
                    + Long.parseLong(arrayOfString[15]) + Long.parseLong(arrayOfString[16]) + Long.parseLong(arrayOfString[17])
                    + Long.parseLong(arrayOfString[18]) + Long.parseLong(arrayOfString[19]);
            // idle = idle + iowait
            idle = Long.parseLong(arrayOfString[15]) + Long.parseLong(arrayOfString[16]);
            // cpu usage = (total - oldTotal) - (idle - oldIdle) / (total - oldTotal)
            value = 100.0F * (((float)total - mTotalCpu0) - ((float)idle - mIdleCpu0)) / ((float)total - mTotalCpu0);
            perfDetail.setCpu0(value);
            mTotalCpu0 = (float)total;
            if(mTotalCpu0 < 1) mTotalCpu0 = 1;
            mIdleCpu0 = (float)idle;

            total = Long.parseLong(arrayOfString[22]) + Long.parseLong(arrayOfString[23]) + Long.parseLong(arrayOfString[24])
                    + Long.parseLong(arrayOfString[25]) + Long.parseLong(arrayOfString[26]) + Long.parseLong(arrayOfString[27])
                    + Long.parseLong(arrayOfString[28]) + Long.parseLong(arrayOfString[29]);
            idle = Long.parseLong(arrayOfString[25]) + Long.parseLong(arrayOfString[26]);
            value = 100.0F * (((float)total - mTotalCpu1) - ((float)idle - mIdleCpu1)) / ((float)total - mTotalCpu1);
            perfDetail.setCpu1(value);
            mTotalCpu1 = (float)total;
            if(mTotalCpu1 <1) mTotalCpu1 =1;
            mIdleCpu1 = (float)idle;
        } catch (IOException e) {
            Timber.e("failed to update CPUs Load ; %s", e.toString());
        }
    }

    private void updateGpuVersion() {
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(GPU_VERSION_PATH)), 64);
            try {
                String str = localBufferedReader.readLine();
                localBufferedReader.close();
                String[] version = str.trim().split("\\s+");
                mGpuNewVersion = !version[0].startsWith(GPU_VERSION_OLD);
                Timber.d("GPU version %s", version[0]);
            } catch (IOException e) {
                Timber.e(e.toString());
            }
        } catch (FileNotFoundException e) {
            Timber.e(e.toString());
        }
    }

    /*
    Case with following information (old version):
    Start:   8754197086 ns
    End:     159501327032 ns
    On:      16803348339 ns
    Off:     110378128095 ns
    Idle:    0 ns
    Suspend: 23565653512 ns
    */
    private void updateGpu(PerfDetail perfDetail) {
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(GPU_INFO_PATH))), 200);
            String str = localBufferedReader.readLine() + localBufferedReader.readLine() + localBufferedReader.readLine();
            localBufferedReader.close();
            String[] arrayOfString = str.split(" +");

            // GPU_Load = 100 * On / (End - Start)
            long start = Long.parseLong(arrayOfString[1]);
            long end = Long.parseLong(arrayOfString[3]);
            long on = Long.parseLong(arrayOfString[5]);
            float value = 100.0F * (float)on / ((float)end - (float)start);
            perfDetail.setGpu(value);

        } catch (IOException e) {
            Timber.e("failed to update GPU Load ; %s ", e.toString());
        }
    }

    /*
    Case with following information (new version):
    On:                   81,478,235,933 ns
    Off:                 269,353,777,254 ns
    Idle:                              0 ns
    Suspend:              86,398,908,271 ns
    */
    private void updateGpuNew(PerfDetail perfDetail) {
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(GPU_INFO_PATH))), 200);
            String str = localBufferedReader.readLine() + localBufferedReader.readLine() + localBufferedReader.readLine() + localBufferedReader.readLine();
            localBufferedReader.close();
            String[] arrayOfString = str.split(" +");

            // GPU_Load = 100 * (On - Prev(ON))/ (On + Off + Idle + Suspend - Prev(On + Off + Idle + Suspend))
            long on = Long.parseLong(arrayOfString[1].replace(",",""));
            long off = Long.parseLong(arrayOfString[3].replace(",",""));
            long idle = Long.parseLong(arrayOfString[5].replace(",",""));
            long suspend = Long.parseLong(arrayOfString[7].replace(",",""));

            if (!((mGpuPrevOn == 0.0F) && (mGpuPrevTotal == 0.0F))) {
                float value = 100.0F * ((float) on - mGpuPrevOn) / ((float) (on + off + idle + suspend) - mGpuPrevTotal);
                perfDetail.setGpu(value);
            } else {
                perfDetail.setGpu(0.0F);
            }

            mGpuPrevOn = (float) on;
            mGpuPrevTotal = (float) (on + off + idle + suspend);

        } catch (IOException e) {
            Timber.e("failed to update GPU Load ; %s ", e.toString());
        }
    }


    private void updateFps(PerfDetail perfDetail) {
        try {
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(FPS_INFO_PATH))), 1000);
            String str = localBufferedReader.readLine();
            while (! str.contains("user_updates")){
                str = localBufferedReader.readLine();
            }
            localBufferedReader.close();
            String[] arrayOfString = str.split("=");
            // only first plane fps is considered
            perfDetail.setFps(Integer.parseInt(arrayOfString[1].replace("fps","")));
        } catch (IOException e) {
            Timber.e("failed to update FPS ; %s ", e.toString());
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        mPerfDetail = msg.getData().getParcelable(PERF_DETAIL, PerfDetail.class);
        if (mPerfDetail != null) {
            if (mLog) {
                Timber.d("CPU(%%) %.1f, CPU0(%%) %.1f, CPU1(%%) %.1f, GPU(%%) %.1f, FPS %d",
                        mPerfDetail.getCpu(), mPerfDetail.getCpu0(), mPerfDetail.getCpu1(),
                        mPerfDetail.getGpu(), mPerfDetail.getFps());
            }
            mPerfLayout.updateValues(mPerfDetail);
            return true;
        }
        return false;
    }
}
