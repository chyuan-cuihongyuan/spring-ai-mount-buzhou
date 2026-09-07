# Spec 190 — 模型成本健康面（effort #139）

> wayfinder map：`.wayfinder/maps/effort-139.md`（T551–T552）。

## Problem Statement

成本台账有了账有了导出，但运维在健康端点看不到「现在哪个模型最烧钱」——
要 SSH 查表或等窗口导出。

## Solution

`health/ModelCostHealth`：恒 UP（观测面——成本高是账单议题不是健康议题，
裁决在预算闸）；details = distinctModels + totalMicroUsd/totalUsd 双口径 +
topCosts 前 8 行（model/microUsd/usd，健康详情有界纪律）。台账是全局旋钮 +
预算钩子自动入账（spec 176），恒有数据（含全零——诚实空态）。

## User Stories

1. 作为运维，健康端点一屏看到烧钱榜与总额，所以成本异常（突涨模型）巡检
   即可见。

## Testing Decisions

- 红队：top-8 有界（12 全量 distinct 只显 8）+ 双口径一致 + 降序；空台账
  零行诚实。

## Out of Scope

- autoconfig bean 登记（后续接线轮）；DOWN；价目。

## Further Notes

- 成本观测三面：台账（进程内）/ JSONL（窗口）/ 健康段（实时巡检）。
