# impl 557 — NormalizingEmbeddingModel（effort #804）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/cache/NormalizingEmbeddingModel.java` — 仅覆写 call+embed(Document)（721 同模式）；normalizeOne 复用逻辑；AtomicLong 三计数；EPSILON=1e-4；Embedding(vector 副本, index) 重构。
- `buzhou-resilience/src/test/java/.../resilience/cache/NormalizingEmbeddingModelTest.java` — stub 需实现抽象 embed(Document)（Spring AI 契约）；7 例。

## 口径

- 已归一分支仍 `vector.clone()`——防御 delegate 返回内部数组被下游突变（审计计数≠快路径承诺）。
- 零/近零向量原样返回（新数组包装）——不抛错不置 NaN。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='NormalizingEmbeddingModelTest' → 7/7 绿；快照再生 1 新公共类型。
