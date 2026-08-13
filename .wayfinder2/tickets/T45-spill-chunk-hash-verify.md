---
id: T45
title: spill · 内容寻址 chunk hash 回读校验
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

evidence-id 已是内容寻址句柄，但**切片级**回读如何证明「与你溢出的同字节」？事实源：git/git（62,540★ 概念锚点：对象名即内容 hash、读回重算必校验 whole-object CA；切片级需自建摘要表——类 Merkle：叶=chunk、root 进 evidence-id）。

## 待定决策（研究推荐已备）

1. spill 落盘时除 whole-content hash 外记录 **chunk 摘要**（每切片 sha256，可选 Merkle root）——采纳。
2. 回读响应 envelope 附 `{data, byteRange, chunkSha256, handleRoot}`；校验失败走**既有读侧 lenient（warning）/ 写侧 strict 非对称**——采纳。
3. Merkle root 是否进 evidence-id 本体（格式演进兼容旧 handle）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §4.3（工作量小–中，ROI 中高：腐化/TOCTOU 可检测，evidence-id 闭环）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §spill-17**（用户常设授权 2026-08-14 ratify、可推翻）。chunk sha256（可选 Merkle root）进回读 envelope；校验失败走读写非对称。
