package com.bj.performance.alpha;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.UiThread;

import com.bj.performance.alpha.sort.TaskSortUtil;
import com.bj.performance.alpha.task.TaskRunnable;
import com.bj.performance.alpha.task.ITask;
import com.bj.performance.alpha.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task调度类（调度分发Task）
 */
public class TaskManager {

    private static TaskManager sInstance;
    private Context mContext;

    // 维持task和它的依赖Task的依赖关系，这里是仿照EventBus的存放事件的机制设计
    private HashMap<Class<? extends ITask>, ArrayList<ITask>> dependOfTaskArray = new HashMap<>();

    // 存放已经执行完毕的Task队列
    private volatile List<Class<? extends ITask>> taskFinishedArray = new ArrayList<>();

    // 存放所有的task
    private List<Task> taskAll = new ArrayList<>();
    private List<Class<? extends Task>> taskAllClazz = new ArrayList<>();

    // 需要在主线程中执行的Task队列
    private volatile List<Task> mainThreadTaskArray = new ArrayList<>();

    // 主线程需要等待先执行的task数量
    private AtomicInteger mainNeedWaitCount = new AtomicInteger();
    private CountDownLatch mCountDownLatch;

    private static final int WAITTIME_TIME = 996 * 31;
    private List<Future> futureArray = new ArrayList<>();

    private TaskManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null.");
        }
        mContext = context;
    }

    /**
     * 使用单例模式创建对象
     */
    public static TaskManager getInstance(Context context) {

        if (sInstance == null) {
            sInstance = new TaskManager(context);
        }
        return sInstance;
    }

    /**
     * 添加任务
     */
    public TaskManager add(Task task) {

        if (task == null) {
            throw new IllegalArgumentException("task is null !");
        }
        setDependentOfTask(task);
        taskAll.add(task);
        taskAllClazz.add(task.getClass());

        // 非主线程且需要wait的
        if (ifNeedWait(task)) {
            // 主线程的锁加一把
            mainNeedWaitCount.getAndIncrement();
        }
        return this;
    }

    /**
     * 获取依赖的集合，主要做的为两件事
     *
     *  1.是以依赖类为Key，
     *  2.在从完成的任务集合里面查询，该task所依赖的类是否已经完成，完成的话进行解锁
     * */
    private void setDependentOfTask(Task task) {
        if (task.dependentArr() != null && task.dependentArr().size() > 0) {
            for (Class<? extends ITask> dependTaskClazz : task.dependentArr()) {
                if (dependOfTaskArray.get(dependTaskClazz) == null) {
                    dependOfTaskArray.put(dependTaskClazz, new ArrayList<ITask>());
                }

                // 如果该task所依赖的依赖任务已经加载过了，就解锁其中已经完成的
                dependOfTaskArray.get(dependTaskClazz).add(task);
                if (taskFinishedArray.contains(dependTaskClazz)) {
                    task.unlock();
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
        if (taskAll.size() > 0) {

            // 效率排序
            taskAll = TaskSortUtil.getSortResult(taskAll, taskAllClazz);
            mCountDownLatch = new CountDownLatch(mainNeedWaitCount.get());

            // 分发任务
            dispatchTasks();
            runOnMainThread();
        }
    }

    /**
     * task分发，根据设定的不同规则，分发到不同的线程
     */
    private void dispatchTasks() {
        for (final Task task : taskAll) {
            // 如果是需要在主线程中运行的，加入到主线程队列中
            if (task.runOnMainThread()) {
                mainThreadTaskArray.add(task);
            } else {
                // 异步线程中执行，是否执行取决于具体线程池
                Future future = task.runOnExecutor().submit(new TaskRunnable(task, this));
                futureArray.add(future);
            }
        }
    }

    private void runOnMainThread() {
        for (Task task : mainThreadTaskArray) {
            new TaskRunnable(task,this).run();
        }
    }

    @UiThread
    public void startLock() {
        try {
            if (mainNeedWaitCount.get() > 0) {
                mCountDownLatch.await(WAITTIME_TIME, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * 取消
     * */
    public void cancel() {
        for (Future future : futureArray) {
            future.cancel(true);
        }
    }

    /**
     * 当完成一个任务之后，通知所有依赖它的任务，并解锁他们
     */
    public void unLockForChildren(Task task) {
        ArrayList<ITask> arrayList = dependOfTaskArray.get(task.getClass());
        if (arrayList != null && arrayList.size() > 0) {
            for (ITask subTask : arrayList) {
                subTask.unlock();
            }
        }
    }

    public void finish(Task task) {
        if (ifNeedWait(task)) {
            taskFinishedArray.add(task.getClass());
            mCountDownLatch.countDown();
            mainNeedWaitCount.getAndDecrement();
        }
    }
}