package com.google.sample.oboe.manualtest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Run an automated test from a UI, gather logs,
 * and display a summary.
 */
public  class AutomatedTestRunner extends LinearLayout implements Runnable {

    private Button       mStartButton;
    private Button       mStopButton;
    private Button       mShareButton;
    private TextView     mAutoTextView;
    private TestAudioActivity  mActivity;
    private StringBuffer mFailedSummary;
    private int          mTestCount;
    private int          mPassCount;
    private int          mFailCount;

    private Thread           mAutoThread;
    private volatile boolean mThreadEnabled;

    public AutomatedTestRunner(Context context) {
        super(context);
        initializeViews(context);
    }

    public AutomatedTestRunner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public AutomatedTestRunner(Context context,
                               AttributeSet attrs,
                               int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    public TestAudioActivity getActivity() {
        return mActivity;
    }

    public void setActivity(TestAudioActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context
     *           the current context for the view.
     */
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.auto_test_runner, this);

        mStartButton = (Button) findViewById(R.id.button_start);
        mStartButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutoThread();
            }
        });

        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAutoThread();
            }
        });

        mShareButton = (Button) findViewById(R.id.button_share);
        mShareButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResult();
            }
        });
        mShareButton.setEnabled(false);

        mAutoTextView = (TextView) findViewById(R.id.text_log);
        mAutoTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void updateStartStopButtons(boolean running) {
        mStartButton.setEnabled(!running);
        mStopButton.setEnabled(running);
    }

    public int getTestCount() {
        return mTestCount;
    }

    public boolean isThreadEnabled() {
        return mThreadEnabled;
    }

    public void appendSummary(String text) {
        mFailedSummary.append(text);
    }

    public void incrementFailCount() {
        mFailCount++;
    }
    public void incrementPassCount() {
        mPassCount++;
    }
    public void incrementTestCount() {
        mTestCount++;
    }

    // Write to scrollable TextView
    public void log(final String text) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAutoTextView.append(text);
                mAutoTextView.append("\n");
            }
        });
    }

    private void logClear() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAutoTextView.setText("");
            }
        });
    }

    public void startAutoThread() {
        updateStartStopButtons(true);
        mThreadEnabled = true;
        mAutoThread = new Thread(this);
        mAutoThread.start();
    }

    // Only call from UI thread.
    public void onTestFinished() {
        updateStartStopButtons(false);
        mShareButton.setEnabled(true);
    }

    public void stopAudioTest() {
        mActivity.stopAudio();
    }

    public void stopAutoThread() {
        try {
            if (mAutoThread != null) {
                mThreadEnabled = false;
                mAutoThread.interrupt();
                mAutoThread.join(100);
                mAutoThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getTimestampString() {
        DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date now = Calendar.getInstance().getTime();
        return df.format(now);
    }

    // Share text from log via GMail, Drive or other method.
    public void shareResult() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String subjectText = "OboeTester AutoGlitch result " + getTimestampString();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subjectText);

        String shareBody = mAutoTextView.getText().toString();
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

        mActivity.startActivity(Intent.createChooser(sharingIntent, "Share using:"));
    }

    @Override
    public void run() {
        logClear();
        log("=== STARTED at " + new Date());
        log(Build.MANUFACTURER + " " + Build.PRODUCT);
        log(Build.DISPLAY);
        mFailedSummary = new StringBuffer();
        mTestCount = 0;
        mPassCount = 0;
        mFailCount = 0;
        try {
            mActivity.runTest();
        } finally {
            stopAudioTest();
            if (mThreadEnabled) {
                log("\n==== SUMMARY ========");
                if (mFailCount > 0) {
                    log(mPassCount + " passed. " + mFailCount + " failed.");
                    log("These tests FAILED:");
                    log(mFailedSummary.toString());
                } else {
                    log("All tests PASSED.");
                }
                log("== FINISHED at " + new Date());
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onTestFinished();
                    }
                });
            }
        }
    }

}
