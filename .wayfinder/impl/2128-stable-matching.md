# impl 2128 — R 会话 R28 稳定匹配（spec 4027 / T6055–T6056 / R28）

纵切片：StableMatching（core/policy，纯静态）——延迟接受 + 换优
踢旧 + 稳定性可机检。

- 验证：`mvn -pl buzhou-core test -Dtest='StableMatchingTest'` 全绿。
