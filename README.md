# Image Converter 图像转换工具

## 项目描述 (Project Description)
这是一个Java图像转换工具，支持多种图像格式的相互转换。
This is a Java-based image conversion utility that supports conversion between multiple image formats.

## 先决条件 (Prerequisites)
- Java开发工具包 (JDK) 8或更高版本 
  (Java Development Kit (JDK) 8 or higher)
- Apache Maven

## 项目设置与安装 (Setup and Installation)
1. 克隆仓库 (Clone the repository)
2. 确保已安装Maven (Ensure Maven is installed)
3. 进入项目目录 (Navigate to the project directory)

## 构建项目 (Building the Project)
使用以下Maven命令编译项目：
(Use the following Maven command to compile the project:)
```bash
mvn clean compile
```

## 运行测试 (Running Tests)
使用以下命令运行测试套件：
(Run the test suite with:)
```bash
mvn test
```

### 测试覆盖范围 (Test Coverage)
测试套件包括以下场景：
(The test suite includes the following scenarios:)
- 转换各种扩展名的图像（PNG, JPG, GIF, BMP, TIFF, JFIF, WebP）
  (Converting images with various extensions: PNG, JPG, GIF, BMP, TIFF, JFIF, WebP)
- 处理有无文件扩展名的图像
  (Handling images with and without file extensions)
- 将图像转换为PNG格式
  (Converting images to PNG format)

### 测试资源 (Test Resources)
测试图像位于 `src/test/resource/` 目录，包括：
(Test images are located in the `src/test/resource/` directory and include:)
- computer.jpg
- Egale.gif
- grass.bmp
- sample_tiff_640.tiff
- photo.webp
- sample_jfif.jfif

## 测试输出 (Test Output)
转换后的图像保存在 `src/test/resource/tmpoutput/` 目录。
(Converted images are saved in the `src/test/resource/tmpoutput/` directory.)

## 运行应用程序 (Running the Application)
使用以下命令运行主应用程序：
(To run the main application:)
```bash
mvn exec:java
```

## 故障排除 (Troubleshooting)
- 确保所有Maven依赖已下载
  (Ensure all Maven dependencies are downloaded)
- 检查是否安装了最新的JDK
  (Check that you have the latest JDK installed)
- 使用 `mvn --version` 验证Maven安装
  (Verify your Maven installation with `mvn --version`)

## 测试详情 (Test Details)
测试类位于：
(Test class is located at:)
`src/test/java/image/convert/creality/ImageConverterTest.java`

输入的图片文件保存在：
(Input image files are stored in:)
`/src/test/resource/`
