# effort #822 — MCP 能力协商快照

- 会话：H 会话 800 系第 23 轮 ｜ spec [822](../../../docs/spec/822-mcp-capability-snapshot.md) ｜ 票 [T1145](../tickets/T1145-mcp-capability-snapshot.md)/[T1146](../tickets/T1146-mcp-capability-snapshot-verify.md) ｜ impl575
- 借鉴：LSP initialize capabilities（microsoft/language-server-protocol ≈11K star）——建连时记录 server 声明了什么

## 勘察（排重）

- McpConnection seam：toolCallbacks/listToolNames/toolHints 三观察点——无单点快照形状。
- McpDirectoryDiff（706）：双快照 diff——缺「一份可存的快照+指纹」基线形状（本类即其输入）。
- grep -i `capability.*snapshot`：ModelCapabilities 是模型域。

## 决定

`McpCapabilitySnapshot`（mcp，纯函数）：of(server, connection, atMs)——名册排序（空则降级 callbacks 定义名提取）+hintCount/readOnlyCount/destructiveCount 计数+fingerprint（排序名册 join，确定性无加密语义）；seam 异常逐路降级（names 炸→callbacks、双炸→空真）；null server 归一空串、null connection fail-fast。与 706 正交：单快照 vs 双快照 diff。

## 测试

排序名册+hint 双计数+指纹值/指纹确定性三例（乱序归一/空真/异名册异值）/names 空降级 callbacks 提取（乱序入排序出）/seam 全炸空真降级/null 归一+null fail-fast——5 例绿（首跑抓获静态 fingerprint 未排序违反契约——实现修正）。

## 诚实边界

只读三观察点（不做 initialize 握手记录——SDK 面不暴露完整 capabilities，这是 seam 边界的诚实口径）；指纹非加密哈希（排序 join——可读性与确定性优先）；hint 计数不裁决危险性（600 既有决策延续）。
