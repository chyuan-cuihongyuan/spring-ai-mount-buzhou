# Contributing

感谢关注 Spring AI Mount Buzhou！本文档说明如何搭建环境、提交改动。

## 开发环境

- JDK 21+
- Maven 3.9+
- `buzhou-store-jdbc` / `buzhou-store-redis` 的测试用 [Testcontainers](https://www.testcontainers.org/)，本地跑这两个模块的测试需要 Docker 可用

## 构建与测试

```bash
mvn verify                                  # 全量构建 + 测试（CI 守门命令，PR 必过）
mvn -pl buzhou-core -am test                # 单模块测试（-am 连带构建依赖模块）
mvn -pl buzhou-core test -Dtest=HookChainTest              # 单个测试类
mvn -pl buzhou-core test -Dtest=HookChainTest#methodName   # 单个测试方法
```

### 本机构建镜像（仅本机需要）

仓库 POM **不声明** `<repositories>`。CI（ubuntu-latest）直连 Maven Central，无需任何配置。

如果你的本机访问 `repo.maven.apache.org` 存在 TLS 指纹级拦截，可启用仓库自带的阿里云镜像：

```bash
cp settings.xml ~/.m2/settings.xml   # 首次执行一次即可，之后 mvn 直连阿里云
```

> 该 `settings.xml` 仅用于本机构建，不会进入已发布的 POM，因此不会把镜像传染下游、也不会被 Central Portal 拒收。

## 设计约定（Spec 先行）

- **领域术语** 以仓库根 [CONTEXT.md](CONTEXT.md) 为准。
- **机制设计** 以 [docs/spec/](docs/spec/) 的 Spec 为准（00-overview 总入口 + 01–55 共 55 份详设）。**改机制先改 Spec**：蓝本明确描述的机制严格遵循，留白处的自主推演以 `> 【推演】` 标注。
- 公共 API 在各模块的 `api` 子包（语义版本承诺）；`internal` 子包跨模块禁止引用、不承诺兼容。

## 模块依赖规则（硬性）

依赖图是以 [`buzhou-core`](buzhou-core) 为根的两层星形，**物理无环**（详见 [docs/spec/09-modules-engineering.md](docs/spec/09-modules-engineering.md)）：

1. **feature 模块之间禁止直接依赖**（memory / spill / skills / mcp / guard / tools / store-* 互不依赖）；跨机制协作一律走 core 的事件总线或 core SPI。这是依赖白名单，不是建议。
2. 唯一允许的二层边：`buzhou-observe-otel` / `buzhou-observe-dashboard` 依赖 `buzhou-observability`。
3. store 实现（jdbc / redis）只依赖 core SPI，由用户按需引入、`buzhou.store.type` 激活。
4. 每个机制模块独立可用；`buzhou-spring-boot-starter` 只做依赖聚合、无代码。
5. 每模块一个 AutoConfiguration，`META-INF/spring/...AutoConfiguration.imports` 注册，`@ConditionalOnProperty("buzhou.<mech>.enabled")` 控制开关。

提交前请确认你的改动没有引入违反上述规则的依赖。

## 测试约定

- **行为变更必须带测试**：新功能先写测试再实现，修 bug 先写能复现的测试再改到通过。
- **持久化 SPI 契约测试**：`buzhou-core` 发布 test-jar，内含 `AbstractBuzhouStoresContractTest`；store 实现模块（jdbc / redis）依赖该 test-jar 并继承契约测试类，保证所有存储实现语义一致。新增 store 实现时复用此模式（索引契约同款：`AbstractSessionIndexContractTest`）。
- **黄金轨迹回归集**（spec 32/34）：机制行为改动须跑 examples `golden/` 包——`EventSequenceAssert`（testsupport）收集会话/全局事件流，断言类型子序列、间隔约束（neverAfter）、计数与 payload；`attach(session)` 会话面、`attachGlobal(runtime)` 全局面（forked 等发往分支通道的事件）。新机制落地时补一条确定性轨迹（脚本化输入→序列断言）。

## 提交约定

- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：`feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `chore:` …
- **一个 PR 只做一件事**；公共 API 变更需在 PR 描述中说明兼容性影响。
- 全模块同版本演进，统一由 [`buzhou-bom`](buzhou-bom) 收口；父 POM 的 enforcer 规则禁止对 `io.github.chyuan-cuihongyuan` 的依赖声明不同版本，子模块**不**声明自己的 `<version>`。

## Issue 与 PR

- **Bug** 请附：版本、最小复现、期望 / 实际行为。
- **功能请求** 请先开 Issue 讨论再动代码，避免重复或方向性返工。
- 提交 PR 前确认本地 `mvn verify` 通过；PR 模板会引导你描述动机、改动点与兼容性影响。

## 行为准则

参与本项目即代表你同意遵守 [行为准则](CODE_OF_CONDUCT.md)。请保持尊重与专业。

## API 稳定性政策（effort #5 / impl-75）

- 公开 API 面以 [docs/api-surface.md](docs/api-surface.md) 清单为准；`*.internal.*` 包与
  模块 `fromYml(Map)` 私有解析契约不属公开 API。
- 语义化版本：minor 只加不改，破坏性变更留 major。
- 废弃类型保留 ≥ 2 个 minor，javadoc `@deprecated` 注明替代；新公开类型标 `@since`。
