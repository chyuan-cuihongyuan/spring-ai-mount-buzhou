---
id: T3129
title: 键压缩日志语义的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

状态日志多版本读取的终态语义与删除语义怎么定？（spec 2014 / effort #2014 / R15）

## Resolution

**Kafka log compaction 纯函数 `KeyCompaction`（core/recovery）**：
同 key 取 maxSeq（乱序幂等——保留者与输入顺序无关）+payload null 即
tombstone 墓碑删除（墓碑后更高 seq 可复活、旧墓碑不遮新值）+同 seq
后见者退化口径 + CompactionResult 终态/tombstones/superseded/
compactionRatio 对账面。
