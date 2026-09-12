---
id: T876
title: gzip 导出压缩档位的口径裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

nginx gzip_comp_level：压缩档位是 CPU 与体积的旋钮——大体量热导出省 CPU 用低档、冷归档求体积用高档。本仓三个 gzip 导出重载全用 JDK 缺省档。口径怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 14 轮 = effort #600 / spec 613 / impl 466）：

1. 三个 gzip 导出（全量/增量/单会话）各增带 `compressionLevel` 重载；无参重载保持 Deflater 缺省（-1，零行为变化）。
2. level ∈ {-1（默认）} ∪ [0,9]（Deflater 语义：0 不压缩 … 1 BEST_SPEED … 9 BEST_COMPRESSION）；越界 fail-fast。
3. 实现经匿名子类 `def.setLevel`（构造后首写前设置——protected 字段唯合法通路）。
4. 不接 yml（导出是 API 调用面，档位随调用而非全局配置）。
