# 📚 Library Backend & Certificate Generator System

这是一个基于 **Spring Boot** 的后端系统，主要功能是管理教师信息，并集成 **Python 脚本** 动态生成教师培训证书（支持预览和下载 PDF/图片）。

## 🛠 技术栈

* **后端核心**: Java (Spring Boot 3.x), JPA (Hibernate)
* **数据库**: MySQL 8.0
* **脚本工具**: Python 3.x (用于图像处理和证书生成)
* **图像处理库**: Pillow (Python)

## 📂 目录结构说明

```text
project-root/
├── src/
│   ├── main/
│   │   ├── java/.../controller/TeacherController.java  # 核心控制器 (登录/下载/预览)
│   │   └── resources/
│   │       └── application.yml                         # 数据库与应用配置
│   └── python/
│       ├── cert_generator.py                           # 证书生成脚本
│       ├── requirements.txt                            # Python 依赖
│       ├── resources/                                  # [必须] 存放字体(.ttf)和模板图片(.png)
│       └── preview_resources/                          # [自动生成] 存放生成的预览图
```

## 环境准备（使用前必读）

在使用本系统前，请确保安装并准备好以下环境：

- **JDK 17+**：用于运行 Spring Boot 后端。
- **MySQL**：请在数据库中创建一个 `school_db`（或在 `application.yml` 中配置你要使用的库名）。
- **Python 3.x**：并将 `python` 命令加入系统 PATH，以便在终端直接运行 `python`。

## 🚀 快速开始

1. 配置 Python 环境

	进入 Python 目录并安装依赖（推荐使用虚拟环境）：

	```powershell
	cd src/python
	pip install -r requirements.txt
	```

2. 准备资源文件

	请确保 `src/python/resources/` 目录下包含以下必要文件（否则脚本会报错）：

	- 字体文件: `SIMLI.TTF`, `timesbd.ttf`
	- 模板图片: `潍坊-优秀.png`, `徐州-合格.png`, `社会人员证书-优秀.png` 等（脚本中会根据教师类型与等级拼接模板名）

3. 配置数据库

	根据 `src/main/resources/application.yml`中的设置，进行数据库连接：

	```yaml
	spring:
	  datasource:
		 url: jdbc:mysql://localhost:3306/school_db?useSSL=false&serverTimezone=UTC
		 username: root
		 password: root
	```
    实际的数据要自行导入，数据库资源在：src\main\resources\static\user_teacher.dbf
4. 启动项目

	在项目根目录运行（Windows）：

	```powershell
	.\mvnw.cmd spring-boot:run
	```

	启动后可以访问： `http://localhost:8080/teacher/test-env` 进行环境自检。

## 🔌 API 接口说明（摘要）

- 所有接口前缀：`/teacher`
- 登录：`POST /teacher/login`，body JSON `{username, password}`
- 找回账号：`POST /teacher/findAccount`，body JSON `{name, idCard}` 返回手机号
- 下载证书：`GET /teacher/downloadCertificate?phone=...`（返回 PDF 下载）
- 预览证书：`GET /teacher/previewCertificate?phone=...`（返回图片流，`image/png`）
- 环境自检：`GET /teacher/test-env`
- 调试：`GET /teacher/debug`

## ⚠️ 重要注意事项（开发必读）

1. 路径硬编码问题

	- `TeacherController` 中部分路径为硬编码（例如 `D:/...`）。部署到其他环境时请务必修改这些路径或将路径写入 `application.yml`。

2. Python 输出编码

	- Java 调用 Python 时对输出流的编码处理在不同地方可能使用 `GBK` 或 `UTF-8`。在 Windows 下常用 `GBK`，在 Linux/Mac/Docker 下应使用 `UTF-8`。建议统一或通过配置控制。

3. 字体与模板严格依赖文件名

	- `cert_generator.py` 会根据教师类型与等级拼接模板文件名，文件名必须与脚本中预期的完全一致。

## 💡 代码优化建议（Code Review Tips）

- 将所有路径统一通过 `application.yml` 配置，避免硬编码导致部署失败。
- 将 Python 调用与日志处理抽成工具方法并统一处理 stdout/stderr 的读取（建议异步读取避免阻塞）。
- 对临时文件实行自动清理策略，或将其放入系统 tmp 并定期清理。

## 🐛 常见报错与解决办法

- Pillow 报错：运行 `/test-env` 会提示缺少 Pillow，解决方法：`pip install Pillow`。
- 找不到资源文件：确认 `src/python/resources/` 中包含所需的 `.ttf` 和 `.png`。
- 生成卡住或进程等待异常：确认 `python` 命令可用且脚本路径正确。

---

## 2. Python 依赖文件 (`src/python/requirements.txt`)

请将以下内容保存为 `src/python/requirements.txt`：

```txt
Pillow>=9.0.0
```

把以上依赖写入 `README.md`（已包含），并在项目说明中提示用户安装。

