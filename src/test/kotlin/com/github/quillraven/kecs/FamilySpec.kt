package com.github.quillraven.kecs

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.github.quillraven.kecs.component.PhysiqueComponent
import com.github.quillraven.kecs.component.PlayerComponent
import com.github.quillraven.kecs.component.RenderComponent
import com.github.quillraven.kecs.component.TransformComponent
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

@Suppress("unused")
object FamilySpec : Spek({
    val familyCache by memoized { ObjectSet<Family>() }
    val entityComponentsCache by memoized { Array<BitSet>() }
    val world by memoized { World(3) }

    describe("A FamilyBuilder") {
        describe("Creating a new family") {
            lateinit var family: Family
            beforeEachTest {
                FamilyBuilder(world, entityComponentsCache, familyCache).run {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                    family = build()
                }
            }

            it("should create a new family for the cache") {
                familyCache.contains(family) `should be equal to` true
            }

            it("should add the family as component listener to its component managers") {
                (family in world.componentManager<TransformComponent>()) `should be equal to` true
                (family in world.componentManager<RenderComponent>()) `should be equal to` true
                (family in world.componentManager<PlayerComponent>()) `should be equal to` true
                (family in world.componentManager<PhysiqueComponent>()) `should be equal to` true
            }
        }

        describe("Creating a family twice") {
            lateinit var family1: Family
            lateinit var family2: Family
            beforeEachTest {
                FamilyBuilder(world, entityComponentsCache, familyCache).run {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                    family1 = build()
                }
                FamilyBuilder(world, entityComponentsCache, familyCache).run {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                    family2 = build()
                }
            }

            it("should return the already existing instance") {
                family1 `should be` family2
            }
        }

        describe("Creating two different families") {
            lateinit var family1: Family
            lateinit var family2: Family
            beforeEachTest {
                FamilyBuilder(world, entityComponentsCache, familyCache).run {
                    allOf(TransformComponent::class)
                    anyOf(PhysiqueComponent::class)
                    family1 = build()
                }
                FamilyBuilder(world, entityComponentsCache, familyCache).run {
                    noneOf(PlayerComponent::class)
                    family2 = build()
                }
            }

            it("should create a family for transform and physique components") {
                (family1 in world.componentManager<TransformComponent>()) `should be equal to` true
                (family1 in world.componentManager<RenderComponent>()) `should be equal to` false
                (family1 in world.componentManager<PlayerComponent>()) `should be equal to` false
                (family1 in world.componentManager<PhysiqueComponent>()) `should be equal to` true
            }

            it("should create a family for player components") {
                (family2 in world.componentManager<TransformComponent>()) `should be equal to` false
                (family2 in world.componentManager<RenderComponent>()) `should be equal to` false
                (family2 in world.componentManager<PlayerComponent>()) `should be equal to` true
                (family2 in world.componentManager<PhysiqueComponent>()) `should be equal to` false
            }
        }
    }

    describe("A Family") {
        describe("Adding a valid component configuration to an entity") {
            lateinit var family: Family
            var entityID = -1
            beforeEachTest {
                entityID = world.entity()
                family = world.family {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                }
                world.componentManager<TransformComponent>().register(entityID)
                world.componentManager<RenderComponent>().register(entityID)
                world.componentManager<PhysiqueComponent>().register(entityID)
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }
            }

            it("should add the entity to the family") {
                family.entities[entityID] `should be equal to` true
                world.componentManager<TransformComponent>()[entityID].x `should be equal to` 1
            }
        }

        describe("Adding an invalid component configuration to an existing entity") {
            lateinit var family: Family
            var entityID1 = -1
            var entityID2 = -1
            beforeEachTest {
                family = world.family {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                }
                entityID1 = world.entity()
                entityID2 = world.entity()
                world.componentManager<TransformComponent>().register(entityID1)
                world.componentManager<RenderComponent>().register(entityID1)
                world.componentManager<PhysiqueComponent>().register(entityID1)
                world.componentManager<TransformComponent>().register(entityID2)
                world.componentManager<RenderComponent>().register(entityID2)
                world.componentManager<PhysiqueComponent>().register(entityID2)
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }

                world.componentManager<PhysiqueComponent>().deregister(entityID1)
                world.componentManager<PlayerComponent>().register(entityID2)
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }
            }

            it("should remove the entity from the family") {
                family.entities[entityID1] `should be equal to` false
                world.componentManager<TransformComponent>()[entityID1].x `should be equal to` 1

                family.entities[entityID2] `should be equal to` false
                world.componentManager<TransformComponent>()[entityID1].x `should be equal to` 1
            }
        }

        describe("Adding and Removing components while iterating over a family") {
            lateinit var family: Family
            var entityID = -1
            beforeEachTest {
                family = world.family {
                    allOf(TransformComponent::class, RenderComponent::class)
                    noneOf(PlayerComponent::class)
                    anyOf(PhysiqueComponent::class)
                }
                entityID = world.entity()
                world.componentManager<TransformComponent>().register(entityID)
                world.componentManager<RenderComponent>().register(entityID)
                world.componentManager<PhysiqueComponent>().register(entityID)
                val manager = world.componentManager<PlayerComponent>()
                // valid iteration
                family.iterate {
                    manager.register(it)
                    world.componentManager<TransformComponent>()[it].x++
                }
                // invalid iteration
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }
                manager.deregister(entityID)
                // valid iteration
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }
                // valid iteration
                family.iterate {
                    manager.register(it)
                    manager.deregister(it)
                    world.componentManager<TransformComponent>()[it].x++
                }
                // valid iteration
                family.iterate {
                    world.componentManager<TransformComponent>()[it].x++
                }
            }

            it("should delay the update operations of the family to the end of the iteration") {
                world.componentManager<TransformComponent>()[entityID].x `should be equal to` 4
            }
        }
    }
})
