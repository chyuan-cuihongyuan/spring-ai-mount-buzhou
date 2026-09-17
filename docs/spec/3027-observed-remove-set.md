# Spec 3027 — OR-Set 观察删除集（effort #3027，R28）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5055–T5056，impl 2028）。
> 借鉴：CRDT Observed-Remove Set（Shapiro et al. 2011）。

## Problem Statement

多副本集合（会话标签/协作标注/多端收藏）并发「一端删、一端加」
时，普通集合要么删胜丢加、要么加胜复活已删——缺一个免协调且语
义明确的收敛口径。

## Solution

`ObservedRemoveSet<T>`（core/concurrent，单副本线程口径）：

- add 带唯一标签 Tag(replica, seq)；remove 只墓碑**当前观察到**
  的标签——并发新标签不受影响 → **add 胜**（显式语义）；
- `merge(other)` 标签并+墓碑并（幂等、交换、只读对侧）；
- contains/elements/size 按未墓碑标签判定；replicaId 非空校验。

## User Stories

1. 作为协作作者，一端删一端并发加——合并不丢不复活，语义可诺。
2. 作为多端作者，任意顺序 merge 收敛一致——免中心协调。

## Testing Decisions

- 增删基础+删不存在无副作用；顺序重加胜；**皇冠场景**（A 加→
  同步→B 删→A 再加→互相同步→x 幸存）；全量同步后删除两副本
  皆清；merge 幂等；merge 交换律（元素集相等而副本号各异）；
  多元素隔离；replicaId 空/null fail-fast。

## Out of Scope

- 不做因果标签压缩（版本向量压缩留白）；不做并发 remove 语义
  参数化（remove-wins 变体留白）；不做持久化/网络传输（归
  调用方）；不做线程安全。

## Further Notes

- 与 LastWriteWinsRegister（值域）/ ReplicatedCounter（数域）成
  CRDT 三形态：集域本件补位。
- 里程碑：28/150。
