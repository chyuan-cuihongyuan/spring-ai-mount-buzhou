# 912 — 会话导出 diff 读面

> 来源：I 会话第 13 轮 = effort #912（[T1275](../../.wayfinder/tickets/T1275-export-diff-shape.md) / [T1276](../../.wayfinder/tickets/T1276-export-diff-verify.md) / impl 665）。spec 719 ConfigDiff 的同构扩散（kubectl diff 思想从配置域迁移到会话导出域）。

## Problem Statement

排障三问之「两份导出差在哪」（版本对比 / 迁移前后验证 / bug 包对比）当前靠人眼比对 JSON 文本——字段序/时戳噪声淹没真实差异。spec 719 已为配置域给出 diff 读面（差异结构化、有界、稳定），会话导出域空白。

## 目标

- 新公共纯函数类 `SessionExportDiff`（core.session）：
  - `between(SessionExport a, SessionExport b)`：
    - sessionId 不同 fail-fast（跨会话对比无意义——IllegalArgumentException）；
    - 标量字段差异：version / appId / agentName（`FieldDiff(field, valueA, valueB)`；null→值 亦记）；
    - messages 按 id 对齐三桶：仅 A 有 / 仅 B 有 / 两侧 content 或 role 不同（`MessageDiff(itemId, kind, detailA, detailB)`）；
    - state / extensions：键集差 + 值差（`StateDiff(key, kind, valueA, valueB)`）；
  - 返回 record `DiffReport(boolean identical, List<FieldDiff> fieldDiffs, List<MessageDiff> messageDiffs, List<StateDiff> stateDiffs, List<StateDiff> extensionDiffs)`；
  - 有界纪律：各桶 `MAX_DIFFS=32` 封顶截断（719 同款；identical 判定不受截断影响——先算全量布尔再截断清单）；
  - 纯函数零 IO；不比 exportedAtEpochMs（时戳非内容——710 口径一致）。
- 既有面零变化。

## 兼容性

纯增量新类型，零既有行为变化。
