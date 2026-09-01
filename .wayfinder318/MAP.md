# Wayfinder Map — Buzhou 会话扰乱预算（effort #318，C 会话第 19 轮）

> C 会话第 19 轮。计划性维护/缩容要排水会话——排多少没有预算面：全排光
> 服务中断（K8s PodDisruptionBudget 对「主动扰乱」的同款问题）。

## Destination

`SessionDisruptionBudget`（core.session，K8s PDB 思想）：ACTIVE 数 -
min-available = 可主动扰乱额度；tryAcquireDisruption 原子预留（排水前领额度，
complete 归还）；yml `buzhou.session.disruption-budget.min-available`（默认 0
= 不限，不装配零变化）。宿主维护排水前领额度——预算守门不替代排水本身。

## Notes

- 号段：spec 318 / T627–T628 / impl-341。
- ACTIVE 计数源 = SessionIndexStore（分页枚举计数；无索引 = 不装配）。

## Decisions so far

- 预算口径：available - reserved &gt; minAvailable 才放行（预留制防并发超发）。
- 非主动扰乱（会话自身异常终结）不占额度——预算只管 voluntary。

## Out of scope

- 自动排水执行器（额度是守门面，执行归宿主/维护轮）；跨实例预算（共享族
  已清——预算语义进程内即部署单元内）。

## Tickets

- [x] [T627 SessionDisruptionBudget（额度/预留/归还）](tickets/T627-pdb.md)（impl-341）
- [x] [T628 yml 装配 + 回归](tickets/T628-pdb-close.md)（impl-341）
