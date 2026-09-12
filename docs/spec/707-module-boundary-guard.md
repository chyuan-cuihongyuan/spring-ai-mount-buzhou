# 707 — 模块边界守卫测试 + internal 存量清零

> 来源：G 会话第 8 轮 = effort #707（借鉴 ArchUnit / eslint no-restricted-imports——自写实现零第三方依赖）/ [T965](../../.wayfinder/tickets/T965-boundary-guard-shape.md) / [T966](../../.wayfinder/tickets/T966-boundary-guard-verify.md) / impl 510。

## 背景

spec 09 两条包级约定——「internal 跨模块禁止引用」「feature 模块互依禁止（星形拓扑，唯一二层边 otel/dashboard → observability）」——此前只有文档约束：enforcer 查 maven 依赖白名单，包级 import 无物理守卫。守卫缺位期间漂移出 8 处存量违规（guard/memory/resilience/observability/examples 直接引用 core internal 类）。

## 目标

- `ModuleBoundaryGuardTest`（starter 测试域）：源码级扫全部 buzhou 模块 src/main/java；包→模块 longest-prefix 自举归属（各模块源码包全集）；import 与内联 FQN 同扫；仓库布局不符时 assume 跳过（诚实边界）。
- **存量清零**：跨模块使用的 internal 类按「事实公共 API」原则迁出——新公共包 `core.memory`（DefaultFactStore / FactDecayPolicy / DecayingFactStore）、`core.token`（CharHeuristicTokenEstimator / TableContextWindowResolver）；AtomicStateCounters 归 `core.hook`。行为零变化，测试随迁。
- API 快照随轮再生（spec 615 门控合规触发；恰 11 行新增：6 新公共类 + 5 迁出类入面）。

## 非目标

不引 ArchUnit（out of scope：零新依赖）；不做 maven 层依赖白名单测试（enforcer 既有面）；api 子包（`*.api`）语义承诺面本轮不重划。

## 测试

守卫先红（抓内联 FQN 违规——自证非恒绿）后绿（清零）；受影响七模块测试绿；全仓 verify 绿（含快照门 / SpecCoverage / enforcer / JaCoCo）。

## 兼容性

internal 类迁出 = 新增公共类（非破坏——原 internal 本不承诺可用）；包改名对 internal 消费者无兼容义务（0.x）。
