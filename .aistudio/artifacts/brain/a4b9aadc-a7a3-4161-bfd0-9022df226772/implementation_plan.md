# OpenGL Rendering Engine & Entity-Component System

Architectural plan for upgrading *The Last Reactor* to an **OpenGL ES 2.0/3.0 rendering pipeline** with a lightweight **Entity-Component System (ECS)**, integrated into Jetpack Compose with custom GLSL shaders featuring dynamic 2D point lights.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed by the user and guide this architecture:

- **Confirmed Decision 1 (OpenGL Integration)**: Use Android `GLSurfaceView` with `GLSurfaceView.RENDERMODE_CONTINUOUSLY` embedded into Jetpack Compose via `AndroidView`. The existing Jetpack Compose HUD, mini-map, touch controls, and dialogs float seamlessly on top.
- **Confirmed Decision 2 (Data-Oriented ECS)**: A clean, modular Entity-Component System:
  - `Entity`: Light numeric identifier (`typealias EntityId = Int`).
  - `Components`: Pure data bags (`TransformComponent`, `VelocityComponent`, `RenderComponent`, `HealthComponent`, `ColliderComponent`, `WardenComponent`, `EnemyAIComponent`, `LightComponent`).
  - `Systems`: Single-responsibility update loops (`MovementSystem`, `EnemyAISystem`, `CombatSystem`, `HazardSystem`, `OpenGLRenderSystem`).
- **Confirmed Decision 3 (Dynamic 2D Lighting Shaders)**: GLSL shaders with ambient gothic darkness and dynamic point lights (alchemical emerald core, torch flickers, steam blasts, and hammer sparks) rendered in the fragment stage.

---

## 1. Overview & Core Concept

- **What It Does**: Transitions the game's core simulation and rendering from 2D CPU Canvas to a native OpenGL ES 2.0/3.0 GPU-accelerated pipeline, driven by an Entity-Component System.
- **Key Value**: Drastically reduces garbage collection pressure, unleashes hardware-accelerated vertex batching and fragment lighting shaders, and makes adding new enemies, behaviors, and environmental mechanisms effortless through composable ECS components.

---

## 2. User Experience & Visual Polish

- **Visual Quality**:
  - Dark, soot-stained industrial slate floor rendered via batched textured/colored vertex quads.
  - Custom GLSL Fragment Shader: Calculates distance-based falloff for multiple 2D point lights (the Great Reactor Flue, player alchemical lantern, enemy optic eyes, and molten slag vents).
  - High-precision particle effects (sparks, steam plumes, and acid splatters) rendered in GPU vertex buffers.
- **Seamless Compose Overlay**:
  - Touch virtual thumbstick, combat buttons, HUD gauges, and dialogue modals continue to render with M3 styling over the hardware-accelerated OpenGL surface.

---

## 3. Key Product Decisions & Trade-Offs

- **`GLSurfaceView` with `AndroidView` vs. Custom `TextureView` / EGL**:
  - *Chosen Approach*: `GLSurfaceView` wrapped in Compose `AndroidView`.
  - *Why*: Manages EGL context, display sync, surface lifecycle, and rendering thread automatically without threading pitfalls or buffer-swap lag.
- **Lightweight Data-Oriented ECS vs. Heavy Framework**:
  - *Chosen Approach*: In-engine Kotlin ECS utilizing sparse component stores (`IntMap` / array indices).
  - *Why*: Zero third-party library dependencies, maximum speed, and zero reflection.

---

## 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Compose Root)                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
          ┌─────────────────────────┴────────────────────────┐
          ▼                                                  ▼
┌───────────────────────────────┐          ┌─────────────────────────────┐
│    GameScreen (Compose HUD)   │          │   GLSurfaceView (Android)   │
│ - Virtual Joystick & Buttons  │          │ - EGL Context (GLES 2.0)    │
│ - Health / Steam Gauges       │          │ - ReactorGLRenderer         │
│ - Transmutation / Codex Modals│          │   └── OpenGLRenderSystem    │
└───────────────────────────────┘          └──────────────┬──────────────┘
                                                          │
                                                          ▼
                                           ┌─────────────────────────────┐
                                           │     ECS World Engine        │
                                           │ - EntityManager & Stores    │
                                           │ - MovementSystem            │
                                           │ - EnemyAISystem             │
                                           │ - CombatSystem              │
                                           │ - HazardSystem              │
                                           └─────────────────────────────┘
```

### ECS Data Model

- **Entities**:
  - `EntityId = 1`: The Alchemical Warden (Player)
  - `EntityId = 2..N`: Enemies (Clockwork Skitterers, Steam Vane Automatons, Overseer Malakor)
  - `EntityId = N..M`: Hazards, Projectiles, and Dynamic Lights
- **Components**:
  - `TransformComponent(var x, var y, var angle, var scaleX, var scaleY)`
  - `VelocityComponent(var vx, var vy, var drag, var maxSpeed)`
  - `RenderComponent(var shape, var colorHex, var layer, var visible)`
  - `ColliderComponent(var radius, var isTrigger, var categoryMask)`
  - `HealthComponent(var current, var max, var invulnerableTimer)`
  - `WardenComponent(var steam, var maxSteam, var comboStep, var attackCooldown, var dashCooldown)`
  - `EnemyAIComponent(var type, var state, var timer, var attackRange, var attackCooldown)`
  - `LightEmitterComponent(var colorR, var colorG, var colorB, var radius, var intensity)`
- **GLSL Shaders**:
  - `vertex_shader.glsl`: Projects 2D world coordinates to camera NDC space with rotation and model transform matrix.
  - `fragment_shader.glsl`: Applies base color, ambient darkness (`#090D14`), and evaluates dynamic attenuation for up to 8 point lights per frame.

---

## Verification Plan

### Automated & Unit Tests
- Unit tests verifying:
  - ECS entity creation, component attachment, query filtering, and entity destruction.
  - `MovementSystem` velocity integration and boundary collision.
  - `CombatSystem` hit registration and health deduction.
  - `EnemyAISystem` state transitions (Idle -> Pursuing -> Windup -> Attack).
- Android build compilation via `compile_applet`.

### Interactive Verification
- Confirm smooth 60 FPS OpenGL rendering inside `GLSurfaceView`.
- Confirm touch and keyboard inputs smoothly control the ECS Alchemical Warden.
- Confirm dynamic 2D lighting highlights surrounding environment and enemies.
