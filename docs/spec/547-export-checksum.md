# Spec 547 — 会话导出校验和（effort #547）

> wayfinder map：`.wayfinder/maps/effort-547.md`（T853-854）。E 会话第 47 轮。

## Problem Statement

510 密文封缄护密文通道——**明文导出**（分享给可信方）的传输/存储衰变
无证据：损坏 JSON 可能仍可解析但内容已变（校验和是更强证据）。

## Solution

`session.SessionExportChecksum`（静态原语，S3 checksum 同 doctrine）：

- of(exportJson) → "sha256:<hex>"（版本化前缀）。
- verify(exportJson, checksum) → 一致 true / 不一致、格式不符、null
  false（fail-closed）。
- 明文导出文件旁写校验和，导入前验校。

## User Stories

1. 作为集成方，我想明文导出附带校验和并在导入前验校， so 传输衰变在
   语义解析前被发现。

## Implementation Decisions

- 防衰变/误写，不防蓄意同改（蓄意归 510 密文封缄——分层诚实）。

## Testing Decisions

- of/verify 往返一致；内容变校验和不变 false；格式不符/null false；
  空 JSON fail-fast。

## Out of Scope

- 防蓄意同改；签名。

## Further Notes

- 无新顶层公共类型? 有——`SessionExportChecksum`（工具类）随轮
  regenerate 快照 + api-surface.md 加行。
