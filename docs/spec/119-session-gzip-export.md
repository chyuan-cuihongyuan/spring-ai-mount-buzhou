# Spec 119 — 会话级 gzip 导出（effort #81）

> wayfinder map：`.wayfinder/maps/effort-81.md`（T427–T428）。spec 109 的单会话粒度补齐。

## Problem Statement

gzip 导出（spec 109）只有全量/增量入口：单会话归档场景（工单附件、事故单会话
取证）仍要明文导出再手工压缩。

## Solution

`ObservabilityJsonlExporter.exportSessionGzip(sessionId, OutputStream)`：GZIP+UTF-8
Writer 复用 exportSession 管线——解压内容与明文导出逐字节一致。

## User Stories

1. 作为客服/风控，我单会话取证即得压缩件，所以附件体积可控。

## Testing Decisions

- 解压与 exportSession 明文逐字节等值。

## Out of Scope

- eval/AB 导出 gzip；分卷。

## Further Notes

- gzip 三入口族完整：全量 / 增量 / 单会话。
