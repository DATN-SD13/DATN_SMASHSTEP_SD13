package com.smashstep.datn.common.upload;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.config.ThuMucAnhSanPham;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AvatarUploadService {
    public static final long MAX_SIZE = 5L * 1024 * 1024;
    private final Path directory;

    public AvatarUploadService(@Value("${smashstep.avatar-images.directory:uploads/avatars}") String directory) {
        this.directory = ThuMucAnhSanPham.resolve(directory);
    }

    public Path directory() { return directory; }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw AppException.badRequest("Vui lòng chọn ảnh đại diện");
        if (file.getSize() > MAX_SIZE) throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB");
        String extension = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw AppException.badRequest("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP");
        };
        try {
            byte[] content;
            try (var input = file.getInputStream()) { content = input.readNBytes((int) MAX_SIZE + 1); }
            if (content.length > MAX_SIZE) throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB");
            validateContent(content, extension);
            Files.createDirectories(directory);
            String name = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(name).normalize();
            if (!target.startsWith(directory)) throw AppException.badRequest("Đường dẫn ảnh không hợp lệ");
            // Original filename is never used in the filesystem path.
            Files.write(target, content, java.nio.file.StandardOpenOption.CREATE_NEW);
            return "/uploads/avatars/" + name;
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh đại diện, vui lòng thử lại");
        }
    }

    private static void validateContent(byte[] bytes, String extension) throws IOException {
        boolean matches = switch (extension) {
            case "jpg" -> bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
            case "png" -> bytes.length > 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "webp" -> bytes.length >= 20 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP")
                    && (ascii(bytes, 12, "VP8 ") || ascii(bytes, 12, "VP8L") || ascii(bytes, 12, "VP8X"))
                    && Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()) == bytes.length - 8L;
            default -> false;
        };
        if (!matches) throw AppException.badRequest("Nội dung tệp không khớp định dạng ảnh");
        if (!"webp".equals(extension)) {
            try (var input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw AppException.badRequest("Tệp ảnh bị hỏng");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    long pixels = (long)reader.getWidth(0) * reader.getHeight(0);
                    if (pixels <= 0 || pixels > 16_000_000) throw AppException.badRequest("Ảnh quá lớn về kích thước điểm ảnh");
                    if (reader.read(0) == null) throw AppException.badRequest("Tệp ảnh bị hỏng");
                } catch (javax.imageio.IIOException ex) {
                    throw AppException.badRequest("Tệp ảnh bị hỏng");
                } finally { reader.dispose(); }
            }
        }
    }

    private static boolean ascii(byte[] bytes, int offset, String text) {
        for (int i = 0; i < text.length(); i++) if (bytes[offset + i] != text.charAt(i)) return false;
        return true;
    }
}
