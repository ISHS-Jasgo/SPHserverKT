package event

class PopulationChangeListener {
    private var onPopulationChangeEventListener: PopulationChangeEvent

    init {
        onPopulationChangeEventListener = object : PopulationChangeEvent {
            override fun onPopulationChange(place: String)
            {
                // Do nothing
            }
        }
    }
    fun setOnPopulationChange(onPopulationChangeEventListener: PopulationChangeEvent) {
        this.onPopulationChangeEventListener = onPopulationChangeEventListener
    }

    fun populationChange(place: String) {
        onPopulationChangeEventListener.onPopulationChange(place)
    }

}