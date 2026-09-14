# 1420 — 评估运行年龄台账

> 来源：L 会话第 21 轮 = effort #1420（票 T2141 / T2142 / impl 1073）。**换题记录**：原题 R46 健康探针时延面在 core 无 probe seam（健康检查已由各 Health 类自计时）——换入 S9 题的注册表年龄化（tqdm 进度条思想 + k8s「Pod 运行 3 天」运行时长异味）。借鉴：tqdm（done/total/eta 的进度可见性——卡死的运行从「感觉慢」变年龄显形）。

## Problem Statement

`EvalRunRegistry` 只有 active(kind) 计数：**在途运行的年龄**（最老活跃运行开了多久）无读面——挂死的评估运行（子进程卡死/数据集锁等待）混在正常长跑里，「有一个 eval run 开了两小时」这种异味不可见。完成运行的时长分布有 EvalRunDurationStats（item 级），运行级跨 run 台账缺位。

## 目标

- `EvalRunAgeLedger`（core/eval，公共静态面 + Registry 埋点，ToolArgsValidator 先例）：
  - `recordOpened(kind)` / `recordClosed(kind, durationMillis)`：Registry.Registration begin/close 双点埋点（只增记账）；
  - 快照：`record Snapshot(int active, long oldestActiveAgeMillis, long maxCompletedDurationMillis, long closed)`——最老活跃运行年龄（卡死异味哨兵；无活跃 = -1）+ 历史最长完成时长 + 完成累计；
  - `stats()` + `resetForTest()` 归零口。
- Registry 埋点为手术式增量（Registration close 语义逐位不变）。

## 兼容性

纯增量读面：Registry 的注册/注销/active 计数语义逐位不变。

## Out of Scope

- item 级进度条（EvalRunner 无进度缝——诚实入档，接线归装配轮）。
- per-kind 分位（kind 只有 eval/ab 两值，量小直接总量+最老）。
- 卡死运行的强制取消（读面不裁决）。
