# 02 — 持久化强度三档（SYNC / ASYNC 默认 / EXIT）+ store 契约扩展

**What to build:** 平台集成者按部署/绑定选择会话状态落盘强度档位；存储实现（内存/jdbc/redis）的写路径按档位缓冲——`SYNC` 同步落盘后返回、`ASYNC`（默认）边写边落、`EXIT` 仅会话关闭时 flush。编排方（记忆写路径）**不按档位分支**，档位纯属存储侧策略。共享契约测试用**并发观测**断言三档语义（不依赖崩溃），会话打开时记 `durability-tier` 事件。

**Blocked by:** 无 — 可立即开始（纯存储侧，可与 01/03 并行）

**Status:** done

- [ ] 提供按部署配置的持久化档位（默认 `ASYNC`），经 yml 可配
- [ ] 内存/jdbc/redis 三后端写路径按档位缓冲：`SYNC` append/put 同步落盘、`ASYNC` shortly after 持久、`EXIT` 仅 close 时 flush
- [ ] 编排方（记忆写路径）不按档位分支；档位由存储实现侧读取并生效（不新增 SPI）
- [ ] 共享契约测试 `AbstractBuzhouStoresContractTest` 扩展三档断言（并发观测：`SYNC` 立即可见且抗中途失效、`ASYNC` 最终持久、`EXIT` 仅 close 后持久），jdbc/redis/内存三后端继承通过
- [ ] 会话打开时记 `durability-tier` 事件（生效档位进 observability）
- [ ] `EXIT` 档 flush 钩子就位，供 06 优雅停机 drain 调用（联动留 06）
