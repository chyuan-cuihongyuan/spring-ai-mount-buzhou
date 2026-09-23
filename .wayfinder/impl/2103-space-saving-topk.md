# impl 2103 — R 会话 R3 Space-Saving 频繁项 top-k（spec 4002 / T6005–T6006 / R3）

纵切片：SpaceSavingTopK（core/metrics）——k 槽精确计数 + 淘汰继承
+ 单侧高估 + 榜单/误差界/守恒三读数。

- 验证：`mvn -pl buzhou-core test -Dtest='SpaceSavingTopKTest'` 全绿。
