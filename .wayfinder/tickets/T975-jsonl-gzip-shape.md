---
id: T975
title: JSONL 轮转旧档 gzip 压缩的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

RollingJsonlWriter（spec 642）代际 shift 全为明文——256MB/文件组的观测明细（JSONL 压缩比 ~10:1）盘上占满明文。logrotate compress + delaycompress 怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 13 轮 = effort #712 / spec 712 / impl 515）：`compressFromGeneration` opt-in 参数（默认 0=关，既有三构造零变化）——代际 ≥ N 的历史档存 gzip（`file.N.gz`）；**file.1 恒明文**（delaycompress：最新代仍可 tail/grep 热查）。shift 语义：`.1→.2` 跨压缩线时明文→gzip 转码（GZPOutputStream 写 target.gz + 删源）；`.gz→.gz` 代际纯 rename；最老代删除清两种形态；压缩线以上源必为 gz（单调性——compressed→plain 不可能路径）。实例 rotate() 与静态 rotateIfNeeded 同口径（新重载，旧签名默认 0 委托）；gzip 失败计入 rotationFailures best-effort 降级（与轮转失败同通道）。指标复用 METRIC_ROTATED。借鉴 logrotate compress+delaycompress。
