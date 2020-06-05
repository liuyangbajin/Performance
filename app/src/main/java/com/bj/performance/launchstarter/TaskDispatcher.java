package com.bj.performance.launchstarter;

import android.content.Context;
import android.os.Looper;


import androidx.annotation.UiThread;

import com.bj.performance.launchstarter.sort.TaskSortUtil;
import com.bj.performance.launchstarter.task.DispatchRunnable;
import com.bj.performance.launchstarter.task.Task;
import com.bj.performance.launchstarter.task.TaskCallBack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动器调用类
 */

public class TaskDispatcher {

    // 存放依赖
    private HashMap<Class<? extends Task>, ArrayList<Task>> mDependedHashMap = new HashMap<>();

    // 存放所有的task
    private List<Task> mAllTasks = new ArrayList<>();
    private List<Class<? extends Task>> mClsAllTasks = new ArrayList<>();

    // 已经结束了的Task队列
    private volatile List<Class<? extends Task>> mFinishedTasks = new ArrayList<>(100);

    // 需要在主线程中执行的Task队列
    private volatile List<Task> mMainThreadTasks = new ArrayList<>();

    // 保存需要Wait的Task的数量
    private AtomicInteger mNeedWaitCount = new AtomicInteger();
    private CountDownLatch mCountDownLatch;

    private static final int WAITTIME = 10000;
    private static Context sContext;
    private static boolean sIsMainProcess;
    private List<Future> mFutures = new ArrayList<>();
    private static volatile boolean sHasInit;

    private TaskDispatcher() {
    }

    public static void init(Context context) {
        if (context != null) {
            sContext = context;
            sHasInit = true;
            sIsMainProcess = isMainThread();
        }
    }

    /**
     * 注意：每次获取的都是新对象
     *
     * @return
     */
    public static TaskDispatcher createInstance() {
        if (!sHasInit) {
            throw new RuntimeException("小子，必须滴在主线程初始化");
        }
        return new TaskDispatcher();
    }

    /**
     * 添加任务
     */
    public TaskDispatcher addTask(Task task) {
        if (task != null) {
            collectDepends(task);
            mAllTasks.add(task);
            mClsAllTasks.add(task.getClass());
            // 非主线程且需要wait的，主线程不需要CountDownLatch也是同步的
            if (ifNeedWait(task)) {
                mNeedWaitCount.getAndIncrement();
            }
        }
        return this;
    }

    private void collectDepends(Task task) {
        if (task.dependsOn() != null && task.dependsOn().size() > 0) {
            for (Class<? extends Task> cls : task.dependsOn()) {
                if (mDependedHashMap.get(cls) == null) {
                    mDependedHashMap.put(cls, new ArrayList<Task>());
                }
                mDependedHashMap.get(cls).add(task);
                if (mFinishedTasks.contains(cls)) {
                    task.satisfy();
                }
            }
        }
    }

    private boolean ifNeedWait(Task task) {
        return !task.runOnMainThread() && task.needWait();
    }

    @UiThread
    public void start() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new RuntimeException("小子，启动器必须要在主线程启动");
        }
        if (mAllTasks.size() > 0) {
            mAllTasks = TaskSortUtil.getSortResult(mAllTasks, mClsAllTasks);
            mCountDownLatch = new CountDownLatch(mNeedWaitCount.get());

            dispatchTasks();
            executeTaskMain();
        }
    }

    public void cancel() {
        for (Future future : mFutures) {
            future.cancel(true);
        }
    }

    private void executeTaskMain() {
        for (Task task : mMainThreadTasks) {
            new DispatchRunnable(task,this).run();
        }
    }

    /**
     * task分发，根据设定的不同规则，分发到不同的线程
     */
    private void dispatchTasks() {
        for (final Task task : mAllTasks) {

            // 如果是需要在主线程中运行的，加入到主线程队列中
            if (task.runOnMainThread()) {
                mMainThreadTasks.add(task);

                if (task.needCall()) {
                    task.setTaskCallBack(new TaskCallBack() {
                        @Override
                        public void call() {
                            task.setFinished(true);
                            satisfyChildren(task);
                            markTaskDone(task);
                        }
                    });
                }
            } else {
                // 异步线程中执行，是否执行取决于具体线程池
                Future future = task.runOn().submit(new DispatchRunnable(task, this));
                mFutures.add(future);
            }
        }
    }

    /**
     * 通知Children一个前置任务已完成
     *
     * @param launchTask
     */
    public void satisfyChildren(Task launchTask) {
        ArrayList<Task> arrayList = mDependedHashMap.get(launchTask.getClass());
        if (arrayList != null && arrayList.size() > 0) {
            for (Task task : arrayList) {
                task.satisfy();
            }
        }
    }

    public void markTaskDone(Task task) {
        if (ifNeedWait(task)) {
            mFinishedTasks.add(task.getClass());
            mCountDownLatch.countDown();
            mNeedWaitCount.getAndDecrement();
        }
    }

    public void executeTask(Task task) {
        if (ifNeedWait(task)) {
            mNeedWaitCount.getAndIncrement();
        }
        task.runOn().execute(new DispatchRunnable(task,this));
    }

    @UiThread
    public void await() {
        try {
            if (mNeedWaitCount.get() > 0) {
                mCountDownLatch.await(WAITTIME, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
        }
    }

    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}