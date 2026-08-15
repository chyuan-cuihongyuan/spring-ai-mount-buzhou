---
Type: task
Status: closed
---
## Question

社区文件：.github 缺 ISSUE_TEMPLATE（bug/feature/security 三型）与 PR 模板/PULL_REQUEST_TEMPLATE——贡献者入口不完整。补齐？

## Resolution

AFK 自决：补。ISSUE_TEMPLATE bug_report（复现步骤/模块/版本/日志摘录 checklist）/feature_request（动机/方案草图/是否愿提 PR）；security 指向 SECURITY.md 私密披露流程（不建公开 issue 模板，用联系方式引导）；PULL_REQUEST_TEMPLATE（变更摘要/spec 链接/测试证据/破坏性变更 checklist）。产 impl-114（并入 CONTRIBUTING 引用）。

### 闭合细化（实现期定稿）

- 图表勘察误判：bug/feature/PR 三模板已存在——真实缺口只有两处：bug_report 结构化（模块/store 形态/证据指引/私密披露提示/PR 意愿）与 config.yml（security 私密披露 + Discussions 引导，blank_issues 关闭）。feature_request/PR 模板现状已够用，不动。
