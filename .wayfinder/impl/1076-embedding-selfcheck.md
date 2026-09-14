# 1076 — Embedding 提供者质量自查探针

**What to build:** EmbeddingSelfCheck 纯函数（合成句对穿测+序判定+维度显形）+ 四测。

**Blocked by:** None.

**Status:** done

- [x] EmbeddingSelfCheck（core/spi，private 构造静态面）
- [x] EmbeddingSelfCheckTest 四测（词包全过/病态显形/负向量对照/维度三态）
- [x] spec 1423 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EmbeddingSelfCheckTest'` 4/4 绿。
