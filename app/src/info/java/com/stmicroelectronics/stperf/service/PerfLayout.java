package com.stmicroelectronics.stperf.service;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.stmicroelectronics.stperf.R;
import com.stmicroelectronics.stperf.data.PerfDetail;

import java.util.Locale;

public class PerfLayout {

    private final static int BAR_DATA_MAX = 20;

    private final Context mContext;

    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mTopLayoutParams;
    private final LayoutInflater mLayoutInflater;

    private LinearLayout mTopLayout;

    private boolean mGraph;
    private boolean mPos;

    private TextView mCpuInfo;
    private LinearLayout mCpu0Layout;
    private TextView mCpu0Title;
    private TextView mCpu0View;
    private LinearLayout mCpu1Layout;
    private TextView mCpu1Title;
    private TextView mCpu1View;
    private LinearLayout mGpuLayout;
    private TextView mGpuTitle;
    private TextView mGpuView;
    private LinearLayout mFpsLayout;
    private TextView mFpsTitle;
    private TextView mFpsView;

    private GraphView mCpu0Graph;
    private BarGraphSeries<DataPoint> mCpu0Series;
    private BarGraphSeries<DataPoint> mCpuSeries;
    private GraphView mCpu1Graph;
    private BarGraphSeries<DataPoint> mCpu1Series;
    private GraphView mGpuGraph;
    private BarGraphSeries<DataPoint> mGpuSeries;
    private GraphView mFpsGraph;
    private BarGraphSeries<DataPoint> mFpsSeries;

    private double mGraphIndex;
    private boolean mCpuAvg;

    private final Object key = new Object();

    PerfLayout(Context context, boolean graph, boolean pos) {
        mContext = context;
        mGraph = graph;
        mPos = pos;
        mGraphIndex = 0;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mGraph) {
            mTopLayout = initLayoutGraph();
        } else {
            mTopLayout = initLayoutNoGraph();
        }
        mTopLayoutParams = initLayoutParams(mPos);
        initGraphs();

        assert mWindowManager != null;
        mWindowManager.addView(mTopLayout, mTopLayoutParams);
    }

    void stopLayout() {
        mWindowManager.removeView(mTopLayout);
    }

    void setBackColor(boolean color) {
        int backColor;
        if (color) {
            backColor = mContext.getResources().getColor(R.color.colorPerfBack,null);
        } else {
            backColor = Color.argb(0,0,0,0);
        }
        if (mTopLayout != null) {
            mTopLayout.setBackgroundColor(backColor);
        }
    }

    private WindowManager.LayoutParams initLayoutParams(boolean pos) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        layoutParams.alpha = 0.8f; // required for a not touchable area
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 0;
        layoutParams.y = 0;
        if (pos) {
            layoutParams.gravity = Gravity.CENTER | Gravity.TOP;
        } else {
            layoutParams.gravity = Gravity.CENTER | Gravity.BOTTOM;
        }
        return layoutParams;
    }

    private LinearLayout initLayoutGraph() {
        LinearLayout view = (LinearLayout) mLayoutInflater.inflate(R.layout.service_perf,null);
        mCpuInfo = view.findViewById(R.id.cpu_info);
        mCpu0Layout = view.findViewById(R.id.cpu0_layout);
        mCpu0Title = view.findViewById(R.id.cpu0_title);
        mCpu0View = view.findViewById(R.id.cpu0_value);
        mCpu0Graph = view.findViewById(R.id.cpu0_graph);
        mCpu1Layout = view.findViewById(R.id.cpu1_layout);
        mCpu1Title = view.findViewById(R.id.cpu1_title);
        mCpu1View = view.findViewById(R.id.cpu1_value);
        mCpu1Graph = view.findViewById(R.id.cpu1_graph);
        mGpuLayout = view.findViewById(R.id.gpu_layout);
        mGpuTitle = view.findViewById(R.id.gpu_title);
        mGpuView = view.findViewById(R.id.gpu_value);
        mGpuGraph = view.findViewById(R.id.gpu_graph);
        mFpsLayout = view.findViewById(R.id.fps_layout);
        mFpsTitle = view.findViewById(R.id.fps_title);
        mFpsView = view.findViewById(R.id.fps_value);
        mFpsGraph = view.findViewById(R.id.fps_graph);
        return view;
    }

    private LinearLayout initLayoutNoGraph() {
        LinearLayout view = (LinearLayout) mLayoutInflater.inflate(R.layout.service_perf_nograph,null);
        mCpuInfo = view.findViewById(R.id.cpu_info);
        mCpu0Title = view.findViewById(R.id.cpu0_title);
        mCpu0View = view.findViewById(R.id.cpu0_value);
        mCpu1Title = view.findViewById(R.id.cpu1_title);
        mCpu1View = view.findViewById(R.id.cpu1_value);
        mGpuTitle = view.findViewById(R.id.gpu_title);
        mGpuView = view.findViewById(R.id.gpu_value);
        mFpsTitle = view.findViewById(R.id.fps_title);
        mFpsView = view.findViewById(R.id.fps_value);
        return view;
    }

    private void initGraph(GraphView graph, double maxY) {
        // remove grid
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(mContext.getResources().getColor(R.color.colorPrimaryDark, null));
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(mContext.getResources().getColor(R.color.colorPrimaryDark, null));

        // remove labels, set min/max values for X and Y
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setDrawBorder(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(BAR_DATA_MAX);
    }

    private BarGraphSeries<DataPoint> initSeries(ValueDependentColor<DataPoint> color) {
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
        series.setValueDependentColor(color);
        return series;
    }

    private void initGraphs() {
        // initialize bar graphs
        if (mGraph) {
            initGraph(mCpu0Graph, PerfDetail.BAR_MAX_PERCENT);
            initGraph(mCpu1Graph, PerfDetail.BAR_MAX_PERCENT);
            initGraph(mGpuGraph, PerfDetail.BAR_MAX_PERCENT);
            initGraph(mFpsGraph, PerfDetail.BAR_MAX_FPS);
        }

        // initialize series
        ValueDependentColor<DataPoint> colorPercent = data -> Color.rgb((int) data.getY() * 255/PerfDetail.BAR_MAX_PERCENT, (int) Math.abs(255 - (data.getY() * 255/PerfDetail.BAR_MAX_PERCENT)), 100);
        mCpuSeries = initSeries(colorPercent);
        mCpu0Series = initSeries(colorPercent);
        mCpu1Series = initSeries(colorPercent);
        mGpuSeries = initSeries(colorPercent);
        ValueDependentColor<DataPoint> colorFps = data -> Color.rgb((int) data.getY() * 255/PerfDetail.BAR_MAX_FPS, (int) Math.abs(255 - (data.getY() * 255/PerfDetail.BAR_MAX_FPS)), 100);
        mFpsSeries = initSeries(colorFps);
    }

    void initLayoutVisibility(boolean cpu, boolean cpu_avg, boolean gpu, boolean fps) {
        if (cpu) {
            if (cpu_avg) {
                mCpu0Title.setText(mContext.getString(R.string.cpu_label));
                mCpu0Title.setVisibility(View.VISIBLE);
                mCpu0View.setVisibility(View.VISIBLE);
                if (mGraph) {
                    mCpu0Graph.addSeries(mCpuSeries);
                    mCpu0Graph.setVisibility(View.VISIBLE);
                    mCpu0Layout.setVisibility(View.VISIBLE);
                    mCpu1Layout.setVisibility(View.GONE);
                } else {
                    mCpu1Title.setVisibility(View.GONE);
                    mCpu1View.setVisibility(View.GONE);
                }
            } else {
                mCpu0Title.setText(mContext.getString(R.string.cpu0_label));
                mCpu0Title.setVisibility(View.VISIBLE);
                mCpu0View.setVisibility(View.VISIBLE);
                mCpu1Title.setVisibility(View.VISIBLE);
                mCpu1View.setVisibility(View.VISIBLE);
                if (mGraph) {
                    mCpu0Graph.addSeries(mCpu0Series);
                    mCpu0Graph.setVisibility(View.VISIBLE);
                    mCpu0Layout.setVisibility(View.VISIBLE);
                    mCpu1Graph.addSeries(mCpu1Series);
                    mCpu1Graph.setVisibility(View.VISIBLE);
                    mCpu1Layout.setVisibility(View.VISIBLE);
                }
            }
            mCpuAvg = cpu_avg;
        } else {
            if (mGraph) {
                mCpu0Layout.setVisibility(View.GONE);
                mCpu1Layout.setVisibility(View.GONE);
            } else {
                mCpu0Title.setVisibility(View.GONE);
                mCpu0View.setVisibility(View.GONE);
                mCpu1Title.setVisibility(View.GONE);
                mCpu1View.setVisibility(View.GONE);
            }
        }
        if (gpu) {
            mGpuTitle.setVisibility(View.VISIBLE);
            mGpuView.setVisibility(View.VISIBLE);
            if (mGraph) {
                mGpuGraph.addSeries(mGpuSeries);
                mGpuGraph.setVisibility(View.VISIBLE);
                mGpuLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (mGraph) {
                mGpuLayout.setVisibility(View.GONE);
            } else {
                mGpuTitle.setVisibility(View.GONE);
                mGpuView.setVisibility(View.GONE);
            }
        }
        if (fps) {
            mFpsTitle.setVisibility(View.VISIBLE);
            mFpsView.setVisibility(View.VISIBLE);
            if (mGraph) {
                mFpsGraph.addSeries(mFpsSeries);
                mFpsGraph.setVisibility(View.VISIBLE);
                mFpsLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (mGraph) {
                mFpsLayout.setVisibility(View.GONE);
            } else {
                mFpsTitle.setVisibility(View.GONE);
                mFpsView.setVisibility(View.GONE);
            }
        }
    }

    void updateLayoutVisibility(boolean cpu, boolean cpu_avg, boolean gpu, boolean fps, boolean graph, boolean pos) {
        synchronized(key) {
            if ((graph != mGraph) || (pos != mPos)) {
                mGraph = graph;
                mPos = pos;
                mWindowManager.removeView(mTopLayout);

                if (mGraph) {
                    mTopLayout = initLayoutGraph();
                } else {
                    mTopLayout = initLayoutNoGraph();
                }
                if (mPos) {
                    mTopLayoutParams.gravity = Gravity.CENTER | Gravity.TOP;
                } else {
                    mTopLayoutParams.gravity = Gravity.CENTER | Gravity.BOTTOM;
                }
                if (mGraph) {
                    initGraph(mCpu0Graph, PerfDetail.BAR_MAX_PERCENT);
                    initGraph(mCpu1Graph, PerfDetail.BAR_MAX_PERCENT);
                    initGraph(mGpuGraph, PerfDetail.BAR_MAX_PERCENT);
                    initGraph(mFpsGraph, PerfDetail.BAR_MAX_FPS);
                }
                mWindowManager.addView(mTopLayout, mTopLayoutParams);
            }
            if (cpu) {
                if (cpu_avg) {
                    mCpu0Title.setText(mContext.getString(R.string.cpu_label));
                    mCpu0Title.setVisibility(View.VISIBLE);
                    mCpu0View.setVisibility(View.VISIBLE);
                    if (mGraph) {
                        mCpu0Graph.removeAllSeries();
                        mCpu0Graph.addSeries(mCpuSeries);
                        mCpu0Graph.setVisibility(View.VISIBLE);
                        mCpu0Layout.setVisibility(View.VISIBLE);
                        mCpu1Graph.removeAllSeries();
                        mCpu1Layout.setVisibility(View.GONE);
                    } else {
                        mCpu1Title.setVisibility(View.GONE);
                        mCpu1View.setVisibility(View.GONE);
                    }
                } else {
                    mCpu0Title.setText(mContext.getString(R.string.cpu0_label));
                    mCpu0Title.setVisibility(View.VISIBLE);
                    mCpu0View.setVisibility(View.VISIBLE);
                    mCpu1Title.setVisibility(View.VISIBLE);
                    mCpu1View.setVisibility(View.VISIBLE);
                    if (mGraph) {
                        mCpu0Graph.removeAllSeries();
                        mCpu0Graph.addSeries(mCpu0Series);
                        mCpu0Graph.setVisibility(View.VISIBLE);
                        mCpu0Layout.setVisibility(View.VISIBLE);
                        mCpu1Graph.removeAllSeries();
                        mCpu1Graph.addSeries(mCpu1Series);
                        mCpu1Graph.setVisibility(View.VISIBLE);
                        mCpu1Layout.setVisibility(View.VISIBLE);
                    }
                }
                mCpuAvg = cpu_avg;
            } else {
                if (mGraph) {
                    mCpu0Layout.setVisibility(View.GONE);
                    mCpu1Layout.setVisibility(View.GONE);
                } else {
                    mCpu0Title.setVisibility(View.GONE);
                    mCpu0View.setVisibility(View.GONE);
                    mCpu1Title.setVisibility(View.GONE);
                    mCpu1View.setVisibility(View.GONE);
                }
            }
            if (gpu) {
                mGpuTitle.setVisibility(View.VISIBLE);
                mGpuView.setVisibility(View.VISIBLE);
                if (mGraph) {
                    mGpuGraph.removeAllSeries();
                    mGpuGraph.addSeries(mGpuSeries);
                    mGpuGraph.setVisibility(View.VISIBLE);
                    mGpuLayout.setVisibility(View.VISIBLE);
                }
            } else {
                if (mGraph) {
                    mGpuLayout.setVisibility(View.GONE);
                } else {
                    mGpuTitle.setVisibility(View.GONE);
                    mGpuView.setVisibility(View.GONE);
                }
            }
            if (fps) {
                mFpsTitle.setVisibility(View.VISIBLE);
                mFpsView.setVisibility(View.VISIBLE);
                if (mGraph) {
                    mFpsGraph.removeAllSeries();
                    mFpsGraph.addSeries(mFpsSeries);
                    mFpsGraph.setVisibility(View.VISIBLE);
                    mFpsLayout.setVisibility(View.VISIBLE);
                }
            } else {
                if (mGraph) {
                    mFpsLayout.setVisibility(View.GONE);
                } else {
                    mFpsTitle.setVisibility(View.GONE);
                    mFpsView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void updateValues(PerfDetail perfDetail) {
        synchronized (key) {
            if (mCpuAvg) {
                mCpu0View.setText(String.format(Locale.US, "%.1f%%", perfDetail.getCpu()));
            } else {
                mCpu0View.setText(String.format(Locale.US, "%.1f%%", perfDetail.getCpu0()));
                mCpu1View.setText(String.format(Locale.US, "%.1f%%", perfDetail.getCpu1()));
            }
            mGpuView.setText(String.format(Locale.US, "%.1f%%", perfDetail.getGpu()));
            mFpsView.setText(String.format(Locale.US, "%dfps", perfDetail.getFps()));
            mCpuInfo.setText(perfDetail.getCpuInfo());

            updateGraphs(perfDetail);
        }
    }

    private void updateGraphs(PerfDetail perfDetail) {
        mCpuSeries.appendData(new DataPoint(mGraphIndex, Math.round(perfDetail.getCpu())),true, BAR_DATA_MAX);
        mCpu0Series.appendData(new DataPoint(mGraphIndex, Math.round(perfDetail.getCpu0())),true, BAR_DATA_MAX);
        mCpu1Series.appendData(new DataPoint(mGraphIndex, Math.round(perfDetail.getCpu1())),true, BAR_DATA_MAX);
        mGpuSeries.appendData(new DataPoint(mGraphIndex, Math.round(perfDetail.getGpu())),true, BAR_DATA_MAX);
        mFpsSeries.appendData(new DataPoint(mGraphIndex, perfDetail.getFps()),true, BAR_DATA_MAX);
        mGraphIndex+=1;
    }
}
