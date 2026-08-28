# Wayfinder Map — Buzhou 成对对比评估（effort #23）

> effort #23（已闭合 2026-08-29），延续 #5–#22；收口后累计 170 轮 / impl 1–209。
> 主线：**成对对比（Pairwise A/B judge）**——单输出判定（LlmJudgeEvaluator）无法回答
> 「两个版本/两个模型谁更好」。借鉴 Ragas pairwise / Chatbot Arena：A/B/TIE 判定 +
> **位置偏差消解**（双向评判——正反两个方向判定一致才裁赢家，不一致判 TIE：LLM judge
> 系统性偏好首位展示，Arena 论文实证）。

## Destination

core.eval 新增 `PairwiseJudge`：`(input, outputA, outputB, rubric?)` → `PairwiseVerdict
(WINNER_A|WINNER_B|TIE, reason)`；内部双向评判（A,B 与 B,A 各一次）——两方向同赢家才
裁赢家，否则 TIE（偏差显性化）；协议解析失败/异常按 TIE + reason 标注；零新键。

## Notes

- 外部事实源：Ragas pairwise comparison（LLM 成对 + 位置偏差问题）；Chatbot Arena
  （双向/随机化消位置偏差的实证）。本地裁定：双向而非多次随机（成本 2 次调用，
  确定性可测）。
- 复用 #21 的协议形态（首词解析），但词汇表为 A/B/TIE。

## Decisions so far

- 双向一致才裁赢家；偏差（两方向不同赢家）与协议失败统一 TIE + reason 区分标注。
- 纯新增只读面（宿主自行驱动两次运行后调用比较）——不做 A/B 运行编排（runtime 已有）。

## Not yet specified

- 数值分加权成对；judge 模型 ensemble；跨数据集聚合统计（需求证据后议）。

## Out of scope

- 沿用 #7–#22；A/B 运行编排；新配置键。

## Tickets

- [x] [T277 PairwiseJudge（双向评判 + 一致裁决 + TIE 语义）](tickets/T277-pairwise.md)（impl-209）
- [x] [T278 红队（一致赢家/偏差 TIE/协议失败/端到端）+ 文档 + verify + 收口](tickets/T278-pairwise-close.md)（impl-209；6 例；累计 170 轮）
