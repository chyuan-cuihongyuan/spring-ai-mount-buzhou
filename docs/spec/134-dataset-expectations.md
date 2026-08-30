# Spec 134 — 数据集期望套件（effort #112）

> wayfinder map：`.wayfinder112/MAP.md`（T459–T460）。借鉴：Great Expectations
> 数据合约。

## Problem Statement

评估数据集回流/手编混入脏行（空输入、期望缺失、重复行、规模异常）时没有
run 前门禁——脏数据直接进评估器，以畸形分数的形式在 run 结束后才暴露，
排障要回溯整条管线。

## Solution

`eval/DatasetExpectations`：声明式期望套件。四内置期望（非空输入 / 期望值
在场 / 输入唯一 / 规模窗口 [min,max]）+ `named()` 自定义行级期望（名单非空白
fail-fast）。`validate(items)` 只读校验：行级发现带行号与 60 字符预览、每期望
样本封顶 10 条 + 「共 N 处」诚实计数（报告面不被脏数据集打爆）；数据集级发现
带 itemCount。`Result.summary()` 单行——CI 门禁可读；空套件恒过（零期望 =
零意见，诚实）。

## User Stories

1. 作为评估管线负责人，我在 run 前挂期望套件，所以脏行 fail-fast 带行号，
   不再以畸形分数晚暴露。
2. 作为 CI 维护者，失败时一行 summary 列出被违反的期望清单，所以门禁日志
   可扫读。

## Testing Decisions

- 红队：干净过 + 单行 summary；脏行带行号/期望名；重复输入与规模窗（空/巨）
  数据集级违反；25 脏行封顶 10 样本 + 共 25 处；空套件恒过 + named 自定义；
  名单/规模窗 fail-fast。

## Out of Scope

- EvalRunner 自动接线；期望持久化随数据集；统计分布期望；自动修数。

## Further Notes

- 与 spec 82（数据集指纹）互补：指纹管「变没变」，期望管「变了之后合不合格」。
