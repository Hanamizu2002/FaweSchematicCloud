# FAWESchematicCloud

FAWE 的 Arkitektonika 云端原理图扩展。将玩家剪贴板上传并返回下载链接，也可以从云端或本地文件加载到剪贴板，再用 `//paste` 粘贴。

## 安装

需要 Java 21、Paper 1.21 和 FastAsyncWorldEdit（编译依赖为 2.11.1）。将 `build/libs/*-all.jar` 放入服务器 `plugins` 后重启。首次运行生成 `plugins/FAWESchematicCloud/config.yml`，修改配置后重启生效。

## 命令与权限

| 命令 | 功能 | 权限 |
| --- | --- | --- |
| `/schemcloud download` | 上传当前剪贴板，返回可点击下载链接 | `worldedit.clipboard.download` |
| `/schemcloud load <filename> [format]` | 加载本地原理图 | `worldedit.clipboard.load` |
| `/schemcloud load url:<download_key> [format]` | 从 Arkitektonika 加载 | 上一项及 `worldedit.schematic.load.web` |

示例：

```text
//copy
/schemcloud download
/schemcloud load url:0123456789abcdef0123456789abcdef
/schemcloud load house.schem
/schemcloud load url:0123456789abcdef0123456789abcdef fast.2
//paste
```

主命令支持 `download`、`load` 的权限过滤补全。仅玩家可使用。插件不再注册 `//download` 或 `//schem`，FAWE 原生命令由 FAWE 自行管理。

上传使用 Sponge v3（FAWE `fast`）格式。远程加载默认 `fast`，旧版 Sponge v2 文件可指定 `fast.2`，其他格式使用 FAWE 支持的别名。本地加载按文件内容检测格式。

本地文件位于 FAWE 配置的 schematic 目录；启用 `PER_PLAYER_SCHEMATICS` 时默认使用玩家 UUID 子目录。访问其他玩家目录需要 `worldedit.schematic.load.other`，路径经规范化检查，不能越出允许目录。为避免跨玩家访问，普通玩家不再自动回退到共享根目录；共享文件可使用 FAWE 原生命令管理。文件对话框 `#` 语法不再由此插件提供。

## 配置

```yaml
arkitektonika:
  backendUrl: https://api.schem.alsace.team
  downloadUrl: https://api.schem.alsace.team/download/{key}
  deleteUrl: https://api.schem.alsace.team/delete/{key}
web:
  frontend: https://schem.alsace.team
  downloadUrl: https://api.schem.alsace.team/download/{key}
  deleteUrl: https://api.schem.alsace.team/delete/{key}
```

`backendUrl` 是 API 根地址；`arkitektonika.downloadUrl` 用于游戏内读取二进制原理图，不能指向 HTML 页面；`web.downloadUrl` 是发给玩家的链接，可指向下载页面或 API。模板保留 `{key}`。`frontend` 和删除链接配置为兼容旧配置保留，当前命令不提供删除功能，也不公开删除密钥。配置中的域名沿用本项目原值，部署时请替换成自己的服务。

## Arkitektonika API

[官方项目](https://github.com/IntellectualSites/Arkitektonika) 接受 GZIP 压缩的 NBT 数据：

- `POST /upload`：multipart 字段 `schematic`；200 返回 `download_key`、`delete_key`。
- `GET /download/<download_key>`：200 返回文件；404 表示找不到；本地 Arkitektonika 实现使用 420 表示过期或文件缺失，旧文档写作 410。本插件对非 200 响应报告 HTTP 状态。
- `DELETE /delete/<delete_key>`：删除文件。删除密钥不能当作下载密钥，也不应公开。

上传和文件读取在后台执行，成功加载后回到服务器主线程替换剪贴板；读取失败保留原剪贴板。远程读取连接超时 15 秒，读取超时 30 秒。

## 结构与构建

- `FAWESchematicCloud`：插件生命周期、后台执行器、命令注册。
- `commands/SchemCloudCommand`：主命令分发、玩家检查、补全。
- `commands/DownloadCommand`、`LoadCommand`：上传和加载流程。
- `util/SchematicUploader`：序列化、API 上传、临时文件清理。
- `build.gradle.kts`：依赖、打包和唯一的插件描述生成入口。

```sh
./gradlew test shadowJar
```

API 契约测试使用本地模拟 HTTP 服务检查真实 Java 客户端的 multipart 字段、响应密钥和 413 错误传播；它不替代真实 Arkitektonika NBT 校验及 Paper/FAWE 联调。

## 限制

不支持多剪贴板上传；未声明 Folia 支持。大型原理图仍需要足够内存。实际服务器加载、跨插件剪贴板并发操作和真实云端上传需在目标环境验证。
