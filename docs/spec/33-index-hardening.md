# Spec 33 — 会话索引工程闭合（契约矩阵 / 删除联动 / fsck 索引源）

> effort #7（T112–T113 / impl-87–88）。T109 索引能力的防线补强。

## §A 契约测试矩阵（T112 / impl-87）

- `AbstractSessionIndexContractTest`（core contract 包，随 test-jar 发布）五契约：
  upsert 幂等收敛 / get、过滤组合（appId×agentName×status）+ lastActive 倒序 + 分页 /
  tag 精确匹配（前缀邻键不误报）/ delete 幂等 / 空索引零行。
- 三实现接入：内存（core）/ JDBC（H2，附持久用例：新 store 实例同数据源行仍在）/
  Redis（Testcontainers，同口径持久用例；无 Docker 跳过）。
- 实现私有细节（JDBC 的 JSON LIKE 预筛 + 内存复核、Redis 翻页）留在各模块既有测试。

## §B 删除联动与 fsck 索引源（T113 / impl-88）

- **DELETED 联动**：core auto-config 在 SessionIndexStore bean 存在时，同步挂接
  `session-index` 清理贡献者——会话 `delete()` 级联把索引行置 `DELETED`（保留元数据供
  审计；物理删由运维按保留策略）。**默认列表排除 DELETED**（status=null = 非 DELETED，
  三实现与契约统一）；显式 `status=DELETED` 过滤可查审计行。
- **fsck 索引源**：`StoreFsck.run(stores, index, extras)` 重载——索引存在且行数 ≥ 观测
  留痕时全集走索引（完整性更高）；索引滞后/未装配回退观测源（诚实降级）。
