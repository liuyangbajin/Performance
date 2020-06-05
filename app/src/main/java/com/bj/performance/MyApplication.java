package com.bj.performance;

import android.app.Application;
import android.os.Debug;

import com.bj.performance.launchstarter.TaskDispatcher;
import com.bj.performance.task.InitBaiduMapTask;
import com.bj.performance.task.InitBuglyTask;
import com.bj.performance.task.InitJPushTask;
import com.bj.performance.task.InitShareTask;

/**
 * @author: lybj
 * @date: 2020/5/7
 * @Description:
 */
public class MyApplication extends Application {

    //获得当前CPU的核心数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    //设置线程池的核心线程数2-4之间,但是取决于CPU核数
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));

    @Override
    public void onCreate() {
        super.onCreate();

        // 常规方式
        Debug.startMethodTracing("MyApplication");
//        final CountDownLatch latch = new CountDownLatch(1);
//        ExecutorService executorService = Executors.newFixedThreadPool(CORE_POOL_SIZE);
//
//        executorService.submit(new Runnable() {
//
//            @Override
//            public void run() {
//
//                initBugly();
//                latch.countDown();
//            }
//        });
//
//        executorService.submit(new Runnable() {
//
//            @Override
//            public void run() {
//
//                initBaiduMap();
//            }
//        });
//
//        executorService.submit(new Runnable() {
//
//            @Override
//            public void run() {
//
//                initJPushInterface();
//            }
//        });
//
//        executorService.submit(new Runnable() {
//
//            @Override
//            public void run() {
//
//                initShareSDK();
//            }
//        });
//
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//            Debug.stopMethodTracing();

        // 使用启动器的方式
        System.out.println("MyApplication开始执行");
        TaskDispatcher.init(this);
        TaskDispatcher instance = TaskDispatcher.createInstance();
        instance.addTask(new InitBuglyTask()) // 默认添加，并发处理
                .addTask(new InitBaiduMapTask())  // 在这里需要先处理了另外一个耗时任务initShareSDK，才能再处理它
                .addTask(new InitJPushTask())  // 等待主线程处理完毕，再进行执行
                .addTask(new InitShareTask())
                .start();
        instance.await();
        System.out.println("MyApplication执行完毕");
    }
}
