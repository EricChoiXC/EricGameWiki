# AGENTS

本文件用于约束本仓库内的人类开发者与 AI Agent 的协作方式。除非有明确说明，所有实现、设计、补文档与重构任务，均以 [开发规范.md](docs/common/开发规范.md) 为最高基线；本文件负责将其转换为当前仓库可直接执行的规则。

## 1. 仓库结构

当前仓库为 EricGameWiki 游戏 Wiki 项目，目录职责如下：

```text
EricGameWiki/
├─ docs/                     # 项目文档
│  ├─ common/                # 公共规范与项目说明
│  ├─ admin/            # 后台管理系统文档
│  └─ wiki/             # 前台wiki系统文档
├─ project/                  # 代码
│  ├─ backend/            # 后端代码
│  ├─ adminWeb/            # 后台管理系统前端代码
│  └─ wikiWeb/             # 前台wiki系统前端代码
├─ AGENTS.md                 # 仓库级协作与开发约束
└─ README.md
```

后端统一根包路径：

```text
com.eric.wiki
```

模块开发时必须优先遵循以下包结构：

```text
com.eric.wiki.{模块}
├─ controller    # 接口
├─ service    # 业务逻辑
│  ├─ impl    # 默认实现
│  ├─ listeners    # 事件监听
│  ├─ webservice    # 远程服务调用
│  └─ task    #  定时任务
├─ dao    # 数据访问
│  └─ impl    # 默认实现
├─ model    # 数据模型
│  ├─ requests    # 请求参数
│  ├─ responses    # 响应参数
│  ├─ dtos    # 视图对象
│  └─ mappers    # 数据转换
├─ utils    # 工具类
├─ properties    # 配置信息
└─ resolver    # 接口鉴权
```

后端资源文件默认结构：

```text
resources/
├─ ${模块}
|  ├─ mappers    # mappers文件夹
|  |   ├─ xxxMapper.xml    # mapper文件
|  |   └─ ....
|  ├─ application.yml    # 模块信息配置文件
|  ├─ roles.yml    # 权限信息配置文件
|  ├─ dict.yml    # 字典信息配置文件
|  ├─ process.yml    # 流程信息配置文件
|  └─ xxx.yml    # 其他配置文件
└─ ...
```

前端默认结构（以实际部署方案为准，文件夹内再按模块进行二级文件夹分割）：

```text
src
├─ api         # 接口封装
├─ router      # 路由
├─ stores      # Pinia 状态
├─ styles      # 全局样式
└─ views       # 页面视图
```

## 2. 文档优先级与冲突处理

### 2.1 基线文档

- 仓库总规范基线：`docs/common/开发规范.md`
- 系统文档目录：`docs/{系统名}`
- 模块级文档目录：`docs/{系统名}/{模块名}/`
- 每个功能模块至少应包含：
    - `{模块名}业务逻辑.md` / `{模块名}需求文档.md`
    - `{模块名}技术方案.md` / `{模块名}UI设计文档.md`
    - `{模块名}API接口设计文档.md`
    - `{模块名}数据库设计.md`

### 2.2 冲突优先级

当不同文档对同一事项描述冲突时，按"冲突项归属"判定最高标准：

- 业务流程、角色权限鉴权、异常处理：需求/业务逻辑文档优先
- 路由、菜单、页面布局：UI 设计文档优先
- 接口规则、接口约束、接口业务流程：API 设计文档优先
- 表结构、字段、索引、外键约束：数据库设计文档优先

若模块文档未覆盖某项约束，则回退到 [开发规范.md](docs/common/开发规范.md)。

## 3. 开发顺序与交付要求

新增模块、次级模块或较大功能时，默认执行顺序如下：

1. 先确认或补齐需求/业务逻辑文档
2. 再确认或补齐技术方案 / UI 设计文档
3. 再确认或补齐 API 设计文档
4. 再确认或补齐数据库设计文档
5. 最后进入前后端与迁移脚本实现

实现代码前必须先读与本次变更直接相关的文档，不允许仅凭已有代码猜测业务规则。

提交实现时应保证：

- 代码目录与文档中的模块边界一致
- 接口、表结构、页面行为与对应设计文档一致
- 若实现导致设计变更，必须同步更新相关文档
- 若选用某一架构方案实现，应同步完善对应 `project/project-{架构}/` 下的方案文档

## 4. 后端开发约束

### 4.1 分层职责

- `Controller`：只负责接收请求、参数校验、返回响应；不得编写业务逻辑
- `CRPService`：只负责模块间调用入口与参数校验；不得编写业务逻辑
- `WebService` / `RestService`：只负责外部系统调用入口与参数校验；不得编写业务逻辑
- `IService` / `Service`：承载业务逻辑；复杂流程应拆分子方法
- `IDao` / `Dao`：只负责与 `Mapper` 一对一对接；不得编写业务逻辑
- `Util`：仅放可复用的小型通用逻辑，不得成为业务逻辑堆积区
- `Resolver`：负责接口级鉴权，不得省略

### 4.2 接口规范

后端接口统一路径：

```text
/api/v1/{系统}/{模块}/{次级模块}/{请求方法}?{请求参数}
```

**URL 架构约束（前端页面对裸根，API 对前缀）：**

- 前端页面通过裸根路径 `/` 直接访问（如 `http://localhost:8083/`）
- 后端 API 统一通过 `/api/v1/{系统}/` 路径前缀访问
- **禁止**使用 `server.servlet.context-path` 在全局层面对静态资源和 API 做统一前缀
- 所有 Controller 的 `@RequestMapping` 必须显式包含完整路径前缀（如 `@RequestMapping("/api/v1/sys/org/user")`）
- SecurityConfig 中的 `.requestMatchers()` 同样需使用完整路径

通用接口命名与方法约束：

- `/init`：`GET`
- `/load?fieldId=${id}`：`GET`
- `/list`：`POST`
- `/save`：`POST`
- `/update`：`PATCH`
- `/delete?fieldId=${id}`：`DELETE`
- `/import`：`POST`
- `/export`：`POST`
- `/download?fieldId=${id}`：`GET`

其他接口继续遵循以下规则：

- 单 `id` 传参或无参查询使用 `GET`
- 包含非 `id` 查询条件使用 `POST`
- 更新仅使用 `PATCH`，禁止使用 `PUT`
- 删除使用 `DELETE`

### 4.3 鉴权规范

- 鉴权核心参数必须是"接口 + 文档 ID"
- 所有单据访问、编辑、删除、提交流程等操作，都必须传入文档 ID
- 每个接口都必须定义自身 `resolver`
- `resolver` 至少接收：当前用户、接口标识、文档 ID
- `resolver` 必须明确返回允许/拒绝结论，并给出可追踪原因或权限码
- Controller 或 Service 在执行核心逻辑前必须先完成鉴权
- 批量鉴权接口只用于权限预判，不能替代目标接口最终鉴权

## 5. 前端开发约束

- 前端基础路径前缀统一为 `/`，前端页面直接通过裸根路径访问
- 前端 Vite `base` 配置为 `/`，路由 history base 为 `/`
- 前端 Axios `baseURL` 配置为 `/api/v1/{系统}`，仅 API 请求走该前缀
- 前端技术栈以 `Vue 3 + Vite + Vue Router + Pinia + Element Plus + Axios` 为标准
- 前端页面、菜单、显示权限、布局与交互行为必须以 UI 设计文档为准
- 前端按钮显隐可基于批量鉴权接口做预判断，但不能替代后端最终鉴权
- API 封装应按模块拆分，避免在页面组件中直接散落请求代码
- 双系统部署下，管理员系统与访客系统应各自独立部署，分别维护入口

## 6. 数据库与迁移约束

- 数据库字段统一使用 `field_` 前缀
- 公共字段优先复用以下命名：
    - 主键 `field_id`
    - 编号 `field_no`
    - 名称/标题 `field_name`
    - 创建人 `field_creator_id`
    - 创建时间 `field_create_time`
    - 更新人 `field_updator_id`
    - 更新时间 `field_update_time`
    - 是否启用 `field_enable`
    - 版本号 `field_version`
- 除 `field_id`, `field_version`, `field_enable` 外，所有字段在数据库中均不设置为 `NOT NULL`
- 表结构、字段类型、索引、外键约束以数据库设计文档为准
- 数据库变更通过 Flyway 管理，不允许只改设计文档不补迁移脚本

## 7. 命名规范

### 7.1 后端命名

- 包名：全小写，按模块语义命名，如 `sys.org`、`sys.wiki`、`sys.attachment`
- `Controller` 类：`{业务名}Controller`
- `Service` 接口：`I{业务名}Service`
- `Service` 实现：`{业务名}ServiceImpl`
- `Dao` 接口：`I{业务名}Dao`
- `Dao` 实现：`{业务名}DaoImpl`
- `Resolver` 类：`{业务名}{动作名}Resolver` 或能清楚表达接口语义的命名
- 请求对象（`Request` 后缀）放 `model/requests/`，响应/视图对象（`Vo` 等后缀）放 `model/vos/`
- VO/DTO/响应对象：使用明确业务语义 + `Request` / `Response` / `Vo`
- DO：使用明确业务语义 + `Do`
- MyBatis Mapper：接口与 XML 同名，如 `OrgMapper.java` / `OrgMapper.xml`

### 7.2 前端命名

- 页面组件：`PascalCase`，建议以 `View.vue` 结尾，如 `HomeView.vue`
- 通用组件：`PascalCase.vue`
- `api` 目录文件：按模块使用小写或 `kebab-case`，如 `system.js`
- `stores` 目录文件：按状态域使用小写或 `kebab-case`，如 `app.js`
- 样式文件：使用小写或 `kebab-case`，如 `base.css`
- 路由 `name`、状态字段、方法名：采用可读性优先的英文语义命名

### 7.3 数据库命名

- 表名、字段名、索引名统一使用下划线风格
- 字段名统一保留 `field_` 前缀
- Flyway 脚本命名遵循：`V{版本号}_{小版本号}__{序列号}__{英文下划线描述}.sql`

### 7.4 JSON 传参命名

- 前后端 JSON 交互统一采用小驼峰命名（lowerCamelCase）
- 后端 Java 属性名、请求/响应对象（Request/Vo 等）的字段名与前端 JavaScript 中的属性名必须一致，均为小驼峰
- 禁止在后端或前端单独使用蛇形命名（snake_case）、串式命名（kebab-case）或其他与对方不一致的 JSON 属性名
- 例：数据库字段 `field_nickname` → 后端 DO 属性 `fieldNickname` → JSON `"fieldNickname"` → 前端 JS 属性 `fieldNickname`
- MyBatis `map-underscore-to-camel-case` 配置负责数据库列名到 Java 属性的转换，此规则不影响 JSON 命名

### 7.5 文档命名

- 系统文档统一放在 `docs/{系统名}/`
- 模块文档统一放在 `docs/{系统名}/{模块名}/`
- 文档文件名采用中文业务名 + 文档类型
- 标准文件名：
    - `{模块名}业务逻辑.md` / `{模块名}需求文档.md`
    - `{模块名}技术方案.md` / `{模块名}UI设计文档.md`
    - `{模块名}API接口设计文档.md`
    - `{模块名}数据库设计.md`
    - `{模块名}开发任务清单.md`

## 8. 编码规则

- 所有文件均使用 UTF-8 编码
- 不允许在 `Controller`、`Dao` 中编写业务逻辑
- 复杂业务流程必须在 `Service` 中拆分为可读的子方法
- 公共逻辑优先复用，禁止复制粘贴形成多份实现
- 新增接口时，必须同时检查接口路径、HTTP 方法、鉴权方式、请求响应对象命名是否符合规范
- 新增数据库对象时，必须同时检查字段前缀、公共字段、迁移脚本命名是否符合规范
- 不同后缀且不同功能的类文件应分开存放在不同子目录中（如 `*Request.java` 放 `requests/`，`*Vo.java` 放 `vos/`），禁止混放
- 新增页面时，必须同时检查路由、菜单、显示权限、接口调用是否与 UI/API 文档一致
- 文档、代码、数据库脚本三者不一致时，不得默认以"现有代码"覆盖设计，必须回到文档优先级规则判断

## 9. 模块范围基线

当前规划中的模块范围以实际文档为准，包含：

** 公共系统（sys）模块：**

| 中文名     | 包名         | 说明                                     |
| ---------- | ------------ | ---------------------------------------- |
| 用户管理   | org          | 用户、角色、权限管理                     |
| 附件管理   | attachment   | 附件上传、存储与删除                     |
| 审计模块   | audit        | 操作自动审计与人工查询                   |

公共系统后端模块统一归入 `com.eric.wiki.sys` 根包。

** WIKI模块：**

| 中文名         | 包名          | 说明                       |
| -------------- | ------------- | -------------------------- |
| WIKI库管理  | wiki         | 项目、数据项、文档项、页面配置           |

WIKI模块后端模块统一归入 `com.eric.wiki.wiki` 根包。

### 通用约束

新增模块前，应先补充设计文档并确认其包名、接口路径、表结构与权限模型。

## 10. Agent 执行准则

AI Agent 在本仓库工作时默认遵循以下流程：

1. 先读 `docs/common/开发规范.md` 与本次涉及模块文档
2. 再确认代码应落在哪个架构方案、哪个模块、哪个分层目录
3. 实现前核对接口路径、鉴权方式、表结构与命名
4. 实现后同步检查文档、代码、迁移脚本是否一致

若本文件与 [开发规范.md](docs/common/开发规范.md) 的明确条款冲突，以后者为准；若本文件未覆盖某项实现细节，按模块设计文档和相邻现有代码风格继续补齐。
