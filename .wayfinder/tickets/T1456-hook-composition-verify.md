---
id: T1456
title: Hook 链解析顺序快照读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1455
created: 2026-09-14
---

## Question

J 会话第 3 轮：链解析快照如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（HookChainCompositionTest，AssertJ + RecordingHook 同款假件风格）：解析序稳定（order 升序、同序按名字典序）；disabled 命中者被滤出 resolved；幽灵禁用（拼错名）入 ghostDisabledNames；真实禁用不入幽灵集；快照 List/Set 不可变；空链空禁用全空。双 reset 不需要（实例态非进程态）。定向 `mvn -pl buzhou-core test -Dtest='HookChainCompositionTest,HookChainTest'` 绿（后者回归排序/禁用既有语义）。
