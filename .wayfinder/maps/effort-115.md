# Wayfinder Map — Buzhou 技能使用统计（effort #115，A 会话第 10 轮）

> A 侧编号策略沿用 #111 声明。借鉴 Backstage catalog score / Caffeine 观测面。

## Destination

「目录里什么在被用、什么在吃灰」的事实表：load 成功打点 + 排行 + 零使用
清单——目录治理（下架/改文案/改绑定）的证据面。

## Notes

- LoadSkillTool 成功路径一行接线（global 旋钮模式——ErrorSignatures 先例）；
  封顶折 __overflow__（新技能名不再扩张，overflow 是唯一例外键）；reset 窗口
  清零与 export → reset 循环同纪律。

## Decisions so far

- [SkillUsageStats](../tickets/T465-skill-usage.md) — recordLoad/topUsed/
  unused(catalog)/reset + 全局旋钮。

## Not yet specified

- search 命中与 load 转化漏斗（spec 116 搜索遥测与本表组合分析）。

## Out of scope

- 跨实例聚合（B 侧舱表族正交）；持久化使用史。

## Tickets

- [x] [T465 技能使用统计](../tickets/T465-skill-usage.md)（impl-282）
- [x] [T466 收口提交](../tickets/T466-skill-usage-close.md)（impl-282）
