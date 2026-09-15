# Spec 1136 — SemanticSkillRanker 排序分布深化（J 会话——工件由 L 会话 R50 吸收补登）

> wayfinder map：`.wayfinder/maps/effort-1000.md`（J 会话 1000 系）。
> **补登说明**：J 会话已把本能力的 README 纵深行合入 main
> （「排序调用/跳过/降级三计数显形」），但 spec 文件与配套代码尚未随行
> ——覆盖门因此红。L 会话 1700 系 R50 收口审计按吸收补登纪律依 README
> 行登记本承诺面；**J 会话落位真件时以 J 原件为准**（双 added 冲突取
> J 方，1133 先例）。

## Problem Statement

SemanticSkillRanker 现有 bypassed（降级回退）单计数：排序调用总数、
跳过数（无需排序的路径）与降级数三桶分布缺位——「排序器在多大比例的
路径上真正工作」不可见，嵌入面异常只能靠 bypassed 单点哨戒。

## Solution（J 会话承诺面，代码落位前以 J 会话后续提交为准）

排序分布三计数：`invocations`（排序调用）/ `skipped`（跳过——单候选或
禁用路径）/ `degraded`（降级——嵌入失败回退原序，即现 bypassed 语义），
守恒 invocations = skipped + 实际排序数；stats()/resetForTest() house 惯例。

## User Stories

1. 作为技能治理者，skipped 占比高 → 排序器在空转，查装配条件。
2. 作为嵌入面哨戒者，degraded 持续增长 → 嵌入服务异常先于业务投诉显形。

## Implementation Decisions

- 三桶 AtomicLong 静态面（J 会话族惯例）；排序返回语义逐位不变。

## Testing Decisions

- 守恒断言 + reset 归零 + 降级路径计数（J 会话后续轮验证）。

## Out of Scope

- 不改排序算法；不做按技能细分。

## Further Notes

- 本件为门绿补登件：只陈述 main 上可验证的事实（README 行 + 号段账目）
  与 J 会话承诺面，不虚构实现细节。
