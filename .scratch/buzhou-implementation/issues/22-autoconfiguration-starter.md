# 22 — AutoConfiguration 装配层与 starter 聚合

**What to build:** 为全量机制模块补齐 Spring Boot 自装配层，兑现 spec 09 设计目标「模块自装配（AutoConfiguration）+ 聚合 starter：单模块引入即得单机制能力」。各机制模块代码已设计好 `XxxModule.configure()` / builder 返回 `RuntimeConfig`、经 `RuntimeConfig.merge` 组合的入口（见 ticket 14/15/17/18/19 收口备注「AutoConfiguration 装配归 ticket 20」），但本层此前未落地——ticket 20 范围裁定为「仅发布工程」，故单列本票。

**Blocked by:** 20（已 done）

**Status:** done

## 范围

### core 装配（`buzhou-core`）
- `BuzhouCoreAutoConfiguration`：收集容器内全部 `BuzhouHook` / `SessionAssemblyCustomizer` / `SessionResourceCustomizer` / `ToolCallback` / `MemoryViewProcessor` bean，经 `RuntimeConfig.merge` 合成单一 `RuntimeConfig`；按 `buzhou.store.type`（默认 `memory`）装配 `BuzhouStores`（memory 走 `Buzhou.inMemoryStores()`；jdbc/redis 在对应模块引入后激活）；暴露 `AgentRuntime` bean（依赖 `ChatModel`）。
- `@ConfigurationProperties("buzhou")` 承载 store.type 等内核开关。
- 契约：`internal` 包不暴露；装配类放 `...core.config` 或 `...core` 公共包。

### 机制模块装配（每模块一个 `Buzhou<Mech>AutoConfiguration`）
- 每模块 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。
- `@ConditionalOnProperty("buzhou.<mech>.enabled")` 控制开关，默认值对齐 spec 09 配置表：
  - 默认 **开**：`memory` / `spill` / `observability` / `skills` / `mcp` / `guard` / `tools`（safe-by-default）
  - 默认 **关**：`observe.otel.enabled` / `observe.dashboard.enabled`
- 各装配类包装既有 `XxxModule.configure(...)` / builder，把产出（hook / customizer / tool / processor）注册为 bean，供 core 装配收集。
- store 模块：`buzhou-store-jdbc` / `buzhou-store-redis` 按 `buzhou.store.type=jdbc|redis` 条件装配五 SPI 实现（替换 core 的内存默认）。
- `buzhou-observe-dashboard`：`dashboard.port` 配置（复用业务容器 or 独立端口）。

### starter 聚合（`buzhou-spring-boot-starter`）
- 当前 starter pom **无任何依赖**（空壳）。补全：聚合 memory/spill/observability/observe-otel/observe-dashboard/skills/mcp/guard/tools/store-jdbc/store-redis 全部机制模块。
- starter **不写装配类**、无代码（spec 09）；仅依赖聚合。

### 测试
- 每模块用 `ApplicationContextRunner` 验证：开关开/关时 bean 是否装配；core 装配能否把多模块产出 merge 进 `AgentRuntime`。
- 契约：引入单模块（如 `buzhou-memory`）不拖入其他机制（依赖白名单物理无环保证）。

## 验收
- [x] 引 `buzhou-spring-boot-starter` 默认配置即可 spawn AgentSession 跑通多轮对话（机制按默认开关启用）
- [x] 每模块 `@ConditionalOnProperty` 开关生效（关掉 `buzhou.memory.enabled` 后 memory bean 不装配）
- [x] `buzhou.store.type=memory|jdbc|redis` 三态切换装配对应 store
- [x] `otel` / `dashboard` 默认关，显式开才生效
- [x] starter 仅聚合、无装配类、无代码
- [x] ApplicationContextRunner 装配测试覆盖每模块开关
- [x] `mvn verify` 全绿

## 备注
- spec 依据：`docs/spec/09-modules-engineering.md`「运行期：模块装配开关」表、「自装配注册」约定（第 5 条）。
- 入口已就绪：`MemoryModule.configure` / `GuardModule.builder().configure()` / `SpillModule` / `ObservabilityModule` 等返回 `RuntimeConfig`，core `HarnessAssembler.assemble` 接收 `Collection<BuzhouHook>` + `List<SessionAssemblyCustomizer>`。
- 不在本票：community extension（`buzhou-config-nacos`、`buzhou-tokenizer-jtokkit`）按需另列。
