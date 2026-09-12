# Wayfinder Map — Buzhou 提示词注册表快照导出/导入（effort #545，E 会话第 45 轮）

> E 会话第 45 轮（401 注册表扩散轮；Langfuse export/import 思想）。
> 勘察：注册表版本/标签只在内存/DB——跨环境搬运（备份/还原）无序列化面。

## Destination

`core.prompt.PromptRegistrySnapshot`：export(registry) → 可移植 JSON
（format 标记+names 字典序+版本升序+labels 全量）；importInto(registry,
json) → 导入到**空注册表**按旧版本序重放 publish（新版本号与旧一致）+
标签重指（latest 自动跳过）；非空目标/格式不符 fail-fast。

## Notes

- 号段：spec 545 / T843-844 → 实际 T847-848 / impl-447。
- 借鉴源：Langfuse prompt export/import。

## Out of scope

- 非空合并导入；publishedAt 保真。

## Tickets

- [x] [T847 导出/导入往返](../tickets/T847-registry-snapshot.md)
- [x] [T848 fail-fast 语义](../tickets/T848-snapshot-failfast.md)
