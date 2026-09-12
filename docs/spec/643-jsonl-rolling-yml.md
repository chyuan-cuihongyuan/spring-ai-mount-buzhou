# 643 — JSONL 轮转 yml 装配扩散

> 来源：F 会话第 44 轮 = effort #600（spec 642 的装配面扩散）/ [T936](../../.wayfinder/tickets/T936-jsonl-rolling-yml-shape.md) / [T937](../../.wayfinder/tickets/T937-jsonl-rolling-yml-verify.md) / impl 496。

## 背景

spec 642 的 RollingJsonlWriter 对三个追加导出器默认保护（64MB×3 代），但只有编程面参数——yml 声明式部署无法按磁盘预算调档位或显式关闭。

## 目标

两个有 yml 装配面的导出器补细调键：

- `buzhou.health.timeline.export-max-bytes` / `export-max-history`（BuzhouHealthTimelineProperties 第 5/6 槽）；
- `buzhou.resilience.shadow.detail-max-bytes` / `detail-max-history`（Shadow record 第 6/7 槽，4/5 参便捷构造兼容）。

语义与编程面同口径：**键缺席 = 默认 64MB×3；显式 ≤0 = 关**（无界追加 escape hatch）。装配处透传三参构造。

## 非目标

PromptUsageJsonl 无 yml 装配面（纯编程静态工具）不扩散；时间触发轮转仍非目标（沿 spec 642）。

## 测试

两侧 AutoConfigTest 绑定用例（缺省默认 / 显式 0 关）+ 既有用例零回归。

## 兼容性

键缺席走默认（与 spec 642 默认开一致）；record 便捷构造保留源兼容。
