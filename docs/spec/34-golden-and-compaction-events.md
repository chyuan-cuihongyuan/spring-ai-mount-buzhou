# Spec 34 — 压缩事件化与黄金轨迹全覆盖

> effort #7（T115–T117 / impl-90–92）。

## §A 压缩事件化（T115 / impl-90）

- `InjectionViewProcessor.setCompactionListener(BiConsumer<String, MicroCompactionResult>)`：
  实际折入（compactedMessageIds 非空）才通知；监听器异常 lenient（不影响视图主链）。
- `MemoryModule` 装配把监听器接 ObservabilityStore 双写 `memory.compacted` 事件
  （payload：compactedCount/reclaimedChars）——视图读路径无会话事件通道，
  RunawayCountorners 同款观测侧写通道。黄金轨迹/运维事件流经 eventsOfSession 可见。

## §B 黄金轨迹扩充 A（T116 / impl-91）

- `GoldenTrajectoryEffort6Test`（examples golden 包）三条确定性轨迹（行为步骤序列断言，
  非 SessionEvent 通道——能力面在 store/观测层）：
  - **G7 evidence 引用生命周期**：fork 登记 → 源删除证据保留且可回读 → fork 关闭
    （最后引用者）→ 延迟物理删 → 悬垂读 EVIDENCE_GONE。
  - **G8 outbox 跨重启**：第一代恒 500（事件滞留）→ close → 第二代（共享 store）补投递
    成功、零死信。
  - **G9 压缩事件**：大历史折叠 → memory.compacted 入观测库（计数/回收为正）。

## §C 黄金轨迹扩充 B（T117 / impl-92）

- `GoldenTrajectoryEffort6BTest` 三条轨迹：
  - **G10 导出/导入**：导出→JSON→导入（新 Id，三槽重映射）→ 以导入 Id spawn 续聊
    （历史注入断言）。
  - **G11 工具结果限幅**：100K 结果工具 → 回喂模型的结果含截断标记与上限提示
    （断言关键标记——FunctionToolCallback 结果经 JSON 字符串包装，尺寸 ±引号）。
  - **G12 索引生命周期**：ACTIVE→turnCount 累计→CLOSED→delete() 级联 DELETED；
    默认列表排除审计行、显式 status=DELETED 可查。
