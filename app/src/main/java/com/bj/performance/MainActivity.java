package com.bj.performance;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.bj.performance.launchstarter.IldeTaskDispatcher;
import com.bj.performance.task.InitBaiduMapTask;
import com.bj.performance.task.InitBuglyTask;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("MainActivity开始执行延迟调用");
        new IldeTaskDispatcher()
                .addTask(new InitBaiduMapTask())
                .addTask(new InitBuglyTask())
                .start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
