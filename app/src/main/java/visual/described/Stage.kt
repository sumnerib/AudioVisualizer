package visual.described

import event.MetronomeListener
import event.Metronome
import visual.Visualization

open class Stage(timeStep: Int, metronome: Metronome) : Visualization(), MetronomeListener {

    private var shouldRestart = false
    private var timeStep = timeStep
    private var restartTime = -1
    var metronome = metronome
        private set

    init { metronome.addListener(this) }

    constructor(timeStep: Int): this(timeStep, Metronome(timeStep))

    fun add(sprite: Sprite) {
        metronome.addListener(sprite)
        super.add(sprite)
    }

    override fun handleTick(millis: Int) {
        if (shouldRestart && (millis > restartTime)) 
            metronome.time = -timeStep
        
        repaint()
    }

    fun remove(sprite: Sprite) {
        metronome.removeListener(sprite)
        super.remove(sprite)
    }

    fun setRestartTime(restartTime: Int) {
        
        if (restartTime < 0) {
            this.restartTime = -1
            shouldRestart = false
        } else {
            this.restartTime = restartTime
            shouldRestart = true
        }
    }

    fun start() { metronome.start() }
    fun stop() { metronome.stop() }
}
