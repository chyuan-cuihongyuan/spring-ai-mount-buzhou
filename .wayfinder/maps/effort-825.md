# effort #825 — 会话迁移对账

- 会话：H 会话 800 系第 26 轮 ｜ spec [825](../../../docs/spec/825-migration-reconciliation.md) ｜ 票 [T1151](../tickets/T1151-migration-reconciliation.md)/[T1152](../tickets/T1152-migration-reconciliation-verify.md) ｜ impl578
- 借鉴：gh-ost 在线迁移行数对账（github/gh-ost ≈16K star）——「搬完」≠「搬对」

## 勘察（排重）

- SessionMigrator（38）：export+import 管线——无迁移后对账。
- SessionExportChecksum：导出完整性校验（自校验）——跨导出对账不同面。
- grep -i `reconcil|对账`：无命中。

## 决定

`MigrationReconciliation`（core.session，纯函数）：verify(sourceExport, targetExport)——四维：消息计数/轮次范围/首尾消息 id（仅同 sessionId 即 keepIds 时比对——重映射语义跳过）/状态键数+缺失键明细（封顶 8+汇总行）；countsMatch 总判定。null 导出 fail-fast；null 集合按 0 计。

## 测试

全对（countsMatch+migrations 轮次数 2）/计数不符「源 2 vs 目标 1」/轮次漂移/状态键数+缺失键列表/重映射跳过 id 比对/空导出零安全——6 例绿（修复三连：缺 import×2、contains 链式误用、断言子串与消息格式不符）。

## 诚实边界

只读对账不修复（修复=重迁移）；首尾 id 比对仅 keepIds 语义下有效（重映射 sessionId 不同即跳过——诚实口径）；摘要/spill 域不在对账（消息与状态是迁移主ayload）。
