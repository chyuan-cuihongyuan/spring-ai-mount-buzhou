package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileSandbox {

    private final Path root;
    private final List<Path> allowedRoots;

    public FileSandbox(Path root, List<Path> additionalAllowedRoots) {
        this.root = realpathOrAbsolute(root);
        this.allowedRoots = (additionalAllowedRoots == null ? List.<Path>of() : additionalAllowedRoots)
                .stream().map(FileSandbox::realpathOrAbsolute).toList();
    }

    public Path root() {
        return root;
    }

    public Path resolve(String raw) {
        Path candidate = absolutize(raw);
        Path check = Files.exists(candidate) ? realpath(candidate) : candidate;
        if (!contains(check)) {
            throw new SandboxViolationException("路径越出沙箱：" + raw);
        }
        return check;
    }

    public Path resolveForWrite(String raw) {
        Path candidate = absolutize(raw);
        Path parent = candidate.getParent();
        if (parent == null) {
            throw new SandboxViolationException("路径越出沙箱：" + raw);
        }
        Path realParent = Files.exists(parent) ? realpath(parent) : parent;
        Path resolved = realParent.resolve(candidate.getFileName().toString());
        if (!contains(resolved)) {
            throw new SandboxViolationException("路径越出沙箱：" + raw);
        }
        return resolved;
    }

    public boolean contains(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return true;
        }
        return allowedRoots.stream().anyMatch(normalized::startsWith);
    }

    private Path absolutize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new SandboxViolationException("路径为空");
        }
        Path path = Path.of(raw);
        if (!path.isAbsolute()) {
            path = root.resolve(path);
        }
        return path.normalize();
    }

    private static Path realpath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            throw new SandboxViolationException("路径解析失败：" + path);
        }
    }

    private static Path realpathOrAbsolute(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }
}
