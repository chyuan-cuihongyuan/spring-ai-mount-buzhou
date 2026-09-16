---
id: T3106
title: 指数直方图滑窗计数的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3105]
created: 2026-09-17
---

## Question

ExponentialWindowCounter 合同（精确域/误差界/对数空间/畸形）怎么钉住？（spec 2002 / effort #2002 / R3）

## Resolution

**七用例全绿**（首跑 4 红根因：合并桶 addLast 破坏时间序 + 只存 last
无法判跨界——重写为三元组桶 set 回原位后 7/7）：无跨界精确=5 / 滑出
清零 / burst 滑出 / 确定性序列（事件@3i、W=16）|est−exact| ≤ 自描述界
（独立精确重放对照）/ 千同刻事件桶数 ≤24 且 est 精确 / 畸形 window
0、−5 fail-fast / 交替 insert/tick 50 轮不丢新鲜事件。
