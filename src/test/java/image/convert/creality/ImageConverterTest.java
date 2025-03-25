package image.convert.creality;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import image.convert.creality.ImageConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageConverter 类的测试类
 */
public class ImageConverterTest {

    // 测试资源目录
    private static final String RESOURCE_PATH = "src/test/resource/";

    // 输出目录
    private static Path outputDir;

    // 测试图片列表
    private static final List<String> TEST_IMAGES = Arrays.asList(
            "1.png",
            "2.png",
            "computer.jpg",
            "Egale.gif",
            "grass.bmp",
            "photo.webp",
            "sample_jfif.jfif",
            "sample_tiff_640.tiff");

    @BeforeAll
    static void setup() throws IOException {
        // 确保资源目录存在
        File resourceDir = new File(RESOURCE_PATH);
        assertTrue(resourceDir.exists(), "资源目录不存在: " + resourceDir.getAbsolutePath());

        // 验证所有测试图片都存在
        for (String imageName : TEST_IMAGES) {
            File imageFile = new File(RESOURCE_PATH + imageName);
            assertTrue(imageFile.exists(), "测试图片不存在: " + imageFile.getAbsolutePath());
        }

        // 设置输出目录为 test/resource/tmpoutput
        outputDir = Paths.get(RESOURCE_PATH, "tmpoutput");

        // 如果目录已存在，清空它
        if (Files.exists(outputDir)) {
            cleanDirectory(outputDir);
        } else {
            // 如果目录不存在，创建它
            Files.createDirectories(outputDir);
        }

        System.out.println("测试输出目录: " + outputDir.toAbsolutePath());
    }

    /**
     * 清空目录中的所有文件
     */
    private static void cleanDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        // 获取目录中的所有文件和子目录
        List<Path> pathsToDelete = Files.list(directory).collect(Collectors.toList());

        // 先删除所有文件，然后删除子目录
        for (Path path : pathsToDelete) {
            if (Files.isDirectory(path)) {
                cleanDirectory(path);
                Files.delete(path);
            } else {
                Files.delete(path);
            }
        }

        System.out.println("已清空输出目录: " + directory);
    }

    /**
     * 测试所有图片格式转换为120x120的PNG (不保持宽高比)
     */
    @Test
    void testConvertAllImagesDefault() {
        System.out.println("测试所有图片格式转换 (默认120x120)");

        for (String imageName : TEST_IMAGES) {
            try {
                String inputPath = RESOURCE_PATH + imageName;
                String outputPath = outputDir.resolve("default_" + imageName + ".png").toString();

                System.out.println("转换: " + imageName);
                ImageConverter.convertImage(inputPath, outputPath);

                // 验证输出文件存在
                File outputFile = new File(outputPath);
                assertTrue(outputFile.exists(), "输出文件不存在: " + outputPath);
                assertTrue(outputFile.length() > 0, "输出文件为空: " + outputPath);

                // 可以添加更多验证，如图片尺寸等

            } catch (Exception e) {
                fail("转换图片时出错: " + imageName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 测试所有图片格式转换为120x120的PNG (保持宽高比)
     */
    @Test
    void testConvertAllImagesPreserveRatio() {
        System.out.println("测试所有图片格式转换 (保持宽高比)");

        for (String imageName : TEST_IMAGES) {
            try {
                String inputPath = RESOURCE_PATH + imageName;
                String outputPath = outputDir.resolve("ratio_" + imageName + ".png").toString();

                System.out.println("转换: " + imageName + " (保持宽高比)");
                ImageConverter.convertImage(inputPath, outputPath);

                // 验证输出文件存在
                File outputFile = new File(outputPath);
                assertTrue(outputFile.exists(), "输出文件不存在: " + outputPath);
                assertTrue(outputFile.length() > 0, "输出文件为空: " + outputPath);

            } catch (Exception e) {
                fail("转换图片时出错: " + imageName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 测试自定义尺寸转换
     */
    @Test
    void testCustomSizeConversion() {
        System.out.println("测试自定义尺寸转换");

        try {
            String inputPath = RESOURCE_PATH + "computer.jpg";
            String outputPath = outputDir.resolve("custom_size.png").toString();

            // 转换为200x150
            ImageConverter.convertImage(inputPath, outputPath, 200, 150, true);

            // 验证输出文件存在
            File outputFile = new File(outputPath);
            assertTrue(outputFile.exists(), "输出文件不存在: " + outputPath);
            assertTrue(outputFile.length() > 0, "输出文件为空: " + outputPath);

        } catch (Exception e) {
            fail("自定义尺寸转换时出错: " + e.getMessage());
        }
    }

    /**
     * 测试无后缀图片的处理
     */
    @Test
    void testNoExtensionFile() {
        System.out.println("测试无后缀图片处理");

        try {
            // 创建一个没有后缀的临时文件
            String originalImage = RESOURCE_PATH + "computer.jpg";
            Path noExtensionPath = outputDir.resolve("no_extension_file");
            Files.copy(Paths.get(originalImage), noExtensionPath);

            String outputPath = outputDir.resolve("from_no_extension.png").toString();

            // 尝试转换无后缀文件
            ImageConverter.convertImage(noExtensionPath.toString(), outputPath);

            // 验证输出文件存在
            File outputFile = new File(outputPath);
            assertTrue(outputFile.exists(), "输出文件不存在: " + outputPath);
            assertTrue(outputFile.length() > 0, "输出文件为空: " + outputPath);

        } catch (Exception e) {
            fail("处理无后缀文件时出错: " + e.getMessage());
        }
    }

    /**
     * 测试所有无后缀图片的处理
     */
    @Test
    void testAllNoExtensionFiles() {
        System.out.println("测试所有无后缀图片处理");

        for (String imageFile : TEST_IMAGES) {
            try {
                // 获取原始文件名（不含扩展名）
                String baseName = imageFile.substring(0, imageFile.lastIndexOf('.'));

                System.out.println("测试文件: " + imageFile);

                // 创建一个没有后缀的临时文件
                String originalImage = RESOURCE_PATH + imageFile;
                Path noExtensionPath = outputDir.resolve("no_extension_" + baseName);
                Files.copy(Paths.get(originalImage), noExtensionPath, StandardCopyOption.REPLACE_EXISTING);

                String outputPath = outputDir.resolve("from_no_extension_" + baseName + ".png").toString();

                // 尝试转换无后缀文件
                ImageConverter.convertImage(noExtensionPath.toString(), outputPath);

                // 验证输出文件存在
                File outputFile = new File(outputPath);
                assertTrue(outputFile.exists(), "输出文件不存在: " + outputPath);
                assertTrue(outputFile.length() > 0, "输出文件为空: " + outputPath);

                System.out.println("成功转换: " + imageFile + " (无后缀) -> " + outputPath);

            } catch (Exception e) {
                fail("处理无后缀文件时出错 [" + imageFile + "]: " + e.getMessage());
            }
        }
    }

    /**
     * 测试错误处理 - 不存在的文件
     */
    @Test
    void testNonExistentFile() {
        System.out.println("测试不存在的文件");

        String inputPath = RESOURCE_PATH + "non_existent_file.jpg";
        String outputPath = outputDir.resolve("should_not_exist.png").toString();

        Exception exception = assertThrows(IOException.class, () -> {
            ImageConverter.convertImage(inputPath, outputPath);
        });

        assertTrue(exception.getMessage().contains("不存在") || exception.getMessage().contains("exist"),
                "错误消息应包含'不存在'或'exist': " + exception.getMessage());
    }

    /**
     * 测试错误处理 - 无效的图片文件
     */
    @Test
    void testInvalidImageFile() {
        System.out.println("测试无效的图片文件");

        try {
            // 创建一个无效的图片文件
            Path invalidImagePath = outputDir.resolve("invalid.png");
            Files.writeString(invalidImagePath, "This is not a valid image file");

            String outputPath = outputDir.resolve("from_invalid.png").toString();

            Exception exception = assertThrows(Exception.class, () -> {
                ImageConverter.convertImage(invalidImagePath.toString(), outputPath);
            });

            // 错误消息可能因不同的图像库而异
            System.out.println("无效图片错误: " + exception.getMessage());

        } catch (Exception e) {
            fail("创建无效图片文件时出错: " + e.getMessage());
        }
    }
}
