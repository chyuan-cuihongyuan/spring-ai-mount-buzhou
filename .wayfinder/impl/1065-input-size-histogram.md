# 1065 — 工具入参字节直方分桶读面

**What to build:** ToolInputSizeHistogram implements BuzhouHook（beforeTool 单点：arguments Jackson 序列化 UTF-8 字节落同款五幂次边界桶+溢出+executed/totalBytes）+ E2E 三测。

**Blocked by:** 全仓 verify 运行——core 不在续验范围（-rf :memory），安全并行编码。

**Status:** done

- [x] ToolInputSizeHistogram（core/hook，1401 对称镜像，序列化失败 0 字节兜底）
- [x] ToolInputSizeHistogramTest（三尺寸 E2E/空参 2 字节/reset）
- [x] spec 1412 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolInputSizeHistogramTest'` 3/3 绿。
