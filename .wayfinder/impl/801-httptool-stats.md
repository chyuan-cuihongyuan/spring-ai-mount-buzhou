# 801 — http_request 请求量水位与结果分布读面

**What to build:** HttpRequestTool 静态八计数（attempts/successes + method/url/ssrf/timeoutParam/oversize/failures 六拒绝桶）+ 嵌套 HttpToolStats + stats()/resetForTest() + 本地回环成功/五类拒绝/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 八计数落点（入口/送达/六拒绝桶；urlRejects 合并非法 URL 与非法 scheme）
- [x] HttpToolStats 嵌套 record + totalRejects 派生 + stats() + resetForTest()
- [x] HttpToolStatsTest（本地 HttpServer 回环 + 放行清单 127.0.0.1，零外网依赖：成功/坏 method/坏 URL/s 内网拒/timeout 越界/响应超限/守恒/reset 八测）
- [x] spec 1049 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='HttpToolStatsTest'` 全绿 + 既有 HttpRequestToolTest/SsrfGuardStatsTest 回归绿。commit 见本轮 `feat(tools)` 提交。
