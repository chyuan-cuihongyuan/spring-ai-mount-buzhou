# 1421 — 实验分桶均衡审计

> 来源：L 会话第 22 轮 = effort #1421（票 T2143 / T2144 / impl 1074）。借鉴：A/A test（无处理对照实验——哈希分桶分配正确性最好的自证是「无差别 key 流入后各桶份额是否均匀」，份额失衡 = 哈希/权重缺陷的先行信号）。

## Problem Statement

`ExperimentBucketer`（core/experiment，哈希分桶 + holdout + 过期实验）做分配，但**分配均衡性**无审计面：权重配错、哈希倾斜、空桶（权重键名笔误）都要等下游指标显著性异动才被倒查——A/A 检查本可在实验上线前 5 秒显形。

## 目标

- `ExperimentBalanceAudit`（core/experiment，纯函数静态面，private 构造）：
  - 两段式入口 `forBuckets(List<String>)` → `withAssignments(Map<String,Long>)`：**先声明桶集合**——零分配桶计入（缺桶 = 最典型的权重配置错误，不能因 Map 里没有就假装桶不存在）；
  - `BalanceReport(buckets, totalAssignments, maxDeviation, balanced)`；`BucketShare(bucket, assignments, share, deviation)`；
  - 均衡 = 每桶份额与均匀份额（1/n）的最大绝对偏移 ≤ `BALANCE_TOLERANCE`（5pp A/A 惯例；含端点——双比较 1e-9 卫生余量）；
  - 无样本哨兵：total=0 时 maxDeviation=-1、balanced=false（不冒充均衡）；buckets assignments 降序平名典序。
- 纯函数零状态：不触 bucketer/曝光记录。

## 兼容性

纯函数零 IO；不参与分配（只读审计）。

## Out of Scope

- 卡方/显著性检验（p 值面另轮——先给工程容差判据）。
- holdout 百分比的审计（bucketer 内部语义）。
- 跨实验多维交叉均衡（单实验口径）。
