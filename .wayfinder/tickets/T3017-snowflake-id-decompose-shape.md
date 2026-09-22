---
id: T3017
title: 雪花 ID 分解的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

时间有序 ID 的自描述信息怎么编解码？（spec 1908 / effort #1908 / R109）

## Resolution`

**Twitter Snowflake 布局纯计算 `SnowflakeIdDecompose`
（core/concurrent）**：decompose（41 位时间戳+10 位机器+12 位序列
按位拆解）+ compose 逆组装 roundtrip 自洽；worker≤1023/seq≤4095/
时间 ≥ epoch 越界 fail-fast。落轮 grep 复核无占坑。
