# Spec 3029 — Q 会话 R30 对账轮（effort #3029，R30）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5059–T5060，impl 2030）。
> R6k 对账轮第五例（Wave 5 收口）。

## Problem Statement

Wave 5（R25–R29）新增 5 个公共类型（GrubbsOutlier / RabinKarpSearch /
GaussianSampler / ObservedRemoveSet / ClockEviction）未入快照——
快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1075→1080（+5 全 Q 系 Wave 5，reactor 全量
regenerate）+ api-surface.md 五行（concurrent/cache/metrics/policy/
eval 五段落位）+ CONTEXT 974→979 + 全仓 mvn verify 三门绿 + push。

## Further Notes

- 里程碑：30/150（20%——五分之一达成）。Wave 5 含两处提交前
  拦截修正（Grubbs 表长双写、CLOCK 进入位语义变体）——rc 门禁
  连续五波生效。
