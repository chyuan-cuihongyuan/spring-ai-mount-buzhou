# impl 1301 — EvalOrderRotator 评测项轮换消序（R2 = effort #1701 / spec 1701 / T2603-T2604）

**What**：`EvalOrderRotator`（core/eval 静态纯函数）——`permutation(n, runIndex)`
runIndex 派生种子 Fisher–Yates（`java.util.Random` LCG 规范固定跨 JVM 重现）；
`shuffled` 重排新列表多重集守恒；`OrderPlan` 审计复现。

**Why**：跨 run 换序摊平顺序效应——与 spec 1700 MAD 配套：换序后 MAD 仍大 =
能力真波动，收窄 = 原方差多为顺序效应（OpenAI Evals/HELM 思想）。

**Verify**：`EvalOrderRotatorTest` 5 断言（确定性/守恒/0..7 两两互异/内容守恒
不改入参/退化尺寸安全）。

**Status**：done（2026-09-15）
