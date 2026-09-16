---
id: T3141
title: 冷启动豁免 φ 门的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

豁免窗与 φ 嫌疑怎么合流？（spec 2020 / effort #2020 / R21）

## Resolution

**spec 2004 × 2013 组合件 `GraceAwareFailureDetector`
（core/concurrent，单对端一件）**：心跳喂 φ 且首拍毕业；失败豁免分流
——窗内不喂 φ（冷启动噪声不毒化模型，豁免账在 grace 侧），窗外以
心跳形态喂（超长间隔拉高 φ）；verdict 三态 HEALTHY/GRACE_HOLD（高
嫌疑但豁免中观望）/CONFIRMED（毕业或窗外判失联）——宽容冷启动不
放过真死。
