# 1187 — dashboard 响应 gzip

**What to build:** writeJson 协商压缩分支 + 端到端测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] writeJson：Accept-Encoding 协商 + 512B 阈值 + Content-Encoding 头
- [x] DashboardGzipTest 两断言 + dashboard 34 用例零回归

## Done

验证：`mvn -pl buzhou-observe-dashboard test` 全绿。
