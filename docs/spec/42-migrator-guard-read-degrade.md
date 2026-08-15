# Spec 42 — 迁移防护与读失败降级（effort #9）

> effort #9 主线 spec。§A：SchemaMigrator 防护（T155）；§B：消息读失败降级（T156）。

## §A 迁移器防护：未来版本拒绝 + checksum（T155 / impl-126）

### Problem Statement

SchemaMigrator 只应用 `version > maxApplied` 的脚本：旧构建对上新库（maxApplied 大于本构建最新
脚本）静默通过——以旧 schema 写新库，损坏不告警；版本表不存脚本 checksum——已应用脚本被事后
修改无法发现，「改脚本不改版本号」的漂移静默发生。

### Solution

Flyway validateOnMigrate 等价语义：迁移前校验——库中已应用最大版本大于本构建最新脚本 →
拒绝迁移（未来版本）；版本表新增 checksum 列（脚本内容 sha256），已应用脚本与记录不符 →
拒绝迁移；存量 NULL 行首次升级回填锚定。

### User Stories

1. As a SRE, I want 旧构建对上新库被拒绝并给出明确原因, so that 降级部署不会静默写坏 schema。
2. As a 框架开发者, I want 已应用脚本被改动可检出, so that 脚本不可变纪律有机器强制。
3. As a SRE, I want 存量库升级零摩擦, so that 防护上线不需要人工迁移版本表。
4. As a 框架开发者, I want 新增变更继续走新版本号, so that 既有迁移流程不变。

### Implementation Decisions

- 版本表 DDL 增 `checksum VARCHAR(64)` 列；存量表由 `ensureVersionTable` 探测缺列后 `ALTER
  TABLE ADD COLUMN`（幂等；H2/PG/MySQL 元数据大小写两形态探测）。
- `applyPending`：maxApplied > 最新脚本版本 → `IllegalStateException`（消息含两版本号与方言）。
- `validateChecksums`：逐脚本比对记录行；NULL/空 → 回填当前脚本 sha256；不等 → 拒绝（消息含
  记录值与期望值）。新应用行在 `apply()` 内随脚本 sha256 一并写入（脚本+版本行+checksum 同事务）。
- 基线行（旧库采纳）同样写 checksum（锚定其后漂移可检）。
- 诚实边界：MySQL DDL 隐式提交的既有原子性洞不变（javadoc 已自认，靠脚本幂等兜底）。

### Testing Decisions

- H2 内嵌（无 Docker）：未来版本行 V999 → 拒绝；篡改 V2 checksum → 拒绝；恢复 NULL → 回填并通过；
  既有四用例（空库全量/幂等/基线/恢复设施）不回归。

### Out of Scope

- MySQL 半应用 DDL 修复工具（靠幂等脚本，既往边界）。

## §B 消息读失败降级（占位，T156 落地时补全）
