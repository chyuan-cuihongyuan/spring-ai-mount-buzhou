# 642 — 追加式 JSONL 大小轮转

> 来源：F 会话第 43 轮 = effort #600 / [T934](../../.wayfinder/tickets/T934-jsonl-rolling-shape.md) / [T935](../../.wayfinder/tickets/T935-jsonl-rolling-verify.md) / impl 495。借鉴：Logback `RollingFileAppender`、logrotate 大小轮转。

## 背景

三个追加式观测明细 JSONL 导出器——HealthTimelineJsonl（spec 405）、ShadowComparisonJsonl（spec 309）、PromptUsageJsonl（spec 424）——均无大小保护：逐行追加永不停止，长期部署磁盘撑爆是确定性风险（旁路观测放大主链故障违背其设计语义）。

## 目标

`core.fs.RollingJsonlWriter`（长驻 AutoCloseable）+ 静态轮转检查：

- **触发**：当前文件字节数 + 待写行字节 > maxBytes（内存记账，打开时以现存文件大小初始化——不每次系统调用）。
- **动作**：close writer → 代际 shift（`file.(h-1) → file.h` 自高向低，最老删除）→ `file → file.1` → 重开 APPEND。
- **默认开**：`DEFAULT_MAX_BYTES = 64MB`、`DEFAULT_MAX_HISTORY = 3`（≈256MB/文件组封顶）——资源保护是缺陷补全非行为变化；既有测试体量远低于阈值不破。`maxBytes`/`maxHistory` ≤ 0 = 显式关（旧无界语义 escape hatch）。
- **降级**：轮转 IO 失败 best-effort——重开原文件继续写（不阻断主链），`rotations()` / `rotationFailures()` 计数可观测。
- 接入：HealthTimelineJsonl / ShadowComparisonJsonl（长驻，内部换 RollingJsonlWriter，既有单参构造 = 默认参数委托）；PromptUsageJsonl（逐次开写，`appendSnapshot` 增 4 参重载，append 前静态轮转检查）。

## 非目标

yml 细调键（各机制 `max-bytes`/`max-history` 声明）留装配扩散轮；时间触发轮转（logrotate cron 语义）不做——大小触发已覆盖磁盘保护主诉求。

## 测试

小阈值 @TempDir：触发轮转（file.1 生成、原文件只含新行、行完整）；多轮代际封顶（.4 不存在）；0=关无轮转；PromptUsage 重载生效；既有三类用例零回归。

## 兼容性

默认参数对既有调用方 = 保护补全（64MB 前行为逐字节一致）；显式 ≤0 回到无界。
