package org.ninetripods.mq.study.coroutine

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ninetripods.mq.study.kotlin.ktx.log

/**
 * try-catch 不能抓到协程异常问题
 */
class CoroutineEx {

    /**
     * 问题：try/catch不能抓到Coroutine内部的异常
     */
    fun question(activity: FragmentActivity) {
        try {
            activity.lifecycleScope.launch {
                //NOTE：这里可能被调度到将来某个时间点执行
                withContext(Dispatchers.IO) {
                    //模拟在协程中抛异常
                    throw IllegalStateException("Exception Occur")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //这里只会捕获函数本身的异常
            log("捕获Exception:$e")
        }
    }

    /**
     * 方式1：try/catch放到协程内部
     */
    fun solution1(activity: FragmentActivity) {
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    //模拟在协程中抛异常
                    throw IllegalStateException("Exception Occur")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //这里只会捕获launch函数本身的异常
                log("捕获Exception:$e")
            }
        }
    }

    /**
     * 方式2：CoroutineExceptionHandler处理异常
     */
    fun solution2(activity: FragmentActivity) {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            log("捕获Exception2:$throwable")
        }
        activity.lifecycleScope.launch(exceptionHandler) {
            //NOTE：这里可能被调度到将来某个时间点执行
            withContext(Dispatchers.IO) {
                //模拟在协程中抛异常
                throw IllegalStateException("Exception Occur")
            }
        }
    }
}