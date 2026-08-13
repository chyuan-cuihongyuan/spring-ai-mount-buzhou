# 01 — CI 在干净 GitHub runner 上稳定转绿

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T1](../tickets/T1-ci-red-remotely-green-locally.md)

**What to build:** 从「GitHub badge=failing、本地 `mvn clean verify` 全绿」到「干净 runner 上 `mvn verify` 稳定绿」的端到端可信度修复。先取真实 CI 失败日志确认根因（[T2](../tickets/T2-spring-ai-native-vs-buzhou.md) 已锁定为**环境性 maven 缓存**：`setup-java` 的 `cache: maven` 残留 GA 前的否定解析标记 `.lastUpdated`），再修复（清缓存 / `-U` 强制更新 / 临时移除 `cache: maven` 之一或组合），push 验证 badge 转绿。**绝不加 milestone / snapshot 仓**——`spring-ai 2.0.0` / `spring-boot 4.1.0` 均已 GA、在 Maven Central，pom 无 `<repositories>` 是对的。修复后「core 鲁棒」的结论才建立在可复现证据之上。

**Blocked by:** 无 — 可立即开始（本环境无 `gh` / 未鉴权，取真实 CI 日志须请用户代取）。

**Status:** ready-for-agent

- [ ] 取到真实 GitHub Actions 失败日志，确认失败步骤为依赖解析类（非编译 / 测试缺陷）
- [ ] 根因与 T2 结论（环境性缓存）一致；若不一致则回写纠正 T1 / T2
- [ ] 实施修复（清缓存 / `-U` / 去 `cache: maven`），pom **不新增**任何 `<repositories>`
- [ ] push 后 CI badge 转绿，且连续一次以上稳定
- [ ] 本地 `mvn -B -ntp clean verify` 仍十五模块全绿
