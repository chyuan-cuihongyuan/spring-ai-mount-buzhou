---
Type: task
Status: closed
---
## Question

回归与收口：resize 五用例（扩容放行/缩容拒新放到在飞释放/热加/热移除
回 NOOP/计数保留）；装配三用例（监听 bean 随舱/事件+PropertySource 改
生效/舱未开不装配）；README 纵深 IV 加行（spec 320）；PROGRESS 台账。

## Resolution

done（2026-09-02）：impl-343；装配三用例绿（addFirst PropertySource +
publishEvent 外部行为路径）；buzhou-core 全模块 1204 绿（MVN_EXIT=0）。
