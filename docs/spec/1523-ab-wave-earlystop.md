# 1523 — A/B 并行 SPRT 波间早停（spec 1522 扩散）

> 来源：M 会话第 26 轮 = effort #1523（impl 1126）。

## 背景

PairwiseEvalRunner 并行路径的 earlyStop（SPRT 达界）/hostCancel 检查在 task 首行，但全量 invokeAll 派发后虚拟线程全起——早停近似无效。

## 目标

分波执行（items 按 workers 分块）：波间检查 earlyStop/hostCancel，达界/取消即 break（剩余 skipped）；波内 scored 原子更新语义不变。

## 兼容性

早停/取消更早生效（行为收紧面内）；未触发路径零变化。
