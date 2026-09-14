# 1090 — 评估数据集质量审计

**What to build:** DatasetQualityAudit 纯函数（退化分桶+短输入阈值+长度分位+退化比）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] DatasetQualityAudit（core/eval，private 构造静态面）
- [x] DatasetQualityAuditTest 五测
- [x] spec 1437 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='DatasetQualityAuditTest'` 5/5 绿。
