package com.bj.performance.launchstarter.task;

import android.os.Process;

import androidx.core.os.TraceCompat;

import com.bj.performance.launchstarter.TaskDispatcher;

/**
 * 任务真正执行的地方
 */

public class DispatchRunnable implements Runnable {
    private Task mTask;
    private TaskDispatcher mTaskDispatcher;

    public DispatchRunnable(Task task) {
        this.mTask = task;
    }
    public DispatchRunnable(Task task,TaskDispatcher dispatcher) {
        this.mTask = task;
        this.mTaskDispatcher = dispatcher;
    }

    @Override
    public void run() {
        TraceCompat.beginSection(mTask.getClass().getSimpleName());
        Process.setThreadPriority(mTask.priority());

        mTask.setWaiting(true);
        mTask.waitToSatisfy();

        // 执行Task
        mTask.setRunning(true);
        mTask.run();

        // 执行Task的尾部任务
        Runnable tailRunnable = mTask.getTailRunnable();
        if (tailRunnable != null) {
            tailRunnable.run();
        }

        if (!mTask.needCall() || !mTask.runOnMainThread()) {
            mTask.setFinished(true);
            if(mTaskDispatcher != null){
                mTaskDispatcher.satisfyChildren(mTask);
                mTaskDispatcher.markTaskDone(mTask);
            }
        }
        TraceCompat.endSection();
    }
}
