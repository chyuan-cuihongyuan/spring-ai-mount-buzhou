# 458 — 技能混合排序（BM25 + RRF）

**What to build:** `LexicalSkillRanker`（BM25 + CJK bigram 分词）+ `HybridSkillRanker`（语义/词法 RRF 融合、加权可配、语义失败降级纯词法 + 观测计数）；opt-in 不接装配。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] LexicalSkillRanker（BM25 候选集级 IDF + tf 饱和 + 长度归一）
- [x] HybridSkillRanker（RRF k=60、并列保原序、bypassCount 差检测降级）
- [x] HybridSkillRankerTest 6/6 绿（buzhou-skills 96/96 零回归）
- [x] spec 605 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test` 绿。commit 见本轮 `feat(skills)` 提交。
