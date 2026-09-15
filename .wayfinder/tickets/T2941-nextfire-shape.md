---
id: T2941
title: 固定间隔下次触发的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

周期任务的触发网格与停机补账怎么算？（spec 1870 / effort #1870 / R71）

## Resolution`

**crontab/systemd timer 网格语义纯计算 `NextFireSchedule`
（core/exec）**：nextFireMillis（≥now 最近网格点含上——恰在格点即
到期；now≤epoch 即首触发）+ missedFires（(lastAcked,now] 网格点数——
补账显式）。epoch 钉网格停机不漂移，错过就错过不重贴。局部方法误用
（Java 不支持）实现期自查修正。

