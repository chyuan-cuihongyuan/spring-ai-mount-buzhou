# 804 — 嵌入 L2 归一化装饰器

> 来源：H 会话第 5 轮 = effort #804 / [T1109](../../.wayfinder/tickets/T1109-normalizing-embedding.md) / [T1110](../../.wayfinder/tickets/T1110-normalizing-embedding-verify.md) / impl 557。
> 借鉴：sentence-transformers `normalize_embeddings=True`（≈17K star）。

## Problem

嵌入范数漂移（不同供应商/批次/长度文本）让余弦相似度的分母不稳定：语义缓存与向量桶的相似度阈值在原始向量上可比性弱；点积检索（归一后等价余弦）是向量库标准优化——归一层缺位。

## Solution

`NormalizingEmbeddingModel`（resilience.cache，Chunking 721 同款装饰器模式）：

- **逐条 L2 归一**：call() 输出向量归到单位范数（维度与 index 保持、输入副本不突变）。
- **三计数面**：normalized（重归一）/alreadyUnit（ε=1e-4 内跳算，仍克隆）/zeroNorm（零向量透传——归一无定义如实计数）。
- **双路径**：call() 与 embed(Document) 均覆盖（与 721 直通委托先例一致）。

## 兼容性

纯装饰器（应用显式包装 delegate）；无配置键；可与 Chunking 叠加（顺序：归一在内层或外层均可——归一逐条、分批仅切批）。

## 诚实边界

ε 判定是工程容差；归一改变相似度算法语义（装配一次到位不混用）；alreadyUnit 非快路径承诺（克隆防御下游突变）。
