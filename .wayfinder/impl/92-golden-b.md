# impl-92 — 黄金轨迹扩充 B

**What to build:** 导出导入往返续用 / 工具结果限幅（模型侧可见标记）/ 索引全生命周期（含 DELETED）。

**Blocked by:** T113（DELETED 联动）— 已闭合

**Status:** done

- [x] G10：JSON 往返三槽重映射 + 导入 Id 续聊历史注入
- [x] G11：100K 工具结果 → 截断标记 + 上限提示进模型 prompt（JSON 包装注记）
- [x] G12：索引 ACTIVE→turnCount→CLOSED→DELETED + 默认排除/显式可查
- [x] examples 3/3 绿；spec 34 §C

## Done

commit：见 git log（impl-92）。
