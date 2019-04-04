package event

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.Handler

class Metronome: Runnable {

    private var listeners = arrayListOf<MetronomeListener>()
    @Volatile private var adjusting: Boolean = false
    @Volatile private var keepRunning: Boolean = false
    @Volatile var delay: Int = 1000
        protected set
    @Volatile var multiplier: Int = 1
    @Volatile var time: Int = 0
    @Volatile private var lastTick: Long = 0
    private var copy = arrayOf<MetronomeListener?>()
    private var dispatcher = MetronomeTickDispatcher()
    protected var timerThread: Thread? = null
    @Volatile var numberOfListeners = this.listeners.size
        private set
        get() = this.listeners.size
    private val handler: Handler = Handler(Looper.getMainLooper())

    constructor(delay: Int) { this.delay = delay }

    constructor(delay: Int, adjusting: Boolean) {
        this.delay = delay
        this.adjusting = adjusting
    } 

    @Synchronized fun addListener(newListener: MetronomeListener) {
        listeners.add(newListener)
        copyListeners()
    }

    private fun copyListeners() {
        copy = arrayOfNulls<MetronomeListener>(listeners.size)
        listeners.toArray(copy)
    }

    @Synchronized protected fun notifyListeners() {
        dispatcher.setup(copy, time)
        handler.post(dispatcher)
    }

    @Synchronized fun removeListener(ml: MetronomeListener) {
        listeners.remove(ml)
        copyListeners()
    }

    fun reset() { time = 0 }

    override fun run() {

        var currentTick: Long = 0
        var drift: Long = 0

        var currentDelay = delay
        if (adjusting) lastTick = System.currentTimeMillis()

        while (keepRunning) {
            try {
                Thread.sleep(delay as Long)
                time += currentDelay * multiplier
                if (adjusting) {
                    currentTick = System.currentTimeMillis()
                    drift = currentTick - lastTick - currentDelay
                    currentDelay = Math.max(0L, delay - drift) as Int
                    lastTick = currentTick
                }
                notifyListeners()
            } catch(ie: InterruptedException) { }
        }

        timerThread = null
    }

    fun start() {
        if (timerThread == null) {
            keepRunning = true
            timerThread = Thread(this)
            (timerThread as Thread).start()
        }
    }

    fun stop() {
        keepRunning = false
        if (timerThread != null) (timerThread as Thread).interrupt()
    }

    class MetronomeTickDispatcher: Runnable {

        private var listeners = arrayOf<MetronomeListener?>()
        private var time: Int = 0

        override fun run() { for (i in listeners.size - 1 downTo 0) listeners[i]?.handleTick(time) }

        fun setup(listeners: Array<MetronomeListener?>, time: Int) {
            this.listeners = listeners
            this.time = time
        }
    }
}