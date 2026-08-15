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

## §B 消息读失败降级（T156 / impl-127）

### Problem Statement

消息历史读失败（DB 瞬断/Redis 抖动）直接炸掉整个 Turn——会话在存储恢复前完全不可用。
可用性敏感的生产部署需要「读降级续聊」选项：历史读不到时以空历史继续，会话保活。

### Solution

可配读降级策略 `buzhou.store.read-degrade`：`off`（默认，读失败原样上抛——行为不变）与
`empty`（读失败降级空历史继续本轮）。降级必须可感：WARN 日志 + `buzhou.stores.read-degraded`
（outcome=empty）计数，绝不静默。

### User Stories

1. As a SRE, I want 读失败时可配「空历史续聊」保活, so that DB 瞬断不会让全部会话同时失败。
2. As a SRE, I want 降级发生可观测（日志+计数）, so that 「模型看不到历史」的异常状态不被静默吞掉。
3. As a 应用开发者, I want 默认策略保持读失败上抛, so that 升级零语义变化。
4. As a 应用开发者, I want 非法策略值启动即拒, so that 配置错误不延后暴露。

### Implementation Decisions

- `ReadDegradePolicy`（OFF/EMPTY）公共枚举 + `ReadDegradeHolder` 全局默认（Holder 模式同
  ToolResultLimiterHolder；进程内全部 BuzhouChatMemory 实例共享）。
- `BuzhouChatMemory` 三处历史读（get/seedTurn/seedSeq）统一路由 `loadHistory`：EMPTY 时捕获
  读异常 → WARN + 计数 + 空列表；OFF 原样上抛。
- `BuzhouCoreProperties.Store` 增 `readDegrade`（兼容 2 参构造；canonical @ConstructorBinding）；
  非法值构造期 fail-fast。auto-config 以独立初始化 bean 把策略写入 Holder（memory/jdbc/redis
  任何 store 形态都生效）。
- 诚实边界：EMPTY 下模型看不到历史，回答质量下降是预期代价（runbook 记调优指引）。

### Testing Decisions

- core 单测（永远读失败的 store 替身）：OFF 默认上抛不变；EMPTY 时空历史返回 + 写路径不受影响。
- Prior art：InMemoryMessageStoreTest 同款替身风格。

### Out of Scope

- 摘要/状态读失败降级（各自模块的独立策略面，量级不抵复杂度）。
