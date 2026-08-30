package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 文档覆盖门（spec 213 / T587，绑定矩阵先例——纪律变测试）：
 * ① docs/spec 每个 NNN-*.md 文件名被 README 引用（新 spec 不入表即红）；
 * ② README 的 docs/spec/ 链接全部实存（死链即红）。EXCLUDED 显式例外集。
 */
class SpecCoverageTest {

    /** 过渡期例外（显式登记——当前为空：撞号双文件按文件名各自可匹配）。 */
    private static final Set<String> EXCLUDED = Set.of();

    private static Path repoRoot() {
        // starter 测试基线：模块目录向上一级到仓库根（快照测试同款路径策略）
        return Path.of(System.getProperty("user.dir")).getParent();
    }

    private static List<String> specFileNames() throws IOException {
        try (Stream<Path> files = Files.list(repoRoot().resolve("docs/spec"))) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".md"))
                    .filter(n -> n.matches("\\d{1,3}-.*\\.md"))
                    .map(n -> n.substring(0, n.length() - 3))
                    .collect(Collectors.toList());
        }
    }

    private static String readme() throws IOException {
        return Files.readString(repoRoot().resolve("README.md"));
    }

    @Test
    void everySpecFileIsReferencedFromReadme() throws IOException {
        String readme = readme();
        List<String> uncovered = specFileNames().stream()
                .filter(name -> !EXCLUDED.contains(name))
                .filter(name -> !readme.contains(name))
                .toList();
        assertThat(uncovered)
                .as("docs/spec 下未被 README 引用的 spec（新能力必须入表——文档轮补或登记 EXCLUDED）")
                .isEmpty();
    }

    @Test
    void everyReadmeSpecLinkResolvesToRealFile() throws IOException {
        Set<String> existing = specFileNames().stream().collect(Collectors.toSet());
        Matcher matcher = Pattern.compile("docs/spec/([\\w.-]+\\.md)").matcher(readme());
        List<String> dead = new java.util.ArrayList<>();
        while (matcher.find()) {
            String referenced = matcher.group(1);
            String withoutExt = referenced.substring(0, referenced.length() - 3);
            if (!existing.contains(withoutExt)) {
                dead.add(referenced);
            }
        }
        assertThat(dead).as("README 中的死 spec 链接").isEmpty();
    }
}
