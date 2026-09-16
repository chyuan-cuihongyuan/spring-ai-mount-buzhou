---
id: T3186
title: 刻度轮定时器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3185]
created: 2026-09-17
---

## Question

TickWheelTimer 合同（轮内/跨轮/回绕/幂等/取消/畸形）怎么钉住？（spec 2042 / effort #2042 / R43）

## Resolution

**七用例全绿**（两轮修复：①advance 因 due 空提前 return 丢圈数递减
写回——无条件写回；②圈数公式 (cursor+delay)/W 使首访即到期多算一圈
——改 (delay−1)/W；修后 7/7）：恰第 5 tick / W=8 delay=20 恰第 20 /
同槽三任务齐发 / 15 tick 回绕后 delay2 仍准 / 幂等重调度第 7 tick /
取消不触发 / 畸形六型（W 4、非 2 幂 100、空 id、delay 0、null cancel）
fail-fast。
