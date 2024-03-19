package event

class FirstLoadEndListener {
    private var onFirstLoadEndEventListener: FirstLoadEndEvent

    init {
        onFirstLoadEndEventListener = object : FirstLoadEndEvent {
            override fun onFirstLoadEnd()
            {
                // Do nothing
            }
        }
    }
    fun setOnFirstLoadEnd(onFirstLoadEndEventListener: FirstLoadEndEvent) {
        this.onFirstLoadEndEventListener = onFirstLoadEndEventListener
    }

    fun firstLoadEnd() {
        onFirstLoadEndEventListener.onFirstLoadEnd()
    }
}