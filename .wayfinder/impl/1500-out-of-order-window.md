# impl 1500 — OutOfOrderWindow 乱序接纳窗（R100 = effort #1899 / spec 1899 / T2999-T3000）

**What**：`OutOfOrderWindow`（core/metrics 静态纯函数 + Accept 枚举）
——classify 三态（FRESH/LATE_ACCEPTED 窗沿含下/TOO_OLD）+ advance
水位取大单调；负窗/负时点 fail-fast。

**Why**：QuestDB out-of-order 语义——严格按序需全局重排、放任乱序
让历史定稿被改写；「水位 − 窗宽」接纳窗让迟到容忍与定稿速度可换
挡。与 SequenceOrder 互补（顺序对错 vs 迟到策略）。

**Verify**：`OutOfOrderWindowTest` 4 用例全绿（三态/窗沿含下/推进
单调/畸形三例 fail-fast）。

**Status**：done（2026-09-23）
