# Wayfinder Map — Buzhou 生效配置指纹（effort #212，B 会话第 35 轮）

> B 会话第 35 轮。工具目录有指纹（175）了，<b>配置面</b>没有——「环境 A 和环境
> B 的 buzhou.* 差在哪」「上次发布后配置被谁改了」不可答。配置漂移是生产事故
> 高频根因。延续 SBOM/锁单思想到配置。

## Destination

ConfigFingerprint（core/config）：of(Map<String,String> 生效键值) → 有序
归一化摘要 + 键级指纹表；diff(other) 三分类（added/removed/changed——值级）；
summaryHex 部署记录锚。供 config-doctor/审计消费。

## Notes

- 号段：B=奇数 spec（本轮 187）；轮次 .wayfinder200+。
- strip 归一（空白差不构成变更——全仓哈希纪律）。
- 键集来源归宿主（Environment/SpringBinder 提取 buzhou.* 前缀）——本类不绑定
  Spring 面。

## Decisions so far

- null 键值跳过（脏输入不炸指纹）。

## Not yet specified

- 与 config-doctor（91）联动输出；定时快照存档。

## Out of scope

- 沿用各轮；secret 值脱敏（归宿主提取时做——本类不猜哪些是敏感）。

## Tickets

- [x] [T559 ConfigFingerprint（归一摘要+键级 diff）](tickets/T559-config-fp.md)（impl-307）
- [x] [T560 配置指纹回归（稳定/归一/三分类/脏输入）](tickets/T560-config-fp-tests.md)（impl-307）
