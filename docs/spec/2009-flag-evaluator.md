# Spec 2009 — 特性开关求值器（effort #2009，R10）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3119–T3120，impl 1560）。
> 借鉴：OpenFeature——求值永不抛出、五态 reason、默认变体兜底、
> 求值质量显形。

## Problem Statement

特性开关散落各处 if-else：未注册 flag 静默 null（NPE 深处爆）、targeting
谓词抛错炸整条调用链、求值出错无计数——开关病灶（谓词异常/幽灵 flag）
不可见。

## Solution

`FlagEvaluator`（core/policy，synchronized 小临界区）：

- `FlagDefinition` record：默认变体 + 可选 targeting 谓词与命中变体
  （须成对非空——canonical compact constructor 守约；staticFlag /
  targetedFlag 工厂便捷）；
- `evaluate(flag, ctx)` **永不抛出**：未注册 → FLAG_NOT_FOUND + null
  值；targeting 命中 → TARGETING_MATCH + 命中变体；未命中 → STATIC +
  默认变体；谓词抛错 → DEFAULT + 默认变体兜底（ERROR 与 DEFAULT 同记
  ——兜底发生即求值质量事件）；
- `reasonCounts()` 五态分布读数（ERROR/DEFAULT 占比 = 谓词病灶率）；
- `registeredFlags()` 注册面只读快照；
- 契约：flag 名非空非白、ctx 可 null（空 Map 语义）fail-fast。

## User Stories

1. 作为业务作者，evaluate 永不抛——开关炸不了调用链，兜底值可预期。
2. 作为开关运维，reasonCounts 里 FLAG_NOT_FOUND 高 = 幽灵 flag 引用；
   ERROR 高 = targeting 谓词病灶——两类腐化分别显形。

## Implementation Decisions

- 五态 reason 是 OpenFeature reason + errorCode 的工程并近似（文档
  显式映射）；注册期一次/求值期只读的约定（热注册允许但非设计目标）。

## Testing Decisions

- 静态/命中/未命中/未注册/抛错兜底五态各例；null ctx 正常；分布计数
  精确；注册面快照；畸形六型 fail-fast（含 canonical 构造的不成对）。

## Out of Scope

- 不做 provider 抽象/远程 flag 源（本地注册表语义）；
- 不接 GuardModule 装配（接线归后续轮）。

## Further Notes

- 与 LayeredPolicy（四层配置覆盖）正交：policy 定绑定值，flag 定实验
  变体分支。
