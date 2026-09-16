---
id: T3150
title: 老化优先级队列的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3149]
created: 2026-09-17
---

## Question

AgingPriorityQueue 合同（反超/FIFO/退化/快照/畸形）怎么钉住？（spec 2024 / effort #2024 / R25）

## Resolution

**八用例全绿**（首跑 1 红根因：测试误设同刻入队——同速率老化不改变
相对差，反超须等待时长不同；改后入队场景后 8/8）：零速率静态序 /
90 秒老住户（有效 100）反超新来者（90）/ 10 秒差距未够新来者仍胜 /
同分 FIFO / 零速率退化 / 空队 null / 快照不动队列 / 畸形六型
fail-fast。
