---
id: T3018
title: 雪花 ID 分解的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3017]
created: 2026-09-23
---

## Question)

编解码在 roundtrip/越界下正确吗？（spec 1908 / effort #1908 / R109）

## Resolution`

**SnowflakeIdDecomposeTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=SnowflakeIdDecomposeTest）：roundtrip 三例（零序列/满序列
4095/跨机器）；分解字段精确断言；越界三型 fail-fast。
