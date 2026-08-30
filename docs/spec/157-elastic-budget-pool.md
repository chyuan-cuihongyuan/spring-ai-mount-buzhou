# Spec 157 — 弹性预算池（effort #107）

> wayfinder map：`.wayfinder107/MAP.md`（T515–T516）。借鉴：Spark AQE
> （运行时空闲资源再分配）——静态配额在负载不均时一半沉睡一半撞墙。

## Problem Statement

会话 token/成本预算静态分配：A 会话（闲）配额沉睡，B 会话（忙）提前撞墙被拒。
池化全部额度又会饿死（忙会话被抢）；Spark AQE 的答案是「保底 + 弹性」——
每个分区有保底，空闲部分运行时借给忙分区。

## Solution

`ElasticBudgetPool`（core/budget）：

- **构造**：总容量 C + 各会话基础配额（Σbase ≤ C 校验 fail-fast）。
- **获取**：`tryAcquire(sessionId, amount)`——
  - 会话内 held + amount ≤ base：保底路径恒可用（base 永不被他人借走）；
  - 超出 base：借用路径——仅当 amount ≤ 剩余可借（C − Σ max(base_i, held_i)）。
- **归还**：`release(sessionId, amount)`（held 递减，surplus 回升）。
- **观测**：`heldOf` / `surplus()` / snapshot（held/base 每会话）；计数
  borrowed / denied。

## User Stories

1. 作为宿主，忙会话自动借用闲会话的沉睡配额——总容量利用率上去，忙会话不再
   提前撞墙；闲会话回来时保底仍在（永不被借穿）。
2. 作为运维，surplus 曲线就是「池还剩多少弹性」——扩容决策依据。
3. 作为宿主，构造期 Σbase ≤ C 校验——配额表本身写错即失败，不带病上线。

## Implementation Decisions

- 池级单锁（分配是小临界区；高并发会话各自 base 判定共享一把锁可接受——
  诚实边界入档）。
- 借走不召回：借用中不中途斩（在飞轮完整性优先，同 superstep 哲学）。

## Testing Decisions

- 保底恒可用（他人借满 surplus 后 base 仍可取）；借 surplus 成功；借用会吃掉
  他人 base 时拒；release 后 surplus 回升可再借；Σbase > C 构造 fail-fast；
  计数。

## Out of Scope

- 空闲 base 缩容；优先级借用；跨实例池；等待式配额。

## Further Notes

- 预算三层：会话硬顶（16）/ 池化弹性（本轮）/ 全局闸（后续）。
