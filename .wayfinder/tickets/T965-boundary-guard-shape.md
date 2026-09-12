---
id: T965
title: 模块边界守卫测试 + 存量违规清零的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

「internal 跨模块禁止引用」「feature 模块互依禁止」只是文档约定——enforcer 只查 maven 依赖白名单，包级引用无物理守卫。ArchUnit 式守卫怎么做（不引第三方依赖）？存量违规如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 8 轮 = effort #707 / spec 707 / impl 510）：自写源码级守卫 `ModuleBoundaryGuardTest`（starter 测试域，零新依赖）——扫全部 buzhou 模块 src/main/java，包→模块 longest-prefix 自举归属，import + 内联 FQN 同扫，两条规则：跨模块 `.internal.` 引用禁止；feature 互依禁止（唯一二层边 otel/dashboard→observability 豁免；core 双向豁免）。**存量违规不清零不出门**：探查发现 8 处（guard→core DefaultFactStore/FactDecayPolicy/DecayingFactStore、memory→core token 两件、resilience→core AtomicStateCounters、observability→core token、examples 测试）——全部按「已被跨模块使用 = 事实公共 API」原则迁出 internal：core 新公共包 `core.memory`（3 类）/`core.token`（2 类），AtomicStateCounters 归 `core.hook`；行为零变化，测试随迁。守卫上线即抓到 import 扫描漏掉的内联 FQN 违规（自证有效）。借鉴 ArchUnit / eslint no-restricted-imports。
