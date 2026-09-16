---
id: T3132
title: min-RTT 滑窗滤波的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3131]
created: 2026-09-17
---

## Question

MinRttTracker 合同（最小稳定/滑出重算/追认/新鲜度/畸形）怎么钉住？（spec 2015 / effort #2015 / R16）

## Resolution

**七用例全绿**（首版实现缺陷：addLast 后才算 currentMin 含新样本致
新最小检测恒假——改 beforeMin 先算后入样后 7/7）：膨胀样本不扰动
最小 / 999 界内 1000 出窗次小接管 / 网络改善追认+新鲜时刻 / 空窗零+
全部滑出回零 / 持平不刷新更小刷新 / 零 RTT 合法 / 畸形三型
fail-fast。
