# Spec 424 — 提示词使用统计（effort #424）

> wayfinder map：`.wayfinder/maps/effort-424.md`（T739–T740）。D 会话第 25 轮。

## Problem Statement

PromptRegistry（spec 401）的 resolve 是纯查询——哪个提示词哪个版本被
用到多少次完全不可见。提示词的退役/晋级决策没有数据支撑（Langfuse
prompt analytics 缺位）。

## Solution

`prompt.PromptUsageStats` + `prompt.UsageTrackingPromptRegistry` +
`prompt.PromptUsageJsonl`（418 统计/导出同构镜像）：

- **UsageTrackingPromptRegistry**：PromptRegistry 装饰器——三个 resolve
  形态（latest/标签/钉版）命中即 `stats.record(name, version)`；
  resolve miss / publish / label 不记账；其余方法纯透传。
- **PromptUsageStats**：(name, version) → 次数累计；`snapshot()` 出
  行（name→version 字典序，count 降序无关——字典序可复现）；不清零
  （趋势对比用两次快照差——418 同语义）。
- **PromptUsageJsonl.appendSnapshot(path, stats)**：追加快照行
  {at,name,version,count}；父目录自动创建；写失败上抛。
- yml：`buzhou.prompt.usage-tracking=true` 时 buzhouPromptRegistry
  bean 包装饰器（默认关——原样 InMemory 零行为变化）；PromptUsageStats
  bean 恒在（闲置零成本）。

## User Stories

1. 作为提示词维护者，我想知道各版本被 resolve 的次数， so 晋级/退役
   由使用数据说话（406 退役通告的 usage 面补到提示词域）。
2. 作为运维，我想定期把使用快照落盘外存，so 跨重启的趋势可对比。

## Implementation Decisions

- 装饰器不改 InMemoryPromptRegistry（401 类型不动——加法扩展）。
- 统计是旁路：record 内部异常不外溢（LongAdder 无抛点，诚实注记）。

## Testing Decisions

- 装饰器：latest/标签/钉版三形态命中各记账、miss 零记账、publish/
  label 不记账；透传语义（resolve 结果/标签表/版本史与裸 registry 一致）；
  快照字典序。
- JSONL：两轮追加行数累积、字段齐。
- yml：默认关=bean 非 UsageTrackingPromptRegistry；开=是。

## Out of Scope

- per-label 分桶；导出定时装配；跨实例聚合。

## Further Notes

- 新公共类型 `PromptUsageStats`、`UsageTrackingPromptRegistry`、
  `PromptUsageJsonl`、`BuzhouPromptUsageProperties` 随轮 regenerate 快照。
