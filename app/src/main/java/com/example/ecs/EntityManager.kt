package com.example.ecs

import com.example.model.EnemyType
import kotlin.reflect.KClass

/**
 * Base EntityManager providing a data-oriented Entity-Component System structure.
 * Manages entity creation, destruction, generic component storage, fast-path
 * typed tables, and entity prefabs for the Alchemical Warden and enemy types.
 */
class EntityManager {
  private var nextEntityId = 1
  val activeEntities = mutableSetOf<EntityId>()

  // Generic component store: Class -> Map<EntityId, Component>
  private val genericComponentStore = mutableMapOf<KClass<out Component>, MutableMap<EntityId, Component>>()

  // High-performance typed fast-path stores (zero-allocation direct access for systems)
  val transforms = mutableMapOf<EntityId, TransformComponent>()
  val velocities = mutableMapOf<EntityId, VelocityComponent>()
  val renderables = mutableMapOf<EntityId, RenderComponent>()
  val healths = mutableMapOf<EntityId, HealthComponent>()
  val colliders = mutableMapOf<EntityId, ColliderComponent>()
  val wardens = mutableMapOf<EntityId, WardenComponent>()
  val enemies = mutableMapOf<EntityId, EnemyAIComponent>()
  val lights = mutableMapOf<EntityId, LightComponent>()
  val weapons = mutableMapOf<EntityId, WeaponComponent>()
  val physics = mutableMapOf<EntityId, PhysicsComponent>()

  fun createEntity(): EntityId = synchronized(this) {
    val id = nextEntityId++
    activeEntities.add(id)
    id
  }

  fun destroyEntity(id: EntityId) = synchronized(this) {
    activeEntities.remove(id)

    // Remove from generic store
    for (store in genericComponentStore.values) {
      store.remove(id)
    }

    // Remove from fast-path stores
    transforms.remove(id)
    velocities.remove(id)
    renderables.remove(id)
    healths.remove(id)
    colliders.remove(id)
    wardens.remove(id)
    enemies.remove(id)
    lights.remove(id)
    weapons.remove(id)
    physics.remove(id)
  }

  fun clear() = synchronized(this) {
    activeEntities.clear()
    genericComponentStore.clear()
    transforms.clear()
    velocities.clear()
    renderables.clear()
    healths.clear()
    colliders.clear()
    wardens.clear()
    enemies.clear()
    lights.clear()
    weapons.clear()
    physics.clear()
    nextEntityId = 1
  }

  fun getActiveEntitiesSnapshot(): List<EntityId> = synchronized(this) {
    activeEntities.toList()
  }

  // Generic Component Operations
  fun <T : Component> addComponent(entity: EntityId, component: T): T = synchronized(this) {
    val kclass = component::class
    val store = genericComponentStore.getOrPut(kclass) { mutableMapOf() }
    store[entity] = component

    // Synchronize into fast-path stores if applicable
    when (component) {
      is TransformComponent -> transforms[entity] = component
      is VelocityComponent -> velocities[entity] = component
      is RenderComponent -> renderables[entity] = component
      is HealthComponent -> healths[entity] = component
      is ColliderComponent -> colliders[entity] = component
      is WardenComponent -> wardens[entity] = component
      is EnemyAIComponent -> enemies[entity] = component
      is LightComponent -> lights[entity] = component
      is WeaponComponent -> weapons[entity] = component
      is PhysicsComponent -> physics[entity] = component
    }
    component
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Component> getComponent(entity: EntityId, kclass: KClass<T>): T? = synchronized(this) {
    genericComponentStore[kclass]?.get(entity) as? T
  }

  inline fun <reified T : Component> get(entity: EntityId): T? {
    return getComponent(entity, T::class)
  }

  fun <T : Component> hasComponent(entity: EntityId, kclass: KClass<T>): Boolean = synchronized(this) {
    genericComponentStore[kclass]?.containsKey(entity) == true
  }

  inline fun <reified T : Component> has(entity: EntityId): Boolean {
    return hasComponent(entity, T::class)
  }

  fun <T : Component> removeComponent(entity: EntityId, kclass: KClass<T>) = synchronized(this) {
    genericComponentStore[kclass]?.remove(entity)
    when (kclass) {
      TransformComponent::class -> transforms.remove(entity)
      VelocityComponent::class -> velocities.remove(entity)
      RenderComponent::class -> renderables.remove(entity)
      HealthComponent::class -> healths.remove(entity)
      ColliderComponent::class -> colliders.remove(entity)
      WardenComponent::class -> wardens.remove(entity)
      EnemyAIComponent::class -> enemies.remove(entity)
      LightComponent::class -> lights.remove(entity)
      WeaponComponent::class -> weapons.remove(entity)
      PhysicsComponent::class -> physics.remove(entity)
    }
  }

  inline fun <reified T : Component> remove(entity: EntityId) {
    removeComponent(entity, T::class)
  }

  fun getEntitiesWith(vararg kclasses: KClass<out Component>): List<EntityId> = synchronized(this) {
    if (kclasses.isEmpty()) return activeEntities.toList()
    return activeEntities.filter { entity ->
      kclasses.all { kclass -> genericComponentStore[kclass]?.containsKey(entity) == true }
    }
  }

  // --- Entity Prefab Factory Methods ---

  /**
   * Spawns the playable Alchemical Warden with full components.
   */
  fun createWarden(x: Float, y: Float): EntityId {
    val id = createEntity()
    addComponent(id, PlayerTag())
    addComponent(id, TransformComponent(x = x, y = y))
    addComponent(id, VelocityComponent(maxSpeed = 220f))
    addComponent(
      id,
      RenderComponent(
        shape = ShapeType.WARDEN,
        colorR = 0.12f,
        colorG = 0.18f,
        colorB = 0.24f,
        size = 20f,
        layer = 2
      )
    )
    addComponent(id, HealthComponent(current = 100f, max = 100f, defense = 4f))
    addComponent(id, ColliderComponent(radius = 18f))
    addComponent(id, PhysicsComponent(width = 36f, height = 36f, mass = 1.2f))
    addComponent(id, WardenComponent(steam = 80f, maxSteam = 80f))
    addComponent(id, WeaponComponent(weaponId = "hammer_standard", damage = 38f, attackRate = 0.40f, knockback = 180f))
    // Alchemical emerald lantern light
    addComponent(
      id,
      LightComponent(
        colorR = 0.10f,
        colorG = 0.85f,
        colorB = 0.45f,
        radius = 240f,
        intensity = 1.0f
      )
    )
    return id
  }

  /**
   * Spawns a fast melee Clockwork Skitterer enemy.
   */
  fun createSkitterer(x: Float, y: Float): EntityId {
    val id = createEntity()
    addComponent(id, EnemyTag())
    addComponent(id, TransformComponent(x = x, y = y))
    addComponent(id, VelocityComponent(maxSpeed = 150f))
    addComponent(
      id,
      RenderComponent(
        shape = ShapeType.SKITTERER,
        colorR = 0.85f,
        colorG = 0.48f,
        colorB = 0.05f,
        size = 18f,
        layer = 1
      )
    )
    addComponent(id, HealthComponent(current = 65f, max = 65f, defense = 2f))
    addComponent(id, ColliderComponent(radius = 16f))
    addComponent(id, PhysicsComponent(width = 32f, height = 32f, mass = 0.8f))
    addComponent(
      id,
      EnemyAIComponent(
        type = EnemyType.SKITTERER,
        attackRange = 40f,
        homeX = x,
        homeY = y
      )
    )
    // Red optic eye light
    addComponent(
      id,
      LightComponent(
        colorR = 0.95f,
        colorG = 0.15f,
        colorB = 0.15f,
        radius = 90f,
        intensity = 0.7f
      )
    )
    return id
  }

  /**
   * Spawns an armored ranged Steam Vane Automaton enemy.
   */
  fun createSteamVane(x: Float, y: Float): EntityId {
    val id = createEntity()
    addComponent(id, EnemyTag())
    addComponent(id, TransformComponent(x = x, y = y))
    addComponent(id, VelocityComponent(maxSpeed = 70f))
    addComponent(
      id,
      RenderComponent(
        shape = ShapeType.STEAM_VANE,
        colorR = 0.45f,
        colorG = 0.52f,
        colorB = 0.60f,
        size = 26f,
        layer = 1
      )
    )
    addComponent(id, HealthComponent(current = 140f, max = 140f, defense = 10f))
    addComponent(id, ColliderComponent(radius = 24f))
    addComponent(id, PhysicsComponent(width = 48f, height = 48f, mass = 2.0f))
    addComponent(
      id,
      EnemyAIComponent(
        type = EnemyType.STEAM_VANE,
        attackRange = 260f,
        homeX = x,
        homeY = y
      )
    )
    // Boiler pressure light
    addComponent(
      id,
      LightComponent(
        colorR = 0.95f,
        colorG = 0.65f,
        colorB = 0.10f,
        radius = 130f,
        intensity = 0.8f
      )
    )
    return id
  }

  /**
   * Spawns Overseer Malakor, the Forge Sovereign boss.
   */
  fun createOverseerBoss(x: Float, y: Float): EntityId {
    val id = createEntity()
    addComponent(id, BossTag())
    addComponent(id, EnemyTag())
    addComponent(id, TransformComponent(x = x, y = y))
    addComponent(id, VelocityComponent(maxSpeed = 95f))
    addComponent(
      id,
      RenderComponent(
        shape = ShapeType.BOSS_OVERSEER,
        colorR = 0.48f,
        colorG = 0.22f,
        colorB = 0.88f,
        size = 46f,
        layer = 2
      )
    )
    addComponent(id, HealthComponent(current = 950f, max = 950f, defense = 20f))
    addComponent(id, ColliderComponent(radius = 42f))
    addComponent(id, PhysicsComponent(width = 84f, height = 84f, mass = 10.0f))
    addComponent(
      id,
      EnemyAIComponent(
        type = EnemyType.BOSS_OVERSEER,
        attackRange = 75f,
        homeX = x,
        homeY = y
      )
    )
    // Core furnace purple/red glow
    addComponent(
      id,
      LightComponent(
        colorR = 0.82f,
        colorG = 0.25f,
        colorB = 0.95f,
        radius = 380f,
        intensity = 1.3f
      )
    )
    return id
  }

  /**
   * Spawns an environmental or conduit point light.
   */
  fun createPointLight(x: Float, y: Float, r: Float, g: Float, b: Float, radius: Float, intensity: Float = 1f): EntityId {
    val id = createEntity()
    addComponent(id, LightEmitterTag())
    addComponent(id, TransformComponent(x = x, y = y))
    addComponent(
      id,
      LightComponent(
        colorR = r,
        colorG = g,
        colorB = b,
        radius = radius,
        intensity = intensity
      )
    )
    return id
  }
}
