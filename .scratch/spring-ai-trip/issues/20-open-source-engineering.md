# 开源工程化

Type: task
Status: open
Blocked by: —

## Question

开源发布的前置事务：确定 groupId（io.github.\<GitHub用户名\>，需提供用户名并在 Sonatype Central Portal 注册命名空间）；创建 GitHub 仓库 `spring-ai-mount-buzhou`（Apache-2.0、README、CONTRIBUTING、CODE_OF_CONDUCT、issue/PR 模板）；CI（GitHub Actions：构建 + 测试 + 静态检查）；Maven Central 发布通道（central portal publisher 还是 legacy OSSRH；签名 GPG key 准备）。其中注册账号/创建仓库/生成 key 是需要人做的部分——输出一份精确 checklist；能自动做的（模板文件草稿）直接产出。
