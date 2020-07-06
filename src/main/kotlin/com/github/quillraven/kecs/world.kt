package com.github.quillraven.kecs

import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedSet

interface EntityListener {
    fun entityAdded(entityID: Int) = Unit
    fun entityRemoved(entityID: Int) = Unit
}

class World(val initialEntityCapacity: Int) {
    private val entities = IntArray(false, initialEntityCapacity).apply {
        repeat(initialEntityCapacity) {
            add(-1)
        }
    }
    private val freeIDs = IntArray(false, initialEntityCapacity)
    private var nextEntityID = 0
    private val componentManagers = ObjectMap<Class<*>, ComponentManager<*>>()
    private val listeners = OrderedSet<EntityListener>().apply {
        orderedItems().ordered = false
    }
    private val systems = OrderedSet<System>().apply {
        orderedItems().ordered = false
    }
    private val families = OrderedSet<Family>().apply {
        orderedItems().ordered = false
    }
    private val familyBuilder = FamilyBuilder(this, families)

    fun entity(): Int {
        val entity = when {
            freeIDs.isEmpty -> {
                entities[nextEntityID] = nextEntityID++
                nextEntityID - 1
            }
            else -> {
                val id = freeIDs.removeIndex(freeIDs.size - 1)
                entities[id] = id
                id
            }
        }
        listeners.forEach { it.entityAdded(entity) }
        return entity
    }

    fun removeEntity(entityID: Int) {
        listeners.forEach { it.entityRemoved(entityID) }
        freeIDs.add(entityID)
        entities[entityID] = -1
    }

    operator fun contains(entityID: Int) = entities[entityID] != -1

    inline fun <reified T> componentManager(): ComponentManager<T> = componentManager(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T> componentManager(type: Class<T>): ComponentManager<T> {
        return when {
            componentManagers.containsKey(type) -> {
                componentManagers[type] as ComponentManager<T>
            }
            else -> {
                val manager = ComponentManager(initialEntityCapacity, type)
                addListener(manager)
                componentManagers.put(type, manager)
                manager
            }
        }
    }

    fun addListener(listener: EntityListener) = listeners.add(listener)

    fun removeListener(listener: EntityListener) = listeners.remove(listener)

    operator fun contains(listener: EntityListener) = listeners.contains(listener)

    fun family(init: FamilyBuilder.() -> (Unit)) = familyBuilder.apply(init).build()

    operator fun contains(family: Family) = families.contains(family)

    fun systems(vararg system: System) {
        system.forEach { systems.add(it) }
    }

    fun update(deltaTime: Float) {
        systems.forEach { system ->
            if (system.active) {
                system.update(this, deltaTime)
            }
        }
    }
}
