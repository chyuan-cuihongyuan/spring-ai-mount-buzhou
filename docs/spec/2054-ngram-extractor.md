# Spec 2054 — n-gram 特征提取（effort #2054，R55）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3209–T3210，impl 1605）。
> 借鉴：信息检索 n-gram 索引/指纹——定长滑窗特征集。

## Problem Statement

n-gram 特征散落内联（CanaryGuardHook 私有 ngrams 用于注入检测等）
——滑窗口径、去重序、短文本行为各自实现必漂移；SimHash/近重复检测
需要统一特征供给件。

## Solution

`NgramExtractor`（core/metrics，纯函数零状态）：

- `charNgrams(text, n, distinct)`：字符定长滑窗（保持出现序；distinct
  去重保首现序——指纹口径 / false 保留重复——频次口径）；文本短于
  n 返回原文本单元素（不足窗即整段——诚实边界）；
- `wordNgrams(text, n, distinct)`：空白分词的词窗短语；词数不足 n 返
  回原词列表；空白文本空；
- 契约：text 非 null、n ≥ 1 fail-fast。

## User Stories

1. 作为指纹作者，charNgrams 供 SimHash 特征——统一口径不再各造轮。
2. 作为相似检测作者，wordNgrams 短语级相似——比词袋保语序信息。

## Testing Decisions

- abcd→ab/bc/cd 滑窗序；abab 去重保首现（ab/ba）与频次口径（3 项）；
  短文本整段；空白空；词窗短语（2-gram 3 项、3-gram 恰 1 项）；词数
  不足返原词；单字符 gram；畸形四型 fail-fast。

## Out of Scope

- 不做 padding 模式（空格补齐变体留白）；不分词器注入（空白口径）；
  内联实现迁移归后续轮。

## Further Notes

- 与 SimHash（2038）/TextDistance（2052）成三件套：特征供给 + 近似
  指纹 + 精确距离。
