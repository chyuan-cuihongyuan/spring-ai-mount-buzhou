# impl 1303 — JudgePositionBias 裁判位置偏差读面（R4 = effort #1703 / spec 1703 / T2607-T2608）

**What**：`JudgePositionBias`（core/eval 静态纯函数）——`PairJudgement(id,
verdictAB, verdictBA)` → `analyze` → `BiasReport(pairs/consistent/firstWinsBoth/
secondWinsBoth/mixedTie/biasRatio 空哨兵 −1)`；双 TIE 一致、单 TIE mixedTie。

**Why**：LLM 裁判偏爱先出现的回答（MT-Bench/FastChat 实证）——位置偏差裁判
在给门喂毒分数，须显形而非默许。

**Verify**：`JudgePositionBiasTest` 4 断言（空哨兵/三类镜像一致/偏差分桶
ratio=0.5/null 口径）。

**Status**：done（2026-09-15）
