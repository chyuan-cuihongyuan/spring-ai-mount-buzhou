# 515 — JSONL 轮转旧档 gzip 压缩

**What to build:** RollingJsonlWriter compressFromGeneration opt-in——代际 ≥ N 存 .gz（delaycompress：file.1 恒明文），shift 转码/纯 rename/双形态清理，实例与静态路径同口径，gzip 失败入 rotationFailures。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] compressFromGeneration 参数 + shiftGenerations 压缩语义
- [x] 实例 rotate / 静态 rotateIfNeeded 同口径（旧签名零变化）
- [x] 转码/解压回读/file.1 明文/双形态清理/默认零回归用例
- [x] spec 712 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
