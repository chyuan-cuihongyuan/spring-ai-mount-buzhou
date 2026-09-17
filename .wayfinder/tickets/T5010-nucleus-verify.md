---
id: T5010
title: Q 会话 R5 top-p 核采样的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5009]
created: 2026-09-18
---

## Question

R5 合同怎么逐一验绿？（spec 3004 / effort #3004 / R5）

## Resolution

**验证通过**：NucleusSamplerTest 七测全绿——keptCount 六级阶梯
手算（0.5/0.8/0.95 累积界）、p→0 千次恒 argmax、p=0.6 经验频率
0.625/0.375±0.02 且核外恒零、p=1 全覆盖+−∞ 恒不中、同种子百次
序列全等、非法参数五路 fail-fast。
