# 935 — ExportManifest 规范化摘要

> 来源：I 会话第 35 轮 = effort #935（[T1313](../../.wayfinder/tickets/T1313-manifest-canonical-shape.md) / [T1314](../../.wayfinder/tickets/T1314-manifest-canonical-verify.md) / impl 687）。spec 911 JCS 思想向 manifest 域扩散（RFC 8785 延续）。

## 背景

`ExportManifest.add` 的摘要基于原始 JSON 文本 strip——嵌套键序漂移（跨实现搬运）会让同内容 manifest 误报 mismatch（verify 三列校验的 false positive）。

## 目标

- `SessionExportChecksum.canonicalJson(String)` 包级静态提为单点规范化实现（911 的 canonicalize 逻辑收口，canonicalContentFingerprint 复用）；
- `ExportManifest.addCanonical(sessionId, contentJson)`：规范化后 sha256 登记（与既有 `add` 并存）；
- verify 语义不变（同批登记同一方法即自洽——混用新旧登记由运维纪律约束，入档）；
- 既有 `add` 行为逐字节不变。

## 兼容性

纯增量：新方法 + 包级可见性调整（同包内单点复用）；既有行为零变化。
