package event

interface MetronomeListener {

    fun handleTick(millis: Int)
}