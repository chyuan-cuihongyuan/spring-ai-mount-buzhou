# 733 — 导出协商联动补验

> 来源：G 会话第 34 轮 = effort #733（spec 710 补验）/ [T1017](../../.wayfinder/tickets/T1017-export-negotiation-e2e-shape.md) / [T1018](../../.wayfinder/tickets/T1018-export-negotiation-e2e-verify.md) / impl 536。

## 背景

SessionExportConditional 单元面已验（spec 710）——与 toJson/fromJson 往返、整体校验和、归档 checksum 的**组合链路**未闭环。

## 目标（测试域补验轮）

- 导出 → toJson → fromJson 往返后内容指纹稳定（往返不漂）；
- 协商 UNCHANGED → 下轮内容真变 → EXPORTED 且新指纹 ≠ 旧（两轮周期同步脚本语义）；
- contentFingerprint 与 SessionExportChecksum.of（整体）在往返后各自幂等。

## 测试

往返稳定/两轮协商周期/双校验和幂等——三组组合用例。

## 兼容性

纯测试域增量。
