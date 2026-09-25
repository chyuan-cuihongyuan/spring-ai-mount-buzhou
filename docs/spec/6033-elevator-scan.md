# Spec 6033 — Elevator Scan 电梯扫掠（effort #6033，T34）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6267–T6268，impl 2234）。
> 借鉴：磁盘调度 SCAN/LOOK 思想。源码于 T30 核账批预入档。

## Problem Statement

寻道类服务排序的病：FIFO 往返空跑（寻道放大）与 SSTF 近者
插队饿死远端——**定向扫掠折返面**缺失。

## Solution

`ElevatorScan`（core/policy，源码已预载）：

- 显式磁头位+方向：沿当前方向服务至该向最远请求（LOOK
  不到物理端）再折返；同向距离升序；
- 服务后磁头停末次轨道、方向翻转（状态可审计）；重复请求
  幂等（TreeSet）；
- fail-fast：磁头/轨道越域、方向 null。

## User Stories

1. 作为 IO 作者，请求按扫掠序批量服务——寻道减半。
2. 作为审计作者，head/direction 显形——调度状态可查。

## Testing Decisions

- 经典教科书请求集上行序（82,140,170,190,43,24,16）钉住；
  下行对称；磁头自身请求最先；重复幂等；越域 fail-fast。

## Out of Scope

- 不做新请求动态插入运行中（快照面）；不做 C-SCAN 环回。

## Further Notes

- 与 DeficitRoundRobin（5027）同族不同面：寻道路径扫掠 vs
  公平份额记账。
- 里程碑：T34/50（68%）。
