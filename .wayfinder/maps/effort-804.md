# effort #804 — 嵌入 L2 归一化装饰器

- 会话：H 会话 800 系第 5 轮 ｜ spec [804](../../../docs/spec/804-normalizing-embedding.md) ｜ 票 [T1109](../tickets/T1109-normalizing-embedding.md)/[T1110](../tickets/T1110-normalizing-embedding-verify.md) ｜ impl557
- 借鉴：sentence-transformers `normalize_embeddings=True`（UKPLab/sentence-transformers ≈17K star）

## 勘察（排重）

- ChunkingEmbeddingModel（721）：EmbeddingModel 装饰器先例（分批）——无归一维度。
- EmbeddingProvider.cosine：core 侧余弦——消费方按需归一无持久保证。
- grep -i `normaliz`：core/guard PiiNormaliz 等无关族；嵌入归一缺位。

## 决定

`NormalizingEmbeddingModel`（resilience.cache，721 同模式仅覆写 call+embed(Document)）：输出向量逐条 L2 归一——cosine 退化为点积（检索加速+跨供应商尺度一致）；已归一（ε=1e-4 内）跳过重算计数、零向量原样透传计数（归一无定义）；维度与 index 保持；输入不突变（副本语义）。三计数面 normalized/alreadyUnit/zeroNorm 只读。

## 测试

单位范数+index 保持/归一后点积=原始余弦（EmbeddingProvider.cosine 对照精确 1e-6）/已归一跳算计数/零向量透传/embed(Document) 路径/输入不突变/fail-fast——7 例全绿。

## 诚实边界

ε=1e-4 判「已归一」是工程容差非数学严格；alreadyUnit 计数是审计口径非快路径承诺（仍克隆副本防下游突变）；归一改变向量语义（余弦→点积）——下游若存原始向量需一致性切换（装配一次到位，运行期不混用）。
