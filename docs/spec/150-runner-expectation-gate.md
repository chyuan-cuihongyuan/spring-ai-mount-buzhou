# Spec 150 — 期望门禁接线（effort #120）

> wayfinder map：`.wayfinder/maps/effort-120.md`（T503–T504）。spec 134（数据集期望
> 套件）的闸位接线——账本变刹车。

## Problem Statement

spec 134 落了期望套件但没有闸位：没人调用 validate，脏数据照常进评估器、
照常烧 token——套件是合约没人执行。

## Solution

`EvalRunner.setExpectations(DatasetExpectations)`（可选装载，null = 既有行为
零变化）：`run()` 在数据集项加载后、任何模型调用前校验；未过 fail-fast 挂
`EVAL_OPERATION_INVALID`——message 携带单行 summary + 前三条发现（期望名/
行号/有界明细）。脏数据零 token 成本出局。

## User Stories

1. 作为评估管线负责人，我给 runner 挂上期望门禁，所以脏数据集在烧钱前出局，
   失败信息直接指到行。
2. 作为既有用户，不设门禁零变化——升级无感。

## Testing Decisions

- 红队：脏数据集 fail-fast（模型零调用计数断言 + message 锚点）；干净过闸
  照常跑（pass 链路）；未装载门禁脏行照跑（legacy 零变化）。EvalRunner/
  DatasetExpectations 既有测试回归。

## Out of Scope

- 宽严两档；门禁结果入档；CI 输出格式；套件持久化。

## Further Notes

- 与 spec 82 指纹互补链完整：指纹说「变了」、期望说「不合格」、门禁让
  「不合格即出局」。
