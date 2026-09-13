# 911 — JCS 规范化内容指纹

> 来源：I 会话第 12 轮 = effort #911（[T1273](../../.wayfinder/tickets/T1273-jcs-fingerprint-shape.md) / [T1274](../../.wayfinder/tickets/T1274-jcs-fingerprint-verify.md) / impl 664）。借鉴：RFC 8785 [JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785)——指纹绑定内容而非序列化键序。

## Problem Statement

`contentFingerprint`（spec 710）反序列化到 LinkedHashMap（保序）再序列化求和——嵌套 `Map`（message metadata / state value / extensions）的键序取决于原始生成方。跨实现/跨语言重放同一内容时键序漂移 → 指纹不同 → 协商误判「内容已变」。RFC 8785 证明：指纹必须定义在规范化形态上。

## 目标

- `SessionExportChecksum.canonicalContentFingerprint(SessionExport)`：
  - 内容投影同 spec 710（剔除 exportedAtEpochMs）；
  - **递归键排序规范化**：Map → TreeMap（字典序）逐层展开（值内 List 元素保序——数组有序是语义，RFC 8785 同口径）；
  - 诚实边界：数字按 Jackson 文本原样（不做 RFC 8785 §3.1 数字矩阵完整实现——Java 生态内单产单消场景数字文本稳定，入档）；
  - 新前缀 `sha256-j:`（与保序版 `sha256-c:` 显式区分——710 前缀防混用纪律）；
- 既有 `of` / `verify` / `contentFingerprint` 逐字节不动（既有协商状态兼容）。

## 兼容性

纯增量：公共类新增静态方法 + 常量，零既有行为变化。
