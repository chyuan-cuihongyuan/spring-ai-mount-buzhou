# Spec 1704 — 评测集内容指纹（effort #1704，R5）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2609–T2610，impl 1304）。借鉴：
> DVC / HuggingFace Datasets 的数据集内容指纹——「跨 run 对比分数」的前提是
> 对比同一份数据；改名不改内容/悄悄改题都要被显形。

## Problem Statement

评测集（EvalDatasetStore/CSV）演化无账：文件名不变内容悄悄改、或内容不变
改名——跨 run 分数对比的隐含前提（同数据）无声破坏，趋势（spec 1444）与
离散（spec 1700）全部失真。

## Solution

`EvalSetFingerprint`（core/eval，静态纯函数）：

- `of(items[, sensitivity])` → `sha256-<64hex>`：规范形 = 逐项 \n 连接
  （UTF-8）→ SHA-256；
- `OrderSensitivity` 闭集：ORDERED（序敏感——项顺序也是内容）/ UNORDERED
  （字典序排序后摘要——项集合相同即同指纹）；
- null/空表稳定常量指纹（非异常）。

## User Stories

1. 作为评测维护者，我把每轮 run 的指纹记入台账——指纹变了，分数跳变先查数据再查模型。
2. 作为审计者，UNORDERED 口径告诉我只是重排了题序，内容没变。
3. 作为框架宿主，任何字符串项列表（题目/标签/金答案）零适配入口。

## Implementation Decisions

- SHA-256 为 JVM 规范必备算法（NoSuchAlgorithmException 转 IllegalStateException）。
- 序敏感为默认口径（评测顺序本身影响结果——spec 1701 的镜像立场）。
- 不引入任何摘要库；hex 用 Character.forDigit 手拼（零依赖）。

## Testing Decisions

- 格式前缀+长度；同输入恒同指纹；ORDERED 对换序敏感/UNORDERED 不敏感；
- 内容单字变化/重复项变化即变；null 与空表同指纹。

## Out of Scope

- 不做增量/分块指纹；不接 EvalDatasetStore 持久化（读面纪律——宿主落账）。

## Further Notes

- 数据卫生三件套：近重复（DatasetNearDuplicateStats）→ 质量（DatasetQualityAudit）
  → 内容指纹（本轮）。
