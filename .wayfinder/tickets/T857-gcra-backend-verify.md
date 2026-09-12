---
id: T857
title: GCRA 后端验证口径（步调/突发界/预检/超限恢复）
type: task
status: closed
assignee: zcode-f
blocked-by: T856
created: 2026-09-12
---

## Question

GCRA 整形语义如何钉住不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（GcraRateLimitBackendTest，8 用例全绿）：

- 严格平滑 β=0（cap=6→τ=10s）：t0 放行、即刻第二个拒、满 10s 再放行。
- β=30s：即刻连发恰 4 个（第 5 拒）——突发界与公式一致。
- 预检不推进 TAT（反复预检后首个真实扣减仍可用）。
- consume 600 单元强推 6000s：预检拒绝，时间追上恢复。
- secondsUntilAvailable 首个放行后 = τ；available = 4（β=30 新桶）/0（超限）。
- 未启用维度恒拒 + 容量 0 + 等待 MAX_VALUE；kind = memory-gcra；负 β 构造拒绝。
