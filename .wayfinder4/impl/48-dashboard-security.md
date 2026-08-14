# 48 — dashboard 安全模型与健壮性

**What to build:** dashboard 默认只绑 127.0.0.1；绑非 loopback 必须配 token 否则拒绝启动；配 token 后全部端点校验 Bearer（401）；500 不回显内部异常；请求体/分页有上限；XSS 转义补全；context 关闭无泄漏。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] bind-address（默认 127.0.0.1）+ 非 loopback 无 token 启动失败（含 FailureAnalyzer）+ 非 loopback 有 token WARN
- [ ] Bearer token 鉴权（API+静态页；401 JSON）+ auth-token ${ENV:} 占位
- [ ] 500 → internal_error + 服务端 ERROR 日志；body 1MB 413；size clamp [1,200]
- [ ] executor stop() 关闭；esc() &#39;；pathPrefix 校验；启动/路由日志基线
- [ ] 测试修掉 import core.internal；补鉴权/拒启动/413/clamp/转义/401 用例
