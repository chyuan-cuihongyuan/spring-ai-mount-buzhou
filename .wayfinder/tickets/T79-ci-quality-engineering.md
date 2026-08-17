---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

CI 与质量工程收口：jaCoCo 覆盖率（先观测后设线？）、静态分析取舍（spotless/checkstyle/spotbugs/pmd/CodeQL 哪些进、哪些注记不做——注意 10K★ 政策对构建工具链的适用口径）、CI workflow 加固（concurrency 取消、timeout-minutes、测试报告上传、action 版本统一）、failsafe/重试要不要、多 JDK 矩阵是否本轮。

## Resolution

进本轮：
1. **jaCoCo**：根 pom pluginManagement + 全模块 activate（prepare-agent + report）；CI 上传报告构件；**先观测不设线**（BUNDLE 覆盖线注记为后续升硬项，避免大仓一次性卡线）。
2. 静态分析取舍：**spotless（google-java-format 不引入——改用简易 .editorconfig + mvn formatter 不做）**——结论：本轮只引 **spotbugs**（pluginManagement + ci 单独 job，先 observational 不卡门）与 **CodeQL workflow**（GitHub 原生，java 模式，PR/main）；checkstyle/pmd/spotless 注记不做（格式化大迁移违背最小 diff 收口期原则）。
3. CI workflow 加固：ci.yml 加 concurrency（cancel-in-progress）、timeout-minutes=30、surefire 报告上传（always）、action 版本统一 v5（release/redteam 跟齐）。
4. failsafe/重试：不做（测试全绿稳定，注记）；多 JDK 矩阵：不做（Boot 4.1/JDK21 基线单一，注记后续 Boot 升级时再议）。
5. maven-wrapper：补（mvnw + wrapper.properties，CI 用 ./mvnw，README 构建命令对齐）。（可推翻）
