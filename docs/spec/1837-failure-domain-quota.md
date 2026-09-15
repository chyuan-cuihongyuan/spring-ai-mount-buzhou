# Spec 1837 — 失败域配额（effort #1837，R38）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2875–T2876，impl 1438）。借鉴：
> k8s failure-domain / 供应链「分域配额+中央备货」——单域故障只烧自己的桶，
> 全局保留兜底但总量封顶。

## Problem Statement

总量配额在域间无隔离：一个失败域（实例/机房/供应方）的失败重试风暴可
吃光全局预算，其余健康域被连坐饿死；反过来平均分桶又无弹性（忙域闲域
同额）——「隔离」与「兜底」两难。

## Solution

`FailureDomainQuota`（core/budget，静态纯函数）：

- `admit(domainUsed, domainQuota, reserveUsed, reserveQuota)` → 三态
  `FROM_BUCKET / BORROW_RESERVE / DENY`：域桶先花，桶满借全局保留，
  皆尽拒；
- `census(reserveUsed, reserveQuota, usages)` 域普查：atCap 桶满数 /
  borrowing 借用数 / tightest 最紧域（used/quota 最高，并列取首）/
  reserveUtilization（保留 0 时 -1 哨兵）。

## User Stories

1. 作为预算治理者，b 域借用中（used > quota）+ 保留利用 25% → 单域过载
   在吃备货，其余域零影响——连坐结束。
2. 作为容量规划者，tightest 域常年贴顶 → 该调桶额；保留常年贴顶 →
   总量该扩。
3. 作为框架宿主，域语义（实例/机房/供应方）自声明，纯裁决零记账。

## Implementation Decisions

- 纯裁决不记账（用量归属归宿主）；quota=0 域的最紧比按 used==0 ? 0 : ∞
 （零额零用不算紧、零额有用最紧）。

## Testing Decisions

- 三态准入；普查四读数+并列取首；空表/null 哨兵；负计数/空白域
  fail-fast。首跑红为并行会话编辑窗口（BuzhouMemoryAdvisorTest 主测
  不同步），收尾后绿——跨会话干扰第三次入档。

## Out of Scope

- 不做记账与借用利率（惩罚性借价归未来静脉）；不动态调桶。

## Further Notes

- 与 ElasticBudgetPool 互补：那是弹性池，这是失败域隔离+备货兜底。
