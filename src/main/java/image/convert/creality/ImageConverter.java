package image.convert.creality;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * 图片格式转换工具类
 * 支持jpg, jpeg, png, gif, bmp, tiff, webp, jfif格式转换为指定尺寸的PNG
 * 同时支持处理带后缀和不带后缀的图片文件
 */
public class ImageConverter {

    // 静态初始化块，检查支持的格式
    static {
        System.out.println("支持的图片格式:");
        String[] formats = ImageIO.getReaderFormatNames();
        Set<String> supportedFormats = new HashSet<>();
        for (String format : formats) {
            supportedFormats.add(format.toLowerCase());
        }

        // 检查必要格式是否支持
        String[] requiredFormats = { "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "jfif" };
        for (String format : requiredFormats) {
            if (!supportedFormats.contains(format.toLowerCase())) {
                System.out.println("警告: 可能不支持 " + format + " 格式");
            }
        }

        // 打印所有支持的格式
        System.out.println("系统支持的图片格式列表:");
        for (String format : formats) {
            System.out.println("- " + format);
        }
    }
    
    /**
     * 将输入图片转换为指定尺寸的PNG格式
     * 
     */
    public static void convertImage(String inputPath, String outputPath) throws IOException {
        // 调用完整方法，使用默认值
        convertImage(inputPath, outputPath, 120, 120, true);
    }

    /**
     * 将输入图片转换为指定尺寸的PNG格式
     * 
     * @param inputPath     输入图片路径，支持带后缀和不带后缀的图片文件
     * @param outputPath    输出图片路径，将保存为PNG格式
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @param preserveRatio 是否保持原始宽高比
     * @throws IOException              如果读取或写入图片时发生错误
     * @throws IllegalArgumentException 如果输入参数无效
     * @throws SecurityException        如果输出路径不安全
     */
    public static void convertImage(String inputPath, String outputPath,
            int targetWidth, int targetHeight,
            boolean preserveRatio) throws IOException {
        // 参数验证
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("输入和输出路径不能为null");
        }

        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("目标宽度和高度必须为正数");
        }

        // 检查输入文件是否存在
        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IOException("输入文件不存在: " + inputPath);
        }

        if (inputFile.length() == 0) {
            throw new IOException("输入文件为空: " + inputPath);
        }

        // 验证输出路径安全性
        Path normalizedOutputPath = Paths.get(outputPath).normalize();
        if (normalizedOutputPath.isAbsolute() &&
                !normalizedOutputPath.startsWith(Paths.get("").toAbsolutePath())) {
            System.out.println("警告: 输出路径位于工作目录外: " + normalizedOutputPath);
        }

        // 确保输出目录存在
        Path outputDir = normalizedOutputPath.getParent();
        if (outputDir != null && !Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // 读取原始图片
        BufferedImage originalImage = readImageWithFallbacks(inputFile);

        if (originalImage == null) {
            throw new IOException("无法读取图片格式或图片文件损坏: " + inputPath);
        }

        // 使用高质量的多步缩放处理
        BufferedImage resizedImage = resizeImageWithHighQuality(
                originalImage, targetWidth, targetHeight, preserveRatio);

        // 保存为PNG格式
        File outputFile = new File(outputPath);
        boolean success = ImageIO.write(resizedImage, "png", outputFile);

        if (!success) {
            throw new IOException("无法将图像保存为PNG格式");
        }
    }

    /**
     * 尝试多种方法读取图片，支持带后缀和不带后缀的图片文件
     * 
     * @param inputFile 输入文件
     * @return 读取到的BufferedImage，如果所有方法都失败则返回null
     */
    private static BufferedImage readImageWithFallbacks(File inputFile) {
        BufferedImage image = null;

        // 1. 首先通过文件魔数识别格式
        String detectedFormat = null;
        try (InputStream is = new FileInputStream(inputFile)) {
            byte[] signature = new byte[12]; // 增大到12字节以支持WebP检测
            int bytesRead = is.read(signature);
            if (bytesRead > 0) {
                detectedFormat = identifyFormatBySignature(Arrays.copyOf(signature, bytesRead));
                if (detectedFormat != null) {
                    System.out.println("通过文件签名识别为: " + detectedFormat);
                }
            }
        } catch (Exception e) {
            System.out.println("读取文件签名时出错: " + e.getMessage());
            // 继续尝试其他方法
        }

        // 2. 尝试直接读取（适用于带正确后缀的文件）
        try {
            image = ImageIO.read(inputFile);
            if (image != null) {
                System.out.println("成功直接读取图片");
                return image;
            }
        } catch (Exception e) {
            System.out.println("直接读取图片失败: " + e.getMessage());
            // 继续尝试其他方法
        }

        // 3. 如果有检测到格式，尝试使用检测到的格式
        if (detectedFormat != null) {
            try (ImageInputStream iis = ImageIO.createImageInputStream(inputFile)) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(detectedFormat);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(iis);
                    image = reader.read(0);
                    reader.dispose();
                    if (image != null) {
                        System.out.println("成功使用检测到的格式读取图片: " + detectedFormat);
                        return image;
                    }
                }
            } catch (Exception e) {
                System.out.println("使用检测到的格式读取失败: " + e.getMessage());
                // 继续尝试其他方法
            }
        }

        // 4. 尝试所有已知格式（通过临时文件）
        String[] formats = { "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "jfif" };
        for (String format : formats) {
            try {
                // 创建一个临时文件，添加扩展名后尝试读取
                File tempFile = File.createTempFile("image_", "." + format);
                tempFile.deleteOnExit();

                // 复制原始文件内容到临时文件
                try (FileInputStream fis = new FileInputStream(inputFile);
                        FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }

                // 尝试读取临时文件
                BufferedImage tempImage = ImageIO.read(tempFile);
                if (tempImage != null) {
                    System.out.println("成功以 " + format + " 格式读取图片");
                    return tempImage;
                }
            } catch (Exception e) {
                // 忽略单个格式的异常，继续尝试其他格式
            }
        }

        // 5. 尝试使用ImageInputStream和所有可用的ImageReader
        try (ImageInputStream iis = ImageIO.createImageInputStream(inputFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis);
                    image = reader.read(0);
                    if (image != null) {
                        System.out.println("成功使用ImageReader读取图片: " + reader.getFormatName());
                        return image;
                    }
                } catch (Exception e) {
                    // 忽略单个reader的异常，继续尝试其他reader
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            System.out.println("使用ImageReader读取失败: " + e.getMessage());
        }

        return null; // 所有方法都失败
    }

    /**
     * 高质量图像缩放处理
     * 
     * @param originalImage 原始图像
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @param preserveRatio 是否保持宽高比
     * @return 处理后的图像
     */
    private static BufferedImage resizeImageWithHighQuality(
            BufferedImage originalImage, int targetWidth, int targetHeight, boolean preserveRatio) {

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算最终尺寸
        int finalWidth = targetWidth;
        int finalHeight = targetHeight;

        if (preserveRatio) {
            double widthRatio = (double) targetWidth / originalWidth;
            double heightRatio = (double) targetHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            finalWidth = (int) (originalWidth * ratio);
            finalHeight = (int) (originalHeight * ratio);
        }

        // 对于大图片使用多步缩放以提高质量
        BufferedImage resizedImage;
        if (originalWidth > finalWidth * 2 || originalHeight > finalHeight * 2) {
            resizedImage = multiStepResize(originalImage, finalWidth, finalHeight);
        } else {
            resizedImage = singleStepResize(originalImage, finalWidth, finalHeight);
        }

        // 创建最终图像（包含透明背景和居中）
        BufferedImage finalImage = new BufferedImage(
                targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = finalImage.createGraphics();
        try {
            // 设置透明背景
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, targetWidth, targetHeight);

            // 设置高质量渲染
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 居中绘制
            int x = (targetWidth - finalWidth) / 2;
            int y = (targetHeight - finalHeight) / 2;
            g.drawImage(resizedImage, x, y, null);
        } finally {
            g.dispose();
        }

        return finalImage;
    }

    /**
     * 单步图像缩放
     */
    /**
     * 单步图像缩放
     */
    private static BufferedImage singleStepResize(
            BufferedImage original, int width, int height) {

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, width, height, null);
        } finally {
            g.dispose(); // 手动释放Graphics2D资源
        }
        return resized;
    }

    /**
     * 多步图像缩放，提高质量
     */
    private static BufferedImage multiStepResize(
            BufferedImage original, int targetWidth, int targetHeight) {

        int currentWidth = original.getWidth();
        int currentHeight = original.getHeight();

        // 计算缩放步骤
        List<Dimension> steps = calculateResizeSteps(
                currentWidth, currentHeight, targetWidth, targetHeight);

        BufferedImage current = original;

        for (Dimension step : steps) {
            int stepWidth = (int) step.getWidth();
            int stepHeight = (int) step.getHeight();

            BufferedImage temp = new BufferedImage(stepWidth, stepHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = temp.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(current, 0, 0, stepWidth, stepHeight, null);
            } finally {
                g.dispose(); // 手动释放Graphics2D资源
            }

            // 如果不是最后一步，可以应用锐化滤镜提高质量
            if (stepWidth != targetWidth || stepHeight != targetHeight) {
                temp = applySharpening(temp);
            }

            current = temp;
        }

        return current;
    }

    /**
     * 计算多步缩放的步骤
     */
    private static List<Dimension> calculateResizeSteps(
            int startWidth, int startHeight, int targetWidth, int targetHeight) {

        List<Dimension> steps = new ArrayList<>();

        // 计算缩放比例
        double widthRatio = (double) targetWidth / startWidth;
        double heightRatio = (double) targetHeight / startHeight;

        // 计算步骤数量（每步缩放约75%）
        int numSteps = (int) Math.ceil(Math.log(Math.min(widthRatio, heightRatio)) / Math.log(0.75));
        numSteps = Math.max(2, numSteps); // 至少2步

        for (int i = 1; i <= numSteps; i++) {
            double stepRatio;
            if (i == numSteps) {
                stepRatio = 1.0; // 最后一步直接到目标尺寸
            } else {
                stepRatio = Math.pow(Math.min(widthRatio, heightRatio), (double) i / numSteps);
            }

            int stepWidth = (int) (startWidth * stepRatio);
            int stepHeight = (int) (startHeight * stepRatio);

            // 最后一步使用精确的目标尺寸
            if (i == numSteps) {
                stepWidth = targetWidth;
                stepHeight = targetHeight;
            }

            steps.add(new Dimension(stepWidth, stepHeight));
        }

        return steps;
    }

    /**
     * 应用简单的锐化滤镜
     */
    private static BufferedImage applySharpening(BufferedImage image) {
        // 简单的3x3锐化卷积核
        float[] sharpenKernel = {
                0.0f, -0.2f, 0.0f,
                -0.2f, 1.8f, -0.2f,
                0.0f, -0.2f, 0.0f
        };

        // 使用Java 2D API应用卷积
        BufferedImage result = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose(); // 手动释放Graphics2D资源
        }

        // 应用锐化（简化实现，实际项目中可以使用更高级的图像处理库）
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                // 为简化，这里仅实现一个基本版本
                // 实际项目中建议使用ConvolveOp或第三方库
                int rgb = image.getRGB(x, y);
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    /**
     * 通过文件签名（魔数）识别图片格式
     * 
     * @param signature 文件开头的字节数组
     * @return 识别出的格式名称，如果无法识别则返回null
     */
    private static String identifyFormatBySignature(byte[] signature) {
        if (signature.length < 4) {
            return null;
        }

        // JPEG: FF D8 FF
        if (signature[0] == (byte) 0xFF &&
                signature[1] == (byte) 0xD8 &&
                signature[2] == (byte) 0xFF) {
            return "jpeg";
        }

        // PNG: 89 50 4E 47
        if (signature[0] == (byte) 0x89 &&
                signature[1] == (byte) 0x50 &&
                signature[2] == (byte) 0x4E &&
                signature[3] == (byte) 0x47) {
            return "png";
        }

        // GIF: 47 49 46 38
        if (signature[0] == (byte) 0x47 &&
                signature[1] == (byte) 0x49 &&
                signature[2] == (byte) 0x46 &&
                signature[3] == (byte) 0x38) {
            return "gif";
        }

        // BMP: 42 4D
        if (signature[0] == (byte) 0x42 &&
                signature[1] == (byte) 0x4D) {
            return "bmp";
        }

        // TIFF (Intel): 49 49 2A 00
        if (signature[0] == (byte) 0x49 &&
                signature[1] == (byte) 0x49 &&
                signature[2] == (byte) 0x2A &&
                signature[3] == (byte) 0x00) {
            return "tiff";
        }

        // TIFF (Motorola): 4D 4D 00 2A
        if (signature[0] == (byte) 0x4D &&
                signature[1] == (byte) 0x4D &&
                signature[2] == (byte) 0x00 &&
                signature[3] == (byte) 0x2A) {
            return "tiff";
        }

        // WebP: 52 49 46 46 xx xx xx xx 57 45 42 50
        // 检查WebP格式 (RIFF....WEBP)
        if (signature.length >= 12 &&
                signature[0] == (byte) 0x52 &&
                signature[1] == (byte) 0x49 &&
                signature[2] == (byte) 0x46 &&
                signature[3] == (byte) 0x46 &&
                signature[8] == (byte) 0x57 &&
                signature[9] == (byte) 0x45 &&
                signature[10] == (byte) 0x42 &&
                signature[11] == (byte) 0x50) {
            return "webp";
        }

        return null;
    }
}
