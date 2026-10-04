package com.smashstep.datn.product.config;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ThuMucAnhSanPham {
    private ThuMucAnhSanPham() {}

    public static Path resolve(String directory) {
        Path configured = Paths.get(directory);
        if (configured.isAbsolute()) return configured.normalize();
        // IntelliJ có thể chạy từ thư mục cha; đường dẫn tương đối vẫn thuộc module BACKEND.
        try {
            Path source = Paths.get(ThuMucAnhSanPham.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path root = Files.isDirectory(source) ? source : source.getParent();
            for (int level = 0; root != null && level < 4; level++, root = root.getParent()) {
                if (Files.isRegularFile(root.resolve("pom.xml"))) return root.resolve(configured).toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | IllegalArgumentException | SecurityException | FileSystemNotFoundException ignored) {
            // Bản đóng gói ngoài checkout dùng thư mục làm việc hoặc đường dẫn cấu hình tuyệt đối.
        }
        return configured.toAbsolutePath().normalize();
    }
}
