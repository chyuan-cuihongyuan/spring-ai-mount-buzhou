# 1231 — R36：GuardModule$Builder 开关组合矩阵补测

> 来源：K 会话第 36 轮 = effort #1227（[T1875](../../.wayfinder/tickets/T1875-builder-branch-shape.md) / [T1876](../../.wayfinder/tickets/T1876-builder-branch-verify.md) / impl 935 续）。方法论：**真值矩阵测试**（feature toggle builder 的开/关组合全矩阵——「全关最小面 + 逐一开 + 全开超集」三层递进）。

## Problem Statement

GuardModule$Builder 的 112 missed 集中在 build() 方法的 feature toggle 条件链：每个 toggle 开 → 对应 hook 注册、关 → 不注册——「装配合同」从未被逐 toggle 断言。既有测试只测 happy path（多个 toggle 同时开），未覆盖「关态不注册」与「最小面」边界。

## 目标

- GuardModuleBuilderBranchTest（10 用例）：allOff 最小面（仅 DangerousToolGuardHook）；spotlighting/injectionDefense/taintTracking/piiRedaction/canaryGuard 逐一开→hook 注册；enabled=false → DangerousToolGuardHook 移除；dangerousTool entry 注册与名字断言；fromYml 与 builder 等价；allOn ⊇ allOff 超集。

## 实现决策

- Buzhou.inMemoryStores() + builder 直构（无 Mockito）；hookNames() 经 assemblySummary 读数——hook 名即合同（DangerousToolGuardHook/SpotlightHook/CanaryGuardHook 等实际名以 assemblySummary 为准）。

## 测试决策

- 断言只对外部行为：assemblySummary 返回的 hook 名列表；不测内部 hooks List 的实现。
- 验收门：定向绿 + guard 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- HookAdvisor（R22 已覆盖）。
- MemoryModule（158 missed，留 R37+）。

## Further Notes

- 「从未被断言的产出字段即测试缺口」与「全关最小面 + 逐一开 + 全开超集」三层递进——feature toggle builder 的标准化测试范式。
