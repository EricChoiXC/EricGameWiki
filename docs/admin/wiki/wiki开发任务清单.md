# wiki开发任务清单

> wiki 模块开发任务清单，对应 `com.wiki.admin.wiki` 包。
> 本清单基于 [wiki业务逻辑.md](wiki业务逻辑.md)、[wiki技术方案.md](wiki技术方案.md)、[wikiAPI接口设计文档.md](wikiAPI接口设计文档.md)、[wiki数据库设计.md](wiki数据库设计.md) 拆解。
> 每个任务标注来源文档、前置依赖、验收标准与文档同步约束。

---

## 0. 文档控制约定

### 0.1 AI 使用约定

- 每个任务必须有唯一 `TASK ID`
- 每个任务标注来源文档、前置依赖、验收标准
- 任务状态：`待开始` / `进行中` / `已完成` / `已阻塞`
- 文档同步约束：任务完成时需同步更新的文档

### 0.2 文档版本记录

| 文档版本 | 修改日期 | 修改人 | 修改说明 | 审核人 |
|----------|----------|--------|----------|--------|
| v0.1 | 2026-09-20 | | 初始版本创建 | |

---

## 1. 总体规划

wiki 模块开发分为三期，每期可独立验收：

| 期次 | 名称 | 范围 | 核心交付 |
|------|------|------|----------|
| 第一期 | 项目与数据项管理（含动态建表引擎） | wiki_main CRUD + wiki_main_data CRUD + 动态建表引擎 | 项目/数据项管理能力 + 动态表生成 |
| 第二期 | 数据维护（图鉴/关联/文档三类明细） | 动态表数据 CRUD + 三类型数据维护页面 | 数据明细增删改查 |
| 第三期 | wiki 页面维护 + 关联数据导入导出 | wiki 页面配置 + xlsx 导入导出 | wiki 展示配置 + 批量数据导入导出 |

---

## 2. 第一期：项目与数据项管理（含动态建表引擎）

### TASK-W1-01 搭建后端模块骨架

- **来源文档**: [wiki技术方案.md](wiki技术方案.md) 2.1、[业务流转公共规范.md](../../common/业务流转公共规范.md) 2.2
- **前置依赖**: 无
- **任务内容**:
  - 创建 `com.wiki.admin.wiki` 包及分层子包（controller/service/impl/dao/impl/dynamic/model/{request,response,dto,mapper}/util/properties/resolver）
  - 创建 `resources/wiki/` 资源目录与 `mappers/`、`roles.yml`、`application.yml`
  - `roles.yml` 新增权限码 `admin-wiki::ADMIN`（wiki 管理员）
- **验收标准**:
  - 包结构与 [wiki技术方案.md](wiki技术方案.md) 2.1 一致
  - Spring Boot 启动无报错
  - 权限码加载到 `admin_org_role` 表（复用既有 OrgStartupRunner 机制）
- **文档同步约束**: 无

### TASK-W1-02 创建元数据表 Flyway 迁移脚本

- **来源文档**: [wiki数据库设计.md](wiki数据库设计.md) 3、5.1
- **前置依赖**: TASK-W1-01
- **任务内容**:
  - 创建 `V1_0__3__create_wiki_tables.sql`，按 [wiki数据库设计.md](wiki数据库设计.md) 3.1-3.3 创建 `wiki_main`、`wiki_main_data`、`wiki_main_data_wiki_page` 三张表
  - 字段前缀 `field_`，公共字段遵循 [数据库公共规范.md](../../common/数据库公共规范.md)
  - 建立唯一索引 `uk_wiki_main_simple_name`、`uk_wiki_main_data_name`、`uk_wiki_page_data_id`
- **验收标准**:
  - Flyway 迁移成功执行
  - 表结构与 [wiki数据库设计.md](wiki数据库设计.md) 完全一致
  - 索引正确创建
- **文档同步约束**: 无

### TASK-W1-03 实现元数据表 DO / Mapper / Dao

- **来源文档**: [wiki数据库设计.md](wiki数据库设计.md) 3
- **前置依赖**: TASK-W1-02
- **任务内容**:
  - `WikiMainDo`（含 `fieldExtend`、`fieldManagers` json 字段）、`WikiMainDataDo`（含 `fieldDataJson`）、`WikiPageDo`（含 `fieldWikiPage` blob）
  - `WikiMainMapper` / `WikiMainDataMapper` / `WikiPageMapper`（MyBatis Mapper 接口）
  - 对应 XML Mapper 文件，风格对齐 `AdminAttachmentMainMapper.xml`（resultMap + 标准 CRUD）
  - `IWikiMainDao`/`WikiMainDaoImpl`、`IWikiMainDataDao`/`WikiMainDataDaoImpl`、`IWikiPageDao`/`WikiPageDaoImpl`
- **验收标准**:
  - DO 字段与数据库表字段一一对应，遵循 lowerCamelCase + map-underscore-to-camel-case
  - Mapper XML 风格与既有模块一致
  - Dao 层不承载业务逻辑
- **文档同步约束**: 无

### TASK-W1-04 实现 WikiResolver 双层鉴权

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 5、[业务流转公共规范.md](../../common/业务流转公共规范.md) 4.6
- **前置依赖**: TASK-W1-03
- **任务内容**:
  - `WikiResolver`，提供 `requireAdmin()`、`requireMaintain(fieldMainId)` 方法
  - `requireAdmin`：校验 `admin-wiki::ADMIN` 权限
  - `requireMaintain`：ADMIN 权限 或 项目的 `field_managers` 包含当前用户
  - 维护人员判断通过 `WikiCRPService` 调用 `sys.org` `IOrgUserService`，禁止直接访问 `admin_org_user` 表
  - 记录可追踪鉴权审计日志（当前用户 + 接口标识 + 文档 ID + 允许/拒绝结论）
- **验收标准**:
  - 鉴权规则与 [wiki业务逻辑.md](wiki业务逻辑.md) 5.2 一致
  - 跨模块调用经 CRPService，参数为 Dto/Request 非 Do
  - 无权限时抛 `FORBIDDEN`
- **文档同步约束**: 无

### TASK-W1-05 实现 WikiCRPService 跨模块调用

- **来源文档**: [wiki技术方案.md](wiki技术方案.md) 7、[业务流转公共规范.md](../../common/业务流转公共规范.md) 4.2
- **前置依赖**: TASK-W1-04
- **任务内容**:
  - `IWikiCRPService` / `WikiCRPServiceImpl`
  - 封装 sys.org 用户校验、sys.attachment 文件读取（后续导入用）
  - 参数校验与异常封装，不承载业务逻辑
- **验收标准**:
  - 跨模块调用方向正确（CRPService → 其他模块 IService）
  - 异常封装为 BusinessException
- **文档同步约束**: 无

### TASK-W1-06 实现项目 CRUD Service / Controller

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.1、[wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 4
- **前置依赖**: TASK-W1-04、TASK-W1-05
- **任务内容**:
  - `IWikiMainService` / `WikiMainServiceImpl`：list/save/update + 简称全局唯一校验 + 名称多语言模糊查询 + 维护人员用户有效性校验
  - `WikiMainController`：`@RequestMapping("/api/v1/admin/wiki")`，list/save/load/update/init 接口
  - `WikiFieldMaps`：项目列表查询字段白名单（含 `m.` 别名前缀）
  - 请求/响应对象放 `model/request/`、`model/response/`
- **验收标准**:
  - 接口路径、方法、报文与 [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 4 一致
  - 简称唯一性校验生效
  - 名称模糊查询同时匹配 `field_name`、`field_en_name`、`field_jp_name`
  - 鉴权在 Controller 调用 Service 前完成
- **文档同步约束**: 无

### TASK-W1-07 实现动态建表引擎核心

- **来源文档**: [wiki技术方案.md](wiki技术方案.md) 3、[wiki数据库设计.md](wiki数据库设计.md) 4
- **前置依赖**: TASK-W1-03
- **任务内容**:
  - `DynamicTableSqlBuilder`：根据 `WikiMainDo`（简称）+ `WikiMainDataDo`（简称、类型、明细 json）生成 CREATE TABLE / ALTER TABLE / DROP TABLE SQL
  - `DynamicFieldMaps`：根据元数据构建动态查询字段白名单
  - `WikiDynamicDataDao` / `WikiDynamicDataDaoImpl`：执行 DDL、表存在性校验、动态表 CRUD
  - 列名/表名白名单正则校验 `^[a-z0-9_]+$`，列类型由枚举映射
  - 图鉴类自动补充 name/code 固定列，文档类补充 name/code/context 固定列
  - DDL 幂等（`CREATE TABLE IF NOT EXISTS`）
- **验收标准**:
  - 三类型动态表 DDL 生成与 [wiki技术方案.md](wiki技术方案.md) 3.5 示例一致
  - 表名/列名经白名单校验，杜绝 SQL 注入
  - 建表前校验表不存在，删表前校验表存在
- **文档同步约束**: 无

### TASK-W1-08 实现数据项 CRUD Service / Controller（含动态建表）

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.2、[wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 5、[wiki技术方案.md](wiki技术方案.md) 3.7
- **前置依赖**: TASK-W1-06、TASK-W1-07
- **任务内容**:
  - `IWikiMainDataService` / `WikiMainDataServiceImpl`：list/save/update/delete
  - save：校验简称项目内唯一 → 保存 `wiki_main_data` → 触发动态建表引擎建表；DDL 失败需补偿删除元数据记录
  - update：`fieldDataName`/`fieldDataType` 不可改；明细变更仅允许 `ALTER TABLE ADD COLUMN`
  - delete：校验被引用关系 → 删除元数据 → `DROP TABLE`；被引用时返回 CONFLICT
  - `WikiMainDataController`：`@RequestMapping("/api/v1/admin/wiki/data")`，list/save/load/update/delete/init
- **验收标准**:
  - 接口与 [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 5 一致
  - 数据项创建后动态表正确生成
  - 简称项目内唯一校验生效
  - 被引用数据项删除被拒绝（CONFLICT）
  - DDL 失败时元数据一致性保持（补偿机制）
- **文档同步约束**: 无

### TASK-W1-09 实现前端项目/数据项管理页面

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.1-6.2、[wiki系统.md](../wiki系统.md) 页面章节
- **前置依赖**: TASK-W1-06、TASK-W1-08
- **任务内容**:
  - `src/api/wiki.js`：基于 `createStandardApi` 生成项目接口集 + 数据项接口集
  - router 新增项目列表/新建/编辑、数据项列表/新建/编辑路由
  - 项目列表页：筛选区（名称模糊）、排序、按钮权限、操作列
  - 项目新建/编辑页：表单 + 拓展信息明细行（动态行）+ 维护人员多选
  - 数据项列表页：固定 field_id 降序、操作列
  - 数据项新建/编辑页：类型选择联动明细表显示、图鉴类固定 name/code 行、关联项选择源
- **验收标准**:
  - 路由路径与既有风格一致（`admin/wiki/...`）
  - API 封装按模块拆分，不散落 axios 请求
  - 简称校验（小写英文和数字）在前端拦截
  - 数据项明细表类型联动正确（enum 显示枚举项、join 显示关联项选择）
- **文档同步约束**: 无

---

## 3. 第二期：数据维护（图鉴/关联/文档三类明细）

### TASK-W2-01 实现通用动态查询层

- **状态**: 已完成（2026-09-15）
- **来源文档**: [wiki技术方案.md](wiki技术方案.md) 4
- **前置依赖**: TASK-W1-07
- **任务内容**: 完善 `WikiDynamicDataDao`（count/select/insert/update/deleteById/batchInsert/batchDeleteByIds）；通过 `SqlSession` 执行原生 SQL，`#{paramN}` 命名参数绑定；关联项查询支持 `joinClauses` 动态联表（LEFT JOIN 目标动态表），联表列名携带表别名前缀
- **验收标准**: 复用 `QueryConditionBuilder`/`SqlSortBuilder`，动态 FieldMaps 运行时构建；关联项模糊筛选正确联表查询；所有值通过命名参数绑定，无 SQL 注入
- **文档同步约束**: 无

### TASK-W2-02 实现数据明细 CRUD Service / Controller（三类型分发）

- **状态**: 已完成（2026-09-15）
- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.3、[wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 6
- **前置依赖**: TASK-W2-01
- **任务内容**: `IWikiDataService`/`WikiDataServiceImpl`（list/save/load/update/delete/batch-delete）；以 `fieldDataId` 为入口，加载元数据后按 `fieldDataType` 分发；图鉴类固定 name/code + 动态列、名称/编号模糊筛选、fieldCode 降序；关联项动态 join 列、关联数据模糊筛选、fieldId 降序、批量删除；文档类固定 name/code/context、名称/编号模糊筛选、fieldCode 降序；`WikiDataController`（`@RequestMapping("/api/v1/admin/wiki/data-item")`）
- **验收标准**: 三类型查询/排序规则与 [wiki业务逻辑.md](wiki业务逻辑.md) 6.3 一致；接口与 [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 6 一致；鉴权经 `WikiResolver.requireMaintain`
- **文档同步约束**: 无

### TASK-W2-03 实现前端数据维护页面（三类型）

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.3、[wiki系统.md](../wiki系统.md) 页面章节
- **前置依赖**: TASK-W2-02
- **任务内容**: 数据维护页面根据数据项类型动态路由；图鉴类列表页（名称/编号模糊筛选、编号降序、新建、编辑/删除）；关联项列表页（关联数据模糊筛选、fieldId 降序、多选框、导入/导出/批量删除、编辑/删除）；文档类列表页（名称/编号模糊筛选、编号降序、新建、编辑/删除）；图鉴类/关联项新建编辑页按数据项明细动态渲染表单项；文档类新建编辑页（标题/编号/内容富文本）
- **验收标准**: 三类型页面形态与 [wiki业务逻辑.md](wiki业务逻辑.md) 6.3 一致；动态表单根据元数据正确渲染控件；关联项选择控件下拉源来自目标动态表 field_name + field_id
- **文档同步约束**: 无

---

## 4. 第三期：wiki 页面维护 + 关联数据导入导出

### TASK-W3-01 实现 wiki 页面维护 Service / Controller

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.4、[wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 7
- **前置依赖**: TASK-W2-02
- **任务内容**: `IWikiPageService`/`WikiPageServiceImpl`（load/save/init）；init 返回显示信息可选源（本数据项明细 + 包含该数据项的关联类数据项）；save 保存 `fieldWikiPage` blob json，唯一约束 `fieldDataId`；`WikiPageController`（`@RequestMapping("/api/v1/admin/wiki/page")`）
- **验收标准**: 接口与 [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 7 一致；包含该数据项的关联类数据项查询正确
- **文档同步约束**: 无

### TASK-W3-02 实现 ImportExportProcessor 导入导出组件

- **来源文档**: [wiki技术方案.md](wiki技术方案.md) 6、[wiki业务逻辑.md](wiki业务逻辑.md) 6.5-6.6
- **前置依赖**: TASK-W2-01、TASK-W1-05
- **任务内容**: `ImportExportProcessor`（模板生成、文件解析 POI、数据校验、分批导入、导出）；模板下载首行标题行、关联数据显示 `${关联数据}编号`、不列附件类明细；导入读取文件→校验合理性→（异常跳过开关）→分批 batchInsert 每批 200 条→（失败跳过开关）；导出全量数据为 xlsx；复用 `WikiCRPService` 调用 `sys.attachment` 读取上传的 xlsx 附件
- **验收标准**: 导入流程与 [wiki业务逻辑.md](wiki业务逻辑.md) 6.5 一致；失败/异常跳过开关逻辑正确；未选失败跳过时整体回滚，选中时移除失败项继续；批次事务通过 `TransactionTemplate` 编程式控制
- **文档同步约束**: 无

### TASK-W3-03 实现数据明细导入导出接口

- **来源文档**: [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) 6（API-W207/W208/W209）
- **前置依赖**: TASK-W3-02
- **任务内容**: `WikiDataController` 新增 template（GET 模板下载）、import（POST 导入）、export（POST 导出）接口；鉴权经 `WikiResolver.requireMaintain`
- **验收标准**: 接口与 [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) API-W207/W208/W209 一致；导入返回成功数/跳过数/失败明细
- **文档同步约束**: 无

### TASK-W3-04 实现前端 wiki 页面维护 + 导入导出页面

- **来源文档**: [wiki业务逻辑.md](wiki业务逻辑.md) 6.4-6.6、[wiki系统.md](../wiki系统.md) 页面章节
- **前置依赖**: TASK-W3-01、TASK-W3-03
- **任务内容**: wiki 页面维护页（显示信息/显示字段配置）；导入弹窗（复用 `AttachmentUploader.vue` 上传 xlsx、失败/异常跳过复选框、提交、结果展示）；导出按钮；router 新增 wiki 页面维护路由
- **验收标准**: wiki 页面维护交互与 [wiki业务逻辑.md](wiki业务逻辑.md) 6.4 一致；导入弹窗复用附件组件、开关项正确、结果展示完整
- **文档同步约束**: 无

---

## 5. 文档同步约束（全局）

| 变更项 | 需要同步的文档 | 动作 |
|--------|----------------|------|
| 数据项类型 / 固定列约定变化 | [wiki数据库设计.md](wiki数据库设计.md)、[wiki业务逻辑.md](wiki业务逻辑.md) | 新增/更新 |
| 动态建表规则变化 | [wiki技术方案.md](wiki技术方案.md)、[wiki数据库设计.md](wiki数据库设计.md) | 新增/更新 |
| 接口路径 / 报文变化 | [wikiAPI接口设计文档.md](wikiAPI接口设计文档.md) | 新增/更新 |
| 业务流程变化 | [wiki业务逻辑.md](wiki业务逻辑.md) | 新增/更新 |
| 权限规则变化 | [wiki系统.md](../wiki系统.md) | 新增/更新 |
| 任务状态变化 | 本文档 | 更新状态 |
