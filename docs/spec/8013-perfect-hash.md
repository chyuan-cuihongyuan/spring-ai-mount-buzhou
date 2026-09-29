# Spec 8013 — PerfectHash 完美哈希（effort #8013，V14）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8027–V8028，impl 2315）。
> 借鉴：Czech, Havas & Majewski CHM 两级完美哈希（gperf 思想）。

## Problem Statement

静态键集查询的病：通用哈希必留碰撞概率——**静态键集
可以构造无碰撞完美哈希**，比较次数降为零（哈希即唯一
定位），两级结构把空间压到 O(n)。

## Solution

`PerfectHash`（core/metrics）：静态键集构建——种子化扫描
找一级参数 (a,b) 使一级桶无键冲突（键数 ≤桶 2 倍），二级
每桶再扫参数直到桶内无碰撞；`lookup(key)` 返回 [0,n) 槽位
（缺席 −1 诚实缺省——完美哈希不认识集合外的键）；构建
尝试上限后 fail-fast（参数空间耗尽——诚实拒绝而非吐错表）；
null 键 fail-fast；种子注入（同种子同表——确定性可回放）。

## Testing Decisions

- 手锚（小键集 lookup 全集双射无碰撞+缺席 −1）；双射
  性质：n 键 n 槽一一对应（300 随机键集）；同种子同表/
  异种子（大概率）异表；构建失败路径（上限构造触发）；
  fail-fast。

## Out of Scope

- 不做动态插删（静态键集语义明示）；不做最小完美哈希
  （MPH 空间极限另件）。

## Further Notes

- 与 DeterministicHash（2057）同族不同面：通用确定性
  散列 vs 静态集无碰撞定位。
- 里程碑：V14/50（28%）。
