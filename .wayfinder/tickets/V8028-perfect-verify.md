---
id: V8028
title: V 会话 V14 PerfectHash 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8027]
created: 2026-09-29
---

## Question

V14 合同怎么逐一验绿？（spec 8013 / effort #8013 / V14）

## Resolution

**验证通过**：三测全绿——小键集双射无碰撞；300 随机键集
双射性质+同种子同表；构建失败路径；fail-fast。
