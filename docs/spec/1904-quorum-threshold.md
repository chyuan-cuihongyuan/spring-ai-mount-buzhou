# Spec 1904 — 票数下限判定（effort #1904，R105）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3009–T3010，impl 1505）。借鉴：
> 分布式共识票数下限（Paxos/BFT 经典结论）——多数派 = ⌊N/2⌋+1
> （崩坏容错 N ≥ 2f+1）；拜占庭容错 = N ≥ 3f+1（⌊(N−1)/3⌋ 个坏票
> 可容）。共识配置的票数账一行公式两档语义。

## Problem Statement

投票/共识配置拍脑袋：N 个投票者能容几个坏节点、容 f 个坏节点要
多少投票者——崩溃容错与拜占庭容错两档混淆（拜占庭下 2f+1 远远
不够），缺一行算清的判定面。

## Solution

`QuorumThreshold`（core/transaction，静态纯函数）：

- `majority(voters)`：⌊N/2⌋+1——简单多数票数下限；
- `byzantineTolerance(voters)`：⌊(N−1)/3⌋——该规模可容忍的拜占庭
  坏票数；
- `byzantineSize(faults)`：3f+1——容忍 f 个拜占庭坏票所需的最小
  投票者数。

## User Stories

1. 作为共识配置者，3/5 投票者多数派 = 2/3——配置前有数。
2. 作为安全评审者，4 投票者拜占庭容忍 = 1 坏票——「2f+1 想容
   拜占庭」的错误被公式拦住。
3. 作为扩容规划者，容 2 坏票拜占庭 → 至少 7 投票者——扩容预算
   直读。

## Implementation Decisions

- 纯函数零状态；voters ≥ 1、faults ≥ 0 fail-fast；整数除法向下
  取整（诚实下限）。

## Testing Decisions

- 多数派三例（3/4/5 → 2/3/3）；拜占庭容忍三例（3/4/7 → 0/1/2）；
  最小规模两例（f=1→4、f=2→7）；畸形两型 fail-fast。

## Out of Scope

- 不做共识协议执行（归调度/存储层）；不做签名验证。

## Further Notes

- 与 QuorumConsistency（R77 读写副本交集）互补：那是副本读写
  配置，这是投票者票数下限。
