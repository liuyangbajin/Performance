package com.bj.performance.aop;

import android.util.Log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @author: lybj
 * @date: 2020/5/8
 * @Description:
 */
@Aspect
public class PerformanceAop {

//    @Around("call(* com.bj.performance.MyApplication.**(..))")
//    public void getTime(ProceedingJoinPoint joinPoint){
//
//        long startTime = System.currentTimeMillis();
//        String methodName = joinPoint.getSignature().getName();
//        try {
//            joinPoint.proceed();
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }
//        Log.d("lybj", methodName + "方法耗时："+ (System.currentTimeMillis() - startTime));
//    }
}
