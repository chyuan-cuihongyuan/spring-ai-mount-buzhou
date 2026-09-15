---
id: T1851
title: R22 选题——HookAdvisor 切面分发直测（beforeModel 短路/afterModel 回填/onModelError 兜底决策树）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 22 轮：HookAdvisor（6 missed，beforeModel/onModelError 切面分发的 stub 家族）如何直测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 22 轮 = effort #1221 / spec 1221 / impl 924）：

1. **补测面（8 用例，adviseCall × adviseStream 同构对称）**：beforeModel Block 短路（nextCall 不触达、回填文本响应）；happy path（markResponded→afterModel→返回 ctx.response，含 afterModel replaceResponse 回填生效）；nextCall 异常 → onModelError 决策树三分支（Block 回填文本兜底吞错/Replace 回填结构化响应/Continue/无处理 rethrow 原异常）；流式同构（beforeModel Block → Flux.just 兜底；Flux.error → onModelError Block → 兜底 Flux；无处理 → Flux.error 透传）。
2. **形态**：HookChain 子类 override 三切面（脚本化 HookResult）+ HookEnvironment 3 参真件 + CallAdvisorChain/StreamAdvisorChain 匿名 stub；ChatClientResponse 经 record 构造。
3. **边界**：不改主代码；HookChain 自身分度逻辑由 HookChainCompositionTest 等既有套件覆盖，本轮只测 advisor 分发。
