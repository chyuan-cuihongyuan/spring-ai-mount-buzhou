package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共面快照测试（T215 / impl-179 / effort#13）：扫描各模块 src/main 非 internal 包
 * public 类型全集（与 docs/api-surface.md 审计口径一致），与黄金快照
 * {@code docs/api-surface.snapshot.txt} 比对——新增/移除公开类型未更新快照即失败
 * （防意外 API 漂移；有意变更 = 更新快照 + api-surface.md 同步入档）。
 *
 * <p>快照更新：跑 {@link #regenerateSnapshot()}（-Dtest=ApiSurfaceSnapshotTest#regenerateSnapshot）
 * 覆写快照文件后随变更入档。
 */
class ApiSurfaceSnapshotTest {

    private static final Path SNAPSHOT = Path.of("..", "docs", "api-surface.snapshot.txt");

    @Test
    void publicTypeUniverseMatchesSnapshot() throws Exception {
        // classpath 形态门：单模块跑（依赖走 ~/.m2 旧 jar）扫描集不完整必假红——
        // 防线在 reactor 联编（全仓 verify / CI）生效；单跑跳过（诚实边界入档）。
        String cp = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        org.junit.jupiter.api.Assumptions.assumeTrue(
                java.util.Arrays.stream(cp.split(":")).anyMatch(e -> e.endsWith("/classes")
                        && e.contains("buzhou-")),
                "非 reactor classpath（单模块跑）——快照比对跳过");
        Map<String, String> actual = scanPublicTypes();
        assertThat(actual.size()).isGreaterThanOrEqualTo(240); // 面量下限（只增不减粗闸）
        assertThat(Files.exists(SNAPSHOT)).as("快照文件存在（首跑先 regenerate）").isTrue();
        List<String> expected = Files.readAllLines(SNAPSHOT).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("#")).sorted().toList();
        List<String> actualLines = actual.entrySet().stream()
                .map(e -> e.getValue() + "|" + e.getKey()).sorted().toList();
        assertThat(actualLines)
                .as("公共面与快照不符——有意变更请 regenerateSnapshot 更新并同步 api-surface.md")
                .containsExactlyElementsOf(expected);
    }

    /** 快照再生成（维护操作；不入常规断言路径）。 */
    @Test
    void regenerateSnapshot() throws Exception {
        Map<String, String> actual = scanPublicTypes();
        StringBuilder sb = new StringBuilder(
                "# api-surface 黄金快照（impl-179 生成；模块|全限定名，字典序）\n"
                + "# 更新流程：regenerateSnapshot → 人工核对 diff → api-surface.md 同步入档\n");
        actual.entrySet().stream()
                .map(e -> e.getValue() + "|" + e.getKey())
                .sorted()
                .forEach(line -> sb.append(line).append('\n'));
        Files.writeString(SNAPSHOT, sb.toString());
    }

    /** 扫描 buzhou 模块 main 面：classpath 条目（jar 或 classes 目录）→ 非 internal public 类型。 */
    private static Map<String, String> scanPublicTypes() throws Exception {
        Map<String, String> result = new TreeMap<>();
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        for (String entry : classpath.split(":")) {
            String moduleName = moduleNameOf(entry);
            if (moduleName == null) {
                continue;
            }
            if (entry.endsWith(".jar")) {
                try (JarFile jar = new JarFile(entry);
                        Stream<JarEntry> entries = jar.stream()) {
                    entries.filter(e -> e.getName().endsWith(".class") && !e.getName().contains("$"))
                            .forEach(e -> collect(result, moduleName, entry,
                                    e.getName().replace('/', '.').replaceAll("\\.class$", "")));
                }
            } else {
                Path dir = Path.of(entry);
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(dir)) {
                    files.filter(p -> p.toString().endsWith(".class") && !p.getFileName().toString().contains("$"))
                            .forEach(p -> collect(result, moduleName, entry,
                                    dir.relativize(p).toString().replace('/', '.')
                                            .replaceAll("\\.class$", "")));
                }
            }
        }
        return result;
    }

    private static void collect(Map<String, String> result, String moduleName, String entryPath,
            String className) {
        if (!className.startsWith("io.github.chyuan_cuihongyuan.buzhou.")
                || className.contains(".internal.")) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className, false,
                    ApiSurfaceSnapshotTest.class.getClassLoader());
            if (!Modifier.isPublic(clazz.getModifiers())) {
                return;
            }
            result.put(className, moduleName);
        } catch (Throwable ignored) {
            // 不可加载类（可选依赖缺角的桥接类等）不入公共面
        }
    }

    private static String moduleNameOf(String entry) {
        File f = new File(entry);
        String name = f.getName();
        if (name.endsWith(".jar") && name.startsWith("buzhou-")) {
            return name.replaceAll("-[0-9].*\\.jar$", "");
        }
        // /path/buzhou-core/target/classes
        if (entry.contains("buzhou-") && entry.endsWith("/classes")) {
            String dir = f.getParentFile().getParentFile().getName();
            return dir.startsWith("buzhou-") ? dir : null;
        }
        return null;
    }
}
