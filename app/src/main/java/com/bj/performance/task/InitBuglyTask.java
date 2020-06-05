package com.bj.performance.task;

import com.bj.performance.launchstarter.task.Task;

/**
 * @author: lybj
 * @date: 2020/5/13
 * @Description:
 */
public class InitBuglyTask extends Task {

    @Override
    public boolean runOnMainThread() {
        return true;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            System.out.println("InitBuglyTask运行完毕，它所在的线程是："+Thread.currentThread().getName());
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
