# impl-88 — 索引 DELETED 联动 + fsck 索引源

**What to build:** 会话删除级联置索引行 DELETED（审计留存）；fsck 全集优先走索引源。

**Blocked by:** None

**Status:** done

- [x] auto-config：session-index 清理贡献者（get→upsert DELETED；未见过的会话无操作）
- [x] 默认列表排除 DELETED（三实现 + 契约新用例统一）
- [x] StoreFsck.run(stores, index, extras) 索引优先 + 观测回退（行数比较）
- [x] 测试：级联/索引源/回退/DELETED 可见性——core 300 / jdbc 74 全绿；spec 33 §B

## Done

commit：见 git log（impl-88）。
