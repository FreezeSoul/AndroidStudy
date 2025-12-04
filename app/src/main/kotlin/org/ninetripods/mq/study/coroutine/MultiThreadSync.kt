package org.ninetripods.mq.study.coroutine

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.ninetripods.mq.study.kotlin.ktx.log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 多线程同步
 */
class MultiThreadSync {
    companion object {
        private const val THREAD_NUM = 5
    }


    private val mScope = CoroutineScope(Dispatchers.Main)
    private val taskChannel = Channel<TaskData>(Channel.UNLIMITED)

    data class TaskData(
        val index: Int,
        val callback: (Int) -> Unit,
    )

    /**
     * 多线程串行处理
     */
    fun main(activity: FragmentActivity) {
        log("开始执行")
        //1、Channel方式
        mScope.launch {
            val channelHelper = EventHelper()

            //尝试接收数据，如果数据为空会挂起
            channelHelper.startProcess()

            //发送数据
            repeat(THREAD_NUM) { index ->
                channelHelper.sendEvent(Event("data$index", index))
            }

            //监听生命周期，页面关闭时停止发送&接收
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    channelHelper.stop()
                }
            })
        }


        repeat(THREAD_NUM) { index ->
            Thread {
                //2、Dispatchers.IO.limitedParallelism(1)方式
                processData { result -> log("线程$index, 回调结果: $result") }

                //3、Mutex加锁方式
                processWithLock { result -> log("线程$index, 回调结果: $result") }
            }.start()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private val mSequentScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1)) //保证线程每次只有一个执行
    private var mCalculateNum = 0

    /**
     * 16:41:38.017  E  开始执行
     * 16:41:38.582  E  线程4, 回调结果: 1
     * 16:41:38.750  E  线程0, 回调结果: 2
     * 16:41:38.779  E  线程2, 回调结果: 3
     * 16:41:39.112  E  线程3, 回调结果: 4
     * 16:41:39.231  E  线程1, 回调结果: 5
     */
    private fun processData(callback: (Int) -> Unit) {
        //方式1
        mSequentScope.launch {
            //执行在子线程：DefaultDispatcher-worker-xxx
            delay((500..1500).random().toLong()) //模拟耗时操作
            val result = ++mCalculateNum

            withContext(Dispatchers.Main) {
                //回调在主线程
                callback.invoke(result)
                if (result == THREAD_NUM) mSequentScope.cancel() //最后取消协程
            }
        }
    }

    private val mSyncLock = Mutex()
    /**
     * 16:47:40.126  E  开始执行
     * 16:47:40.735  E  线程2, 回调结果: 1
     * 16:47:42.034  E  线程1, 回调结果: 2
     * 16:47:43.311  E  线程4, 回调结果: 3
     * 16:47:44.452  E  线程0, 回调结果: 4
     * 16:47:45.576  E  线程3, 回调结果: 5
     */
    private fun processWithLock(callback: (Int) -> Unit) {
        //方式2
        mScope.launch {
            mSyncLock.withLock {
                val result = withContext(Dispatchers.IO) {
                    delay((500..1500).random().toLong()) //模拟耗时操作
                    ++mCalculateNum
                }
                callback.invoke(result)
            }
        }
    }


    private val mChannelScope = CoroutineScope(Dispatchers.IO)

    fun startChannelProcessor() {
        mChannelScope.launch {
            for (task in taskChannel) {
                processTask(task)
            }
        }
    }

    private suspend fun processTask(task: TaskData) {
        delay((500..1500).random().toLong())
        mCalculateNum++

        val result = mCalculateNum
        withContext(Dispatchers.Main) {
            task.callback.invoke(result)

            if (result == THREAD_NUM) {
                taskChannel.close()
            }
        }
    }

    private fun submitTask(index: Int, callback: (Int) -> Unit) {
        mChannelScope.launch {
            taskChannel.send(TaskData(index, callback))
        }
    }
}

/**
 * 使用Channel来处理
 */
class EventHelper {

    /**
     * | 容量类型 | 发送行为 | 接收行为 | 适用场景 |
     * |---------|----------|----------|----------|
     * | **RENDEZVOUS**，默认值0 | 无缓冲区时挂起 | 无数据时挂起 | 严格同步的生产者消费者 |
     * | **CONFLATED** ，值=-1| 永不挂起，覆盖旧值 | 正常接收 | 状态更新，只关心最新值 |
     * | **BUFFERED** ，值=-2| 缓冲区满时挂起 | 正常接收 | 一般事件处理，应对突发 |
     * | **UNLIMITED**，值=Int.MAX_VALUE | 永不挂起 | 正常接收 | 绝对不能丢失数据的场景 |
     * | **固定数值** | 缓冲区满时挂起 | 正常接收 | 需要精确控制内存的场景 |
     */
    private val channel = Channel<Event>(Channel.UNLIMITED)
    private val mChannelScope = CoroutineScope(Dispatchers.IO)
    private val isActive = AtomicBoolean(true)

    fun startProcess() {
        mChannelScope.launch {
            /**
             * Channel实现了ReceiveChannel（提供了iterator方法）接口，而这个接口通过迭代器模式提供了协程化的遍历能力。
             * 迭代器的hasNext()和next()都是挂起函数，能在没有元素时自动挂起协程，for循环会持续从Channel中接收元素，直到Channel被关闭。
             */
            for (event in channel) {
                if (!isActive.get()) break //停止读取

                delay((500..1500).random().toLong()) //模拟在子线程的耗时处理
                withContext(Dispatchers.Main) {
                    //在主线程展示数据
                    if (isActive.get()) {
                        log("接收event: $event")
                    }
                }
            }
        }
    }

    /**
     * Note：这里不能使用trySend。
     *
     * 1、trySend()：非阻塞尝试发送，立即返回结果，成功返回Success，失败返回Closed或队列已满，适用于非关键事件，允许丢失，避免协程挂起。
     * 2、send():挂起发送，如果Channel已满，挂起协程直到有空间。适用于需要确保事件一定被发送，不丢失数据。
     */
    suspend fun sendEvent(event: Event) {
        if (isActive.get()) {
            channel.send(event)
            log("发送event:$event")
        }
    }

    fun stop() {
        isActive.set(false)
        mChannelScope.cancel()
        channel.close()
    }
}

data class Event(val data: String, val index: Int)