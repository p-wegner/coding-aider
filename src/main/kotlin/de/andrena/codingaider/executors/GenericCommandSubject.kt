package de.andrena.codingaider.executors

class GenericCommandSubject : CommandSubject {
    private val observers = mutableListOf<CommandObserver>()

    override fun addObserver(observer: CommandObserver) = observers.add(observer)
    override fun removeObserver(observer: CommandObserver) = observers.remove(observer)
    override fun notifyObservers(event: (CommandObserver) -> Unit) = observers.forEach(event)
}
