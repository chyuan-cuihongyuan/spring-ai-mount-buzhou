# 16 — 内置原子工具包

**What to build:** read_file/write_file/run_command/http_request/todo 首发（+机制衍生 read_range/load_skill/evidence 回查已随前票就位）；默认开关矩阵（无害默认开/危险 opt-in 挂守卫）；安全边界默认全开：文件根目录沙箱+realpath 防逃逸、命令沙箱+黑名单+超时、HTTP SSRF 拦内网与元数据端点（DNS 解析后校验）；todo 入 SessionStateStore 跨实例续接；瘦 Schema+内置 Skill 承载深度说明。

**Blocked by:** 12, 10

**Status:** done（实现 86b13f4；复审修复同提交：spill 壳类异常包装保旧 catch 语义、run_command 进程树终止+管道排空宽限防悬挂、todo remove 如实报数、SSRF 逐 IP 全量校验+CIDR 前缀校验；spec 06 增补推演 #12–#15；http_request 方法粒度 HITL 归 ticket 27，AutoConfiguration 归 ticket 20）

- [x] 危险工具默认不出现在工具清单，opt-in 后挂 HITL 守卫（AtomicToolsIntegrationTest：阻断→授权→放行全链路）
- [x] 沙箱逃逸（../、符号链接）、黑名单命令、SSRF 内网地址被拦有测试（FileToolsTest/RunCommandToolTest/HttpRequestToolTest）
- [x] todo 写入会话 state，跨实例续接后清单仍在（TodoToolTest.persistsAcrossInstances + 集成测试 Attachment 注入验证）
- [x] 每个工具参数 Schema 与 06 spec 一致（ToolsModuleTest.schemasMatchSpec 逐工具断言）
