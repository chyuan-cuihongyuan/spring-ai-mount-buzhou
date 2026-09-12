---
id: T947
title: JSONL 轮转指标化的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T946
created: 2026-09-13
---

## Question

轮转发生/失败真发指标？tag 带文件名？默认 no-op 零干扰？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 49 轮）：① install 收集 metrics 后小阈值触发轮转 → `buzhou.jsonl.rotated` 计数 = 轮转次数（tag file 命中）；② rotateIfNeeded 静态路径同发；③ 既有 RollingJsonlWriterTest 零回归（no-op 默认下行为不变）；④ `mvn -pl buzhou-core -am test` 全绿。
