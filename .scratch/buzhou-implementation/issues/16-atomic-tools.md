# 16 — 内置原子工具包

**What to build:** read_file/write_file/run_command/http_request/todo 首发（+机制衍生 read_range/load_skill/evidence 回查已随前票就位）；默认开关矩阵（无害默认开/危险 opt-in 挂守卫）；安全边界默认全开：文件根目录沙箱+realpath 防逃逸、命令沙箱+黑名单+超时、HTTP SSRF 拦内网与元数据端点（DNS 解析后校验）；todo 入 SessionStateStore 跨实例续接；瘦 Schema+内置 Skill 承载深度说明。

**Blocked by:** 12, 10

**Status:** ready-for-agent

- [ ] 危险工具默认不出现在工具清单，opt-in 后挂 HITL 守卫
- [ ] 沙箱逃逸（../、符号链接）、黑名单命令、SSRF 内网地址被拦有测试
- [ ] todo 写入会话 state，跨实例续接后清单仍在
- [ ] 每个工具参数 Schema 与 06 spec 一致
