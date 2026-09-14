package io.github.chyuan_cuihongyuan.buzhou.core.coverage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1503 / T2257：核心 API 包类级 Javadoc 覆盖门（纪律变测试，spec 213 绑定
 * 矩阵先例）——session / hook / exec / spi / observability / error 六包的公共
 * 顶层类型必须有类级 Javadoc（CLAUDE.md「api 子包与 SPI 必须有 Javadoc」在
 * 实际分包结构下的落点：core 内核公共 API 面）。注解夹层感知：Javadoc 与
 * public 声明之间允许注解行。
 */
class CoreApiJavadocCoverageTest {

    /** 覆盖门辖管的内核公共包（相对 buzhou-core 根包）。 */
    private static final List<String> GOVERNED_PACKAGES =
            List.of("session", "hook", "exec", "spi", "observability", "error");

    private static final Pattern PUBLIC_TYPE_DECL = Pattern.compile(
            "^public\\s+(?:final\\s+|abstract\\s+)?(?:class|interface|enum|record|sealed)\\s+\\w+",
            Pattern.MULTILINE);

    private static Path coreRoot() {
        return Path.of(System.getProperty("user.dir"));
    }

    @Test
    void everyCoreApiPublicTypeShouldHaveClassJavadoc() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String pkg : GOVERNED_PACKAGES) {
            Path pkgDir = coreRoot().resolve("src/main/java")
                    .resolve("io/github/chyuan_cuihongyuan/buzhou/core").resolve(pkg);
            if (!Files.isDirectory(pkgDir)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(pkgDir)) {
                files.filter(f -> f.getFileName().toString().endsWith(".java")).forEach(f -> {
                    try {
                        String src = Files.readString(f);
                        Matcher m = PUBLIC_TYPE_DECL.matcher(src);
                        if (m.find() && !hasClassJavadoc(src, m.start())) {
                            missing.add(pkg + "/" + f.getFileName());
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("读源码失败：" + f, e);
                    }
                });
            }
        }
        assertThat(missing)
                .as("核心 API 包缺类级 Javadoc 的公共类型（新公共类型必须带一句角色描述）：%s", missing)
                .isEmpty();
    }

    /** 从 public 声明向上跳过注解行与空行，首个有效内容必须是 Javadoc 块的收尾符。 */
    private static boolean hasClassJavadoc(String src, int declStart) {
        String[] lines = src.substring(0, declStart).split("\n", -1);
        int i = lines.length - 1;
        while (i >= 0 && (lines[i].strip().startsWith("@") || lines[i].isBlank())) {
            i--;
        }
        return i >= 0 && lines[i].stripTrailing().endsWith("*/");
    }
}
