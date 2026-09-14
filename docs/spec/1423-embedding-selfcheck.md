# 1423 — Embedding 提供者质量自查探针

> 来源：L 会话第 24 轮 = effort #1423（票 T2147 / T2148 / impl 1076）。借鉴：OpenAI embeddings cookbook / sentence-transformers 语义自检（相似对余弦序成立 = 检索可用的最低自证）。

## Problem Statement

部署侧换 embedding 模型/供应商后，检索质量劣化只能从业务侧「召不回了」倒推：已知相似对的余弦序是否仍成立无一行自查。模型互换/供应商切换/版本升级的回归哨兵缺位。

## 目标

- `EmbeddingSelfCheck`（core/spi，纯函数静态面，private 构造）：
  - 内建确定性合成句对：相似对×2（语义近邻——共享语义关键词）+ 无关对照对×2（一一对照）；
  - `probe(EmbeddingProvider)` → `record ProbeReport(similarPairsPassed, similarPairsTotal, minMargin, dimension)`；
  - **序判定口径**：cos(similar) > cos(dissimilar 对照) 逐对成立 + `minMargin > 0`——余弦绝对值随模型分布漂移，但相似对相对序在任何可用模型上都应成立；
  - 派生 `orderHolds()`（回归哨兵：false 即模型互换回归）；`dimension` 显形（换模型维度漂移的伴生信号——与 FAISS 维度校验同源诉求）。
- 纯函数零状态：不落台账；确定性合成样本（非真实语料）。

## 兼容性

纯函数零 IO；对 provider 只读（embed 调用无副作用假设同 SPI 契约）。

## Out of Scope

- 阈值可配置化（序判定免阈值——绝对余弦阈值随模型不可移植）。
- 探针结果台账化（R8 PiiProbeSelfCheck 同款另轮）。
- 检索端到端回归（Recall@k 归评估域）。
