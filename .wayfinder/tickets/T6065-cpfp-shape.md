---
id: T6065
title: R 会话 R33 祖先费率打包的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

依赖图任务怎么被后继的聚合价值抬进批次？（spec 4032 /
effort #4032 / R33）

## Resolution

**AncestorFeerate（core/policy）**：Bitcoin CPFP——祖先闭包
∑fee/∑size 聚合费率（共享祖先去重），inclusionOrder 每轮取
全池祖先费率最高者整包入场（父先子后）——子的高费把父拖进
批次；前置先注册 DAG-by-construction（未知父 fail-fast）。
