package com.bj.performance.alpha.task;

import android.os.Process;
import androidx.core.os.TraceCompat;
import com.bj.performance.alpha.TaskManager;

/**
 * 任务真正执行的地方
 */
public class TaskRunnable implements Runnable {
    private Task task;
    private TaskManager taskManager;

    public TaskRunnable(Task task) {
        this.task = task;
    }

    public TaskRunnable(Task task, TaskManager taskManager) {
        this.task = task;
        this.taskManager = taskManager;
    }

    @Override
    public void run() {
        TraceCompat.beginSection(task.getClass().getSimpleName());
        Process.setThreadPriority(task.priority());

        task.startLock();
        task.run();

        // 执行Task的尾部任务
        Runnable tailRunnable = task.getTailRunnable();
        if (tailRunnable != null) {
            tailRunnable.run();
        }

        if (!task.runOnMainThread()) {
            if(taskManager != null){
                taskManager.unLockForChildren(task);
                taskManager.finish(task);
            }
        }
        TraceCompat.endSection();
    }
}
