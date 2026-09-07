# Spec 140 — 技能使用统计（effort #115）

> wayfinder map：`.wayfinder/maps/effort-115.md`（T465–T466）。借鉴：Backstage
> catalog score / Caffeine hitRate 观测面（目录价值以使用证据说话）。

## Problem Statement

技能目录只增不减：没人用的技能一直占目录注入预算与搜索面，但「谁在吃灰」
没有事实依据——治理（下架/改文案/改绑定）靠感觉。

## Solution

`skill/SkillUsageStats`：per-skill load 成功计数的进程内有界表（1024 封顶，
新名折 `__overflow__`——封顶后唯一例外键）。`LoadSkillTool` 成功路径一行打点
（global 旋钮模式——ErrorSignatures 先例）。`topUsed(n)` 排行（count 降序 +
名字典序稳定）；`unused(catalogNames)` 零使用清单（字典序——治理候选名单）；
`reset()` 窗口清零（export → reset 循环）。

## User Stories

1. 作为技能目录维护者，我按使用排行与零使用清单治理目录，所以下架有证据、
   热门技能的注入优先级有依据。
2. 作为平台运维，我按窗口导出排行再清零，所以每窗口一份「技能热度榜」、
   表永有界。

## Testing Decisions

- 红队：计数累积 + 稳定排序（同 count 字典序）；零使用清单；封顶折入
  （既有继续细分 + overflow 唯一例外键 + 不再增长）；reset 后重新计数；
  空白名拒绝。skills 模块全量回归兜接线面。

## Out of Scope

- search→load 转化漏斗分析；跨实例聚合；持久化使用史。

## Further Notes

- 与 spec 116（skill_search 遥测）组合即得漏斗两面：搜到没有（miss）与
  在册不载（unused）。
