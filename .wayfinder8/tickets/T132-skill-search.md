---
Type: task
Status: open
---
## Question

skill_search 检索工具（effort #7 fog）：目录截断后模型无法发现未列出技能——除溢出提示外，是否提供运行时检索工具？

## Resolution

AFK 自决：是。skills 模块新增 `SkillSearchTool`（ToolCallback 直实现，LoadSkillTool 同款）：入参 query 子串——按名称与 description 不分大小写匹配（可见全集 listForPage().total 范围内、不受 catalog-max-entries 截断限制），返回匹配清单（name + description，上限 20 条）+ 提示可 load_skill 加载。auto 注册进 SkillModule.configure() 的 autoTools。产 spec 37 §A + impl-105。
