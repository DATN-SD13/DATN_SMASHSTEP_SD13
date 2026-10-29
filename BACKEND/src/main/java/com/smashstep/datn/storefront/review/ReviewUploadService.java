package com.smashstep.datn.storefront.review;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.config.ThuMucAnhSanPham;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Service
public class ReviewUploadService {
    public static final long MAX_SIZE = 5L * 1024 * 1024;
    private final Path directory;

    public ReviewUploadService(@Value("${smashstep.review-images.directory:uploads/reviews}") String directory) {
        this.directory = ThuMucAnhSanPham.resolve(directory);
    }

    public Path directory() { return directory; }

    public String upload(MultipartFile file, Long customer) {
        if (file == null || file.isEmpty()) throw AppException.badRequest("Vui lòng chọn ảnh đánh giá");
        if (file.getSize() > MAX_SIZE) throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB");
        String extension = switch (file.getContentType() == null ? "" : file.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw AppException.badRequest("Chỉ hỗ trợ ảnh JPG hoặc PNG");
        };
        try {
            byte[] content;
            try (var input = file.getInputStream()) { content = input.readNBytes((int) MAX_SIZE + 1); }
            if (content.length > MAX_SIZE) throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB");
            validateContent(content, extension);
            // Decode and re-encode to reject broken files and strip appended content/metadata.
            try (var stream = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(content))) {
                var readers = javax.imageio.ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw AppException.badRequest("Không đọc được ảnh. Hãy chọn ảnh JPG hoặc PNG khác");
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 8000 || height > 8000 || (long) width * height > 20000000)
                        throw AppException.badRequest("Ảnh tối đa 8.000 px mỗi chiều và 20 megapixel");
                    var image = reader.read(0);
                    if (image == null) throw AppException.badRequest("Ảnh bị hỏng");
                    var output = new java.io.ByteArrayOutputStream();
                    if (!javax.imageio.ImageIO.write(image, extension, output)) throw AppException.badRequest("Không thể xử lý ảnh");
                    content = output.toByteArray();
                    if (content.length > MAX_SIZE) throw AppException.badRequest("Ảnh sau xử lý vượt 5 MB; hãy giảm kích thước ảnh");
                } finally { reader.dispose(); }
            }
            Files.createDirectories(directory);
            String name = customer + "-" + UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(name).normalize();
            if (!target.startsWith(directory)) throw AppException.badRequest("Đường dẫn ảnh không hợp lệ");
            Files.write(target, content, java.nio.file.StandardOpenOption.CREATE_NEW);
            return "/uploads/reviews/" + name;
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh đánh giá, vui lòng thử lại");
        }
    }

    public boolean validReference(String url) {
        return url != null && url.matches("/uploads/reviews/(?:[1-9][0-9]*-)?[a-f0-9-]{36}\\.(?:jpg|png|webp)")
                && Files.isRegularFile(directory.resolve(url.substring(url.lastIndexOf('/') + 1)));
    }

    public java.util.List<String> validateReferences(java.util.List<String> urls, Long customer, java.util.List<String> previous) {
        if (urls == null) return previous;
        if (urls.size() > 5) throw AppException.badRequest("Mỗi đánh giá tối đa 5 ảnh");
        var result = new java.util.LinkedHashSet<String>();
        for (String url : urls) {
            if (!validReference(url) || (!previous.contains(url) && !url.startsWith("/uploads/reviews/" + customer + "-")))
                throw AppException.badRequest("Ảnh đánh giá không hợp lệ hoặc không thuộc tài khoản của bạn; vui lòng tải lại ảnh");
            if (!result.add(url)) throw AppException.badRequest("Ảnh đánh giá không được trùng nhau");
        }
        return java.util.List.copyOf(result);
    }

    private static void validateContent(byte[] bytes, String extension) {
        boolean matches = switch (extension) {
            case "jpg" -> bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
            case "png" -> bytes.length > 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "webp" -> bytes.length >= 20 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP");
            default -> false;
        };
        if (!matches) throw AppException.badRequest("Tệp tải lên không phải ảnh hợp lệ");
    }

    private static boolean ascii(byte[] bytes, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            if (bytes[offset + i] != (byte) text.charAt(i)) return false;
        }
        return true;
    }
}
