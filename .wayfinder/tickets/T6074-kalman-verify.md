---
id: T6074
title: R 会话 R37 标量卡尔曼滤波的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6073]
created: 2026-09-24
---

## Question

R37 合同怎么逐一验绿？（spec 4036 / effort #4036 / R37）

## Resolution

**验证通过**：ScalarKalmanFilterTest 六测全绿——常值收敛 +
方差单调收缩；纯 update 增益单调下降 / predict 回升；平滑
降噪（滤波方差 < 裸读数方差）；确定性回放；畸形定构与
非有限读数 fail-fast。
