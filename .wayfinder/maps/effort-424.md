# Wayfinder Map — Buzhou 提示词使用统计（effort #424，D 会话第 25 轮）

> D 会话第 25 轮（#401 扩散轮）。勘察：PromptRegistry（401）resolve 三
> 形态（latest/标签/钉版）纯查询——**零使用面记录**：哪个提示词哪个版本
> 被用到多少次不可见（Langfuse prompt analytics 缺位）。退役/晋级决策
> 没有数据支撑（406 退役通告的 usage 面在提示词域是空白）。

## Destination

`prompt.PromptUsageStats`（418 SecretHitStats 同构）+ `prompt.
UsageTrackingPromptRegistry`（装饰器——resolve 命中记账，其余全透传）
+ `prompt.PromptUsageJsonl.appendSnapshot(path, stats)`（快照追加
{at,name,version,count}——418/421 同族）：

- 装配：`buzhou.prompt.usage-tracking=true` 时 buzhouPromptRegistry
  bean 包装饰器（stats bean 恒在零行为；默认关=原样 InMemory）。

## Notes

- 号段：spec 424 / T739–T740 / impl-397。
- 借鉴源：Langfuse prompt analytics（用次数驱动版本治理）+ 418 统计/
  导出同构镜像。
- 纪律：resolve miss 不记账；publish/label 不记账（不是使用）；快照
  字典序（name→version）可复现；写失败上抛（导出显式动作该红）。

## Out of scope

- per-label 解析计数分桶（版本已可分辨）；导出定时装配（宿主组合）；
- 跨实例聚合（InMemory 域——401 诚实边界同款）。

## Tickets

- [x] [T739 使用统计装饰器与 holder](../tickets/T739-prompt-usage-decorator.md)
- [T740 JSONL 导出+yml 装配](../tickets/T740-prompt-usage-jsonl-assembly.md)
