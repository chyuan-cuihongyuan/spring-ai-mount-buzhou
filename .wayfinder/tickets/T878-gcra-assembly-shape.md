---
id: T878
title: GCRA 装配扩散的配置形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Loop 4 的 GcraRateLimitBackend 只有编程注入（spec 603 明记「yml 装配扩散另行」）。装配面怎么开？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 15 轮 = effort #600 / spec 614 / impl 467）：

1. `buzhou.resilience.rate-limit.smoothing`：`token-bucket`（默认，零变化）/ `gcra`；配 `gcra-burst-tolerance`（默认 0 = 严格平滑）。词汇闭集校验 fail-fast（拼写错不静默宽容）。
2. **共享后端在场则共享语义优先**（store.type=redis 的跨实例一份额度 > 单进程整形——诚实取舍入档）。
3. RateLimit record 扩组件踩多构造绑定坑（R39 同法）——canonical @ConstructorBinding 标注修复。
4. 另：redteam 门阈值主题核查已 env 化（INTERCEPT_MIN/DANGEROUS_MAX）——ruled-out。
