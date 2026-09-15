# 933 — ResilienceAdvisor 影子镜像旁路 e2e 补测（R31）

**What to build:** ShadowMirrorEndToEndTest（4 用例：镜像发生/镜像失败全吞/候选空守卫/零采样守卫）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ShadowMirrorEndToEndTest（4 用例）
- [x] spec 1230 + README 行
- [x] 验证：定向 4 用例绿 + resilience 全量绿

## Done

验证：定向 4 用例全绿；resilience 全量绿。commit 见本轮 `test(resilience)` 提交。
