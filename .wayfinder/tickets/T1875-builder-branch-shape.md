---
id: T1875
title: R36 选题——GuardModule$Builder 开关组合矩阵补测（feature toggle 装配合同）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-16
---

## Question

K 会话第 36 轮：GuardModule$Builder（112 missed，feature toggle 装配合同——每个 toggle 开→hook 注册、关→不注册）如何定向补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 36 轮 = effort #1227 / spec 1231 / impl 935 续）：

1. **补测面（10 用例）**：全关最小面（仅 DangerousToolGuardHook）；spotlighting/injectionDefense/taintTracking/piiRedaction/canaryGuard 逐一开→hook 注册；enabled=false → DangerousToolGuardHook 移除；dangerousTool entry 注册与名字断言；fromYml 与 builder 等价；allOn ⊇ allOff 超集。
2. **形态**：Buzhou.inMemoryStores() + builder 直构（无 Mockito）；hookNames() 经 assemblySummary 读数。
3. **边界**：Builder 内部 hook 实例化逻辑由 GuardModule 构造器承载——本轮只测 toggle→hook 注册合同；HookAdvisor 留 R22。
