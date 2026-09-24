# impl 2182 — S 会话 S32 Leveled Compaction 分层压实挑选（spec 5031 / T6163–T6164 / S32）

纵切片：LeveledCompaction（core/recovery）——层容量阶梯 +
评分挑层（并列浅层优先）+ 区间重叠目标收集 + 完成出账。

- 验证：`mvn -pl buzhou-core test -Dtest='LeveledCompactionTest'` 全绿。
