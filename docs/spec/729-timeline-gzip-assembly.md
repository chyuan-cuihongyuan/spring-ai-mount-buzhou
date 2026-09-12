# 729 — 健康时间线 JSONL 压缩线装配

> 来源：G 会话第 30 轮 = effort #729（D 会话装配轮模式）/ [T1009](../../.wayfinder/tickets/T1009-jsonl-gzip-assembly-shape.md) / [T1010](../../.wayfinder/tickets/T1010-jsonl-gzip-assembly-verify.md) / impl 532。

## 背景

RollingJsonlWriter 压缩线（spec 712）只有编程构造——`buzhou.health.timeline.export-compress-from` 声明式入口缺失。

## 目标

- `BuzhouHealthTimelineProperties` 增 `exportCompressFrom`（缺省 0=关；1 非法 fail-fast——file.1 恒明文）。
- `HealthTimelineJsonl(Path, maxBytes, maxHistory, compressFrom)` 构造重载；bean 透传。

## 测试

压缩线声明 → 跨线档 .gz 可解、file.1 明文；缺省 0 全明文；既有用例零回归。

## 兼容性

缺省逐字节不变；便捷构造向后兼容。
