# 843 — 证据引用失效率读数

> 来源：H 会话第 44 轮 = effort #843 / [T1187](../../.wayfinder/tickets/T1187-evidence-ref-validity.md) / [T1188](../../.wayfinder/tickets/T1188-evidence-ref-validity-verify.md) / impl 596。
> 借鉴：S3 presigned URL 时限校验思想。

## Problem

证据引用指向的 spill 内容可能已被清扫/删除：模型回读时才断链（404）——「引用失效率多高/哪些断了」无对账面。

## Solution

`EvidenceRefValidity`（spill，纯函数）：

- **对账**：audit(referencedUris, existence 谓词)——失效率+失效样本典序封顶 16。
- **解耦**：存在性语义注入（文件存在/句柄可解析均可）；null/空白 URI 忽略。

## 兼容性

纯新增静态工具；EvidenceRefLedger 零变更（包私有边界保持）。

## 诚实边界

不碰文件系统（谓词注入）；样本封顶；时点对账。
