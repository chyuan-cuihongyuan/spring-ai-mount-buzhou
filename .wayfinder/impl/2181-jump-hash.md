# impl 2181 — S 会话 S31 Jump Hash 跳跃一致哈希（spec 5030 / T6161–T6162 / S31）

纵切片：JumpHash（core/policy）——线性同余跳跃定桶 + 单调稳定
迁移 + FNV-1a 64 稳定指纹入口。

- 验证：`mvn -pl buzhou-core test -Dtest='JumpHashTest'` 全绿。
