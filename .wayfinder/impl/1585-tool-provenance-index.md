# impl 1585 — 工具溯源索引（spec 2034 / T3169–T3170 / R35）

纵切片：`ToolProvenanceIndex`（buzhou-mcp provenance 新子包主）+
`ToolProvenanceIndexTest`（七用例）。双向账、独供孤儿、覆盖重注册、
冲突面。

- 测试：`mvn -pl buzhou-mcp test -Dtest=ToolProvenanceIndexTest` 7/7 绿。
