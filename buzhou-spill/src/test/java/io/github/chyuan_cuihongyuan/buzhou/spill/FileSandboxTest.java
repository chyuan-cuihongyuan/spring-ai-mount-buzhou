package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSandboxTest {

    @TempDir
    Path root;

    @TempDir
    Path outside;

    private FileSandbox sandbox() {
        return new FileSandbox(root, List.of());
    }

    @Test
    void resolvesRelativePathWithinRoot() throws Exception {
        Files.writeString(root.resolve("a.txt"), "hi");
        Path resolved = sandbox().resolve("a.txt");
        assertThat(resolved).isEqualTo(root.resolve("a.txt").toRealPath());
    }

    @Test
    void resolvesAbsolutePathWithinRoot() throws Exception {
        Path file = root.resolve("b.txt");
        Files.writeString(file, "hi");
        assertThat(sandbox().resolve(file.toString())).isEqualTo(file.toRealPath());
    }

    @Test
    void rejectsDotDotEscape() {
        assertThatThrownBy(() -> sandbox().resolve("../escape.txt"))
                .isInstanceOf(SandboxViolationException.class);
    }

    @Test
    void rejectsAbsolutePathOutsideRoots() {
        assertThatThrownBy(() -> sandbox().resolve(outside.resolve("x.txt").toString()))
                .isInstanceOf(SandboxViolationException.class);
    }

    @Test
    void resolvesWithinAdditionalAllowedRoot() throws Exception {
        Path file = outside.resolve("allowed.txt");
        Files.writeString(file, "hi");
        FileSandbox sandbox = new FileSandbox(root, List.of(outside));
        assertThat(sandbox.resolve(file.toString())).isEqualTo(file.toRealPath());
    }

    @Test
    void rejectsSymlinkEscape() throws Exception {
        Path secret = outside.resolve("secret.txt");
        Files.writeString(secret, "secret");
        Path link = root.resolve("link.txt");
        Files.createSymbolicLink(link, secret);
        assertThatThrownBy(() -> sandbox().resolve("link.txt"))
                .isInstanceOf(SandboxViolationException.class);
    }

    @Test
    void resolveForWriteAcceptsNonExistentInsideRoot() throws Exception {
        Path resolved = sandbox().resolveForWrite("new/file.txt");
        assertThat(resolved).isEqualTo(root.toRealPath().resolve("new/file.txt"));
    }

    @Test
    void resolveForWriteRejectsOutsideRoot() {
        assertThatThrownBy(() -> sandbox().resolveForWrite("../new.txt"))
                .isInstanceOf(SandboxViolationException.class);
        assertThatThrownBy(() -> sandbox().resolveForWrite(outside.resolve("n.txt").toString()))
                .isInstanceOf(SandboxViolationException.class);
    }

    @Test
    void resolveForWriteRejectsSymlinkedParentEscape() throws Exception {
        Path linkDir = root.resolve("linked-dir");
        Files.createSymbolicLink(linkDir, outside);
        assertThatThrownBy(() -> sandbox().resolveForWrite("linked-dir/new.txt"))
                .isInstanceOf(SandboxViolationException.class);
    }
}
