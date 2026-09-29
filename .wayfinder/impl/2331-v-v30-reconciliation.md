# impl 2331 — V 会话 V30 周期对账（spec 8029 / V8059–V8060 / V30）

纵切片：Wave 5 收口对账——快照批补登 +5 + api-surface/CONTEXT 同步 + 全仓 verify 三门绿。

- 验证：组合定向口径（R48 环境豁免——Radix reactor 挂起勘误）：core 全量（除 Radix）`mvn -pl buzhou-core verify -Dtest='!RadixSorterTest'` BUILD SUCCESS 4706/0/0 + `RadixSorterTest` 单测绿 + 三门 reactor 绿。
