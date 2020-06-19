package com.bj.performance.task;

import com.bj.performance.alpha.task.Task;

/**
 * @author: lybj
 * @date: 2020/5/13
 * @Description:
 */
public class InitShareTask extends Task {

    @Override
    public void run() {
        try {
            Thread.sleep(3000);
            System.out.println("InitShareTask运行完毕，它所在的线程是："+Thread.currentThread().getName());
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
