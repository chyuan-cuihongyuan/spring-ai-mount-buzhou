# Spec 4002 — Space-Saving 频繁项 top-k（effort #4002，R3）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6005–T6006，impl 2103）。
> 借鉴：Metwally 2005 Space-Saving；ClickHouse sumKMorQ 系内建。

## Problem Statement

MisraGries 找候选只给「计数 ≤ 真值」的下界读数——**名单之外还要
名次与读数**（热门工具榜/高频错误榜）需要二次精扫补账。

## Solution

`SpaceSavingTopK`（core/metrics，k 槽精确计数器 + 淘汰继承）：

- 在榜键精确 +1；满榜新键淘汰最小计数者、新键**继承其计数 +1**
  （最小计数即误差界）；
- 保留者计数恒 ≥ 真值（单侧高估），真 top-k 超阈值全召回；
- top() 计数降序、并列先入榜先（确定性）；minCount 误差界读数；
  totalObservations 守恒账。

## User Stories

1. 作为观测作者，热门榜读数即答案——免二次精扫。
2. 作为对账审计者，单侧高估 + 守恒账 + 误差界三合同可测。

## Testing Decisions

- 少容量三键精确 + 榜单排序 + 守恒；k=2 淘汰继承链（a 精确幸存、
  e 继承 ≥ 真值、被淘汰键读零、minCount 累积）；搅局流热键三席
  全保 + 恒在榜精确；畸形三型 fail-fast（capacity 0/null×2）。

## Out of Scope

- 不做加权观测（observe(String) 单位点）；不做合并/decay；
  不做并行槽。

## Further Notes

- 素描族三件按问句选型：要名单+名次（本件）/要候选（MisraGries）/
  要任意键点查（CountMinSketch）。
- 里程碑：3/50。
