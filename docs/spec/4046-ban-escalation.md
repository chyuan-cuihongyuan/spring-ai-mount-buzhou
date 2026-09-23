# Spec 4046 — fail2ban 封禁递升（effort #4046，R47）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6093–T6094，impl 2147）。
> 借鉴：fail2ban maxretry/findtime/bantime.increment（惯犯递升）。

## Problem Statement

反复滥用者处置的病：固定惩罚（惯犯与初犯同价——违法成本
递减）或无窗全量计数（陈年旧账永不洗白）——**滑窗判禁 +
按前科递升面**缺失。

## Solution

`BanEscalation`（core/policy）：

- 滑窗判禁：findTime 窗内失败 ≥ maxRetry 即禁（陈账按窗
  过期自然洗白——无窗全量计数的病解）；
- 递升：禁期时长 = base × multiplier^(banCount−1)，封顶
  maxBanTime——惯犯成本递增（fail2ban bantime.increment
  同思想）；
- 禁期语义：禁期内再失败不计数不复禁（封锁即足够）；解禁
  后失败清零重计；禁即清失败窗（fail2ban 同语义）；
- 时钟注入：时间由调用方传入（确定性可回放，无系统时钟
  依赖）；
- 读数面：banCountOf / isBanned；Verdict(banned, until,
  count, duration)；
- fail-fast：maxRetry<1 / findTime≤0 / baseBan≤0 /
  multiplier<1 / maxBan<base。

## User Stories

1. 作为工具闸门作者，反复越权者禁期递增——惯犯成本递增。
2. 作为审计作者，同事件序列同裁决（时钟注入确定性回放）。

## Testing Decisions

- 满限禁 + 窗滑动豁免（旧账过期）；递升三连（base→×mult→
  ×mult² 封顶 maxBan）；禁期失败不复禁不解窗；解禁重计；
  独立 offender 互不牵连；定构畸形 fail-fast。

## Out of Scope

- 不做正则日志扫描（fail2ban 的 filter 面）；不做跨进程
  共享禁令（本地语义件）；不做白名单/ignoreip。

## Further Notes

- 与 RetryStormSentinel（重试占比风暴判定）互补：出向重试
  风暴 vs 入向滥用禁递升。Wave 8 收口件。
- 里程碑：47/50。
