# Spec 4032 — 祖先费率打包（effort #4032，R33）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6065–T6066，impl 2133）。
> 借鉴：Bitcoin CPFP（Child Pays For Parent）祖先包费率。

## Problem Statement

依赖图任务打包的病：贪婪按单笔费率（价值）排序会让低费
父任务永远滞留——高费子任务的依赖未满足，谁也进不了
批次——**祖先包聚合价值面**缺失。

## Solution

`AncestorFeerate`（core/policy）：

- `Tx(id, fee, size, parents)` 依赖图注册（未知父/重复
  id/非正 size fail-fast）；
- `ancestorFeerateOf`：祖先闭包（含自身）∑fee/∑size——子的
  高费拉高父的排名（CPFP 语义）；
- 共享祖先去重（∑ 只计一次——多子不重复抬）；
- `packageOf`：祖先闭包拓扑序（父先子后，确定性 DFS 后序）；
- `inclusionOrder`：每轮取全池祖先费率最高者（并列 id 字典序），
  其祖先闭包**整包入场**（父先子后）——子的高费把父拖进批次
 （CPFP 同式）；
- 前置先注册（未知父 fail-fast）——DAG-by-construction 环
  构造不可达；闭包 DFS 环检测留防御。

## User Stories

1. 作为批次打包作者，低费高价值前置任务被子任务抬进批次——
   依赖不被饿死。
2. 作为审计作者，同图同序（确定性可回放）。

## Testing Decisions

- 子 5.5 全池最高拖整包先于 5.0 单干者入场（父被抬进批次；
  父自身包费率诚实 1.0 不含子孙）；共享父去重双子只计一次；
  三链累积（k 包 1400/300）；packageOf 父先子后；未知父/
  重复 id/零 size fail-fast（前置先注册——环构造不可达）；
  费率并列 id 字典序确定性。

## Out of Scope

- 不做替代费率（RBF）替换面；不做区块容量装箱
 （slab 装箱件已覆盖）；不做签名/UTXO 语义。

## Further Notes

- 与 TopologicalSorter 同族不同问：偏序排程 vs 聚合价值
  驱动的打包序。Wave 6 第三件。
- 里程碑：33/50。
