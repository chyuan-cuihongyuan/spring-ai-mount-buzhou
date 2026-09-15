# impl 1304 — EvalSetFingerprint 评测集内容指纹（R5 = effort #1704 / spec 1704 / T2609-T2610）

**What**：`EvalSetFingerprint`（core/eval 静态纯函数）——规范形（\n 连接 UTF-8）
SHA-256 → `sha256-<64hex>`；ORDERED（默认）/UNORDERED（排序后摘要）双口径；
null/空表稳定常量指纹。

**Why**：跨 run 分数对比的前提是同一份数据——改名不改内容/悄悄改题都要显形
（DVC/HuggingFace Datasets 数据集指纹思想）。

**Verify**：`EvalSetFingerprintTest` 4 断言（格式+确定性/序口径契约/内容敏感/
null≡空）。

**Status**：done（2026-09-15）
