---
id: T976
title: JSONL 轮转旧档 gzip 压缩的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T975
created: 2026-09-13
---

## Question

压缩线以上真 .gz 且内容可解？file.1 恒明文？默认 0 全明文零回归？静态路径同口径？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 13 轮）：① maxHistory=3 compressFrom=2 轮转两次——file.1 明文存在、file.2.gz 存在且 GZIPInputStream 解压=第一代内容、file.2 明文不存在；② 默认构造（compress=0）轮转全明文（既有用例零回归）；③ 静态 rotateIfNeeded 带 compress 同口径；④ 最老代清理同时清 .gz 与明文形态。`mvn -pl buzhou-core -am test` 全绿。
