# Spec 164 — 输入侧 PII 命中统计接线（effort #126）

> wayfinder map：`.wayfinder126/MAP.md`（T517–T518）。spec 144 fog「输入侧
> 钩子同款接线」收口。

## Problem Statement

PiiHitStats 只在输出侧钩子打点——用户输入侧（误贴身份证/邮箱的入口面）命中
不进报表，合规面缺了「数据从哪进来」的一半。

## Solution

`PiiInputRedactionHook` 镜像输出侧双点接线：内置命中随既有 matches 循环
`record(type)`；自定义命中经 `PiiHitStats.extractCustomRuleNames(redacted)`
（新静态工具——从占位符提取自定义规则名、内置类型名剔除防双计，双钩共用）
`recordCustom`。输入/输出同表：同一类型两侧都炸时一张报表可见。

## User Stories

1. 作为合规审计员，输入侧命中也进排行，所以「敏感数据从用户入口渗入」的
   频度有据可查。
2. 作为规则维护者，输入侧自定义规则命中可见，所以规则在入口面的有效性
   不再是盲区。

## Testing Decisions

- 红队：两次输入（手机号×1 / 邮箱×2）后 countOf 与 top 断言（install/create
  隔离 + finally 清理）。guard 模块全量回归。

## Out of Scope

- 分表（输入/输出列拆分）；报表导出。

## Further Notes

- 至此 PII 观测三面齐：metrics per-type 计数（双侧）/ PiiHitStats 排行（双侧）/
  健康段待 fog。
