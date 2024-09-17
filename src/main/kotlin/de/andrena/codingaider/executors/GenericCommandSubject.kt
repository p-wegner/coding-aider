package de.andrena.codingaider.executors

class GenericCommandSubject : CommandSubject {
    private val observers = mutableListOf<CommandObserver>()

    override fun addObserver(observer: CommandObserver) = observers.add(observer)
    override fun removeObserver(observer: CommandObserver) = observers.remove(observer)
    override suspend fun notifyObservers(event: suspend (CommandObserver) -> Unit) {
        observers.forEach { event(it) }
    }
}
