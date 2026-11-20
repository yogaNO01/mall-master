package com.macro.mall.controller;

import com.macro.mall.common.api.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
public class MediaUploadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUploadController.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("uuuu/MM");
    private static final Map<String, String> DIRECTORIES = Map.of(
            "product-gallery", "product/gallery", "product-sku", "product/sku",
            "product-detail-pc", "product/detail/pc", "product-detail-h5", "product/detail/h5",
            "brand-logo", "brand/logo", "brand-banner", "brand/banner",
            "category-icon", "category/icon", "marketing-banner", "marketing/banner",
            "content-image", "content/image");

    private final Path uploadRoot;

    public MediaUploadController(@Value("${MALL_UPLOAD_ROOT:/opt/macro/uploads}") String uploadRoot) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    }

    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file,
                                                    @RequestParam String purpose) {
        String directory = DIRECTORIES.get(purpose);
        if (directory == null) return CommonResult.failed("不支持的图片用途");
        if (file.isEmpty() || file.getSize() > 2L * 1024 * 1024) {
            return CommonResult.failed("请选择不超过 2 MB 的图片");
        }

        try {
            String extension = imageExtension(file);
            if (extension == null) return CommonResult.failed("仅支持有效的 JPG、PNG 或 GIF 图片");
            String folder = directory + "/" + MONTH.format(LocalDate.now());
            String name = UUID.randomUUID() + "." + extension;
            Path destination = Files.createDirectories(uploadRoot.resolve(folder)).resolve(name);
            try (var input = file.getInputStream()) {
                Files.copy(input, destination);
            } catch (IOException e) {
                Files.deleteIfExists(destination);
                throw e;
            }
            return CommonResult.success(Map.of("name", name, "url", "/media/" + folder + "/" + name));
        } catch (IOException e) {
            LOGGER.error("Failed to store image for purpose {}", purpose, e);
            return CommonResult.failed("图片上传失败，请检查服务器存储");
        }
    }

    private String imageExtension(MultipartFile file) throws IOException {
        try (var input = file.getInputStream(); var image = ImageIO.createImageInputStream(input)) {
            if (image == null) return null;
            var readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            try {
                reader.setInput(image, true, true);
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) return null;
                return switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> null;
                };
            } catch (IOException e) {
                return null;
            } finally {
                reader.dispose();
            }
        }
    }
}
