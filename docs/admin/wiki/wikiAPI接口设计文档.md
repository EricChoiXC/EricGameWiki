# wikiAPI接口设计文档

> wiki 模块 API 接口设计文档，对应 `com.wiki.admin.wiki` 包。
> 通用规范遵循 [接口公共规范.md](../../common/接口公共规范.md)，不再重复编写。
> 查询条件遵循 [查询标准.md](../../common/查询标准.md)。

---

## 0. 文档控制约定

### 0.1 AI 使用约定

- 标题层级和字段名保持稳定
- 所有不确定项写 `TBD`
- 所有不适用项写 `N/A（原因）`
- 所有枚举、状态、默认值、错误码显式列出
- 每个接口必须有唯一 `API ID`

### 0.2 文档版本记录

| 文档版本 | 修改日期 | 修改人 | 修改说明 | 审核人 |
|----------|----------|--------|----------|--------|
| v0.1 | 2026-09-20 | | 初始版本创建 | |

---

## 1. 文档定位与输入依据

### 1.1 契约源策略

- **人类可读设计文档**: 本 API 设计文档
- **机器可读契约文件**: N/A（后续由 API 设计文档生成 `openapi.yaml`）
- **同步规则**: API 设计文档为上游设计，实现后生成机器契约并保持同步

### 1.2 事实源优先级

| 优先级 | 文档类型 | 冲突处理规则 |
|--------|----------|--------------|
| P0 | 本 API 设计文档 | 接口路径、方法、参数、响应以本文档为准 |
| P1 | [wiki业务逻辑.md](wiki业务逻辑.md) | 业务流程、角色权限、异常处理以业务逻辑文档为准 |
| P2 | [wiki数据库设计.md](wiki数据库设计.md) | 表结构、字段以数据库设计文档为准 |
| P3 | 代码现状 | 实现过程中发现的问题需同步更新本文档 |

---

## 2. 通用契约规范

### 2.1 版本、命名与路径

- **Base path**: `/api/v1/admin/wiki`
- **版本策略**: 无版本前缀，通过向后兼容保证平滑升级
- **命名规范**: 资源路径统一使用英文小写，多词以 `/` 层级分隔

### 2.2 鉴权、授权与审计

- **认证方式**: 登录返回 JWT Token，前端在请求头携带 `Authorization: Bearer <token>`
- **授权方式**: 每个接口通过 `WikiResolver` 校验；权限模型为"ADMIN 权限 或 项目维护人员"双层鉴权
- **审计要求**: 写操作（增/删/改）统一记录审计日志

### 2.3 查询接口约定

遵循 [接口公共规范.md](../../common/接口公共规范.md) 3.3 查询接口约定与 [查询标准.md](../../common/查询标准.md)。

### 2.4 写接口约定

遵循 [接口公共规范.md](../../common/接口公共规范.md) 3.4 写接口约定。

### 2.5 通用请求 / 响应报文

遵循 [接口公共规范.md](../../common/接口公共规范.md) 3.5、3.6 节通用报文格式。

### 2.6 错误码约定

遵循 [接口公共规范.md](../../common/接口公共规范.md) 3.7 错误码约定，本模块特有错误码：

| 错误码 | HTTP 状态码 | 场景 | 调用方处理建议 |
|--------|-------------|------|----------------|
| CONFLICT | 409 | 项目简称不唯一 | 提示用户修改简称 |
| CONFLICT | 409 | 数据项简称项目内不唯一 | 提示用户修改简称 |
| CONFLICT | 409 | 动态表已存在 | 提示数据项表已存在 |
| CONFLICT | 409 | 删除被引用的数据项 | 提示该数据项被关联项引用 |
| NOT_FOUND | 404 | 动态表不存在 | 提示数据项表不存在 |
| BAD_REQUEST | 400 | 简称格式不符 | 检查简称格式（小写英文和数字） |
| BAD_REQUEST | 400 | 数据项类型不支持 | 检查数据项类型 |
| BAD_REQUEST | 400 | 导入文件读取失败 | 检查文件格式 |
| BAD_REQUEST | 400 | 导入数据不合理 | 查看错误明细 |

---

## 3. 接口分组总览

| 接口组 | Base path | 说明 |
|--------|-----------|------|
| 项目管理 | `/api/v1/admin/wiki` | wiki 项目 CRUD |
| 数据项管理 | `/api/v1/admin/wiki/data` | 项目下数据项 CRUD + 动态建表 |
| 数据明细维护 | `/api/v1/admin/wiki/data-item` | 数据项动态表数据 CRUD + 导入导出 |
| wiki 页面维护 | `/api/v1/admin/wiki/page` | 数据项 wiki 页面配置 |

---

## 4. 项目管理接口

### API-W001 项目列表查询

| 项 | 值 |
|----|-----|
| API ID | API-W001 |
| 路径 | `/api/v1/admin/wiki/list` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` |

**请求报文**（`data` 为查询条件 VO，`query.data` 为标准查询树）：

```json
{
  "query": {
    "pageNum": 1,
    "pageSize": 15,
    "needPage": true,
    "data": {
      "or": [
        {"like": {"fieldName": "最终幻想"}},
        {"like": {"fieldEnName": "最终幻想"}},
        {"like": {"fieldJpName": "最終幻想"}}
      ]
    }
  }
}
```

**响应报文**：

```json
{
  "success": true,
  "httpCode": 200,
  "code": "SUCCESS",
  "message": "操作成功",
  "list": [
    {
      "fieldId": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      "fieldName": "最终幻想9",
      "fieldEnName": "Final Fantasy IX",
      "fieldJpName": "ファイナルファンタジーIX",
      "fieldSimpleName": "ff9",
      "fieldPublishDate": "2000-07-07T00:00:00",
      "fieldCreateTime": "2026-09-20T10:00:00",
      "fieldStatus": 1,
      "fieldManagers": ["userId1", "userId2"]
    }
  ],
  "query": {
    "total": 1,
    "pages": 1,
    "pageNum": 1,
    "pageSize": 15
  }
}
```

### API-W002 项目新建

| 项 | 值 |
|----|-----|
| API ID | API-W002 |
| 路径 | `/api/v1/admin/wiki/save` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` |

**请求报文**：

```json
{
  "data": {
    "fieldName": "最终幻想9",
    "fieldEnName": "Final Fantasy IX",
    "fieldJpName": "ファイナルファンタジーIX",
    "fieldSimpleName": "ff9",
    "fieldPublishDate": "2000-07-07T00:00:00",
    "fieldStatus": 1,
    "fieldManagers": ["userId1", "userId2"],
    "fieldExtend": [
      {"name": "开发商", "type": "text", "value": "Square"},
      {"name": "发售数量", "type": "number", "value": "5500000"}
    ]
  }
}
```

**响应报文**：`data` 为新建项目 id（`fieldId`）。

### API-W003 项目加载

| 项 | 值 |
|----|-----|
| API ID | API-W003 |
| 路径 | `/api/v1/admin/wiki/load` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` |

**请求参数**：`fieldId`（项目 id）

**响应报文**：`data` 为项目完整数据（含 `fieldExtend`、`fieldManagers`）。

### API-W004 项目更新

| 项 | 值 |
|----|-----|
| API ID | API-W004 |
| 路径 | `/api/v1/admin/wiki/update` |
| 方法 | PATCH |
| 鉴权 | `admin-wiki::ADMIN` |

**请求报文**：`data` 含 `fieldId` 及可更新字段（`fieldSimpleName` 不可更新）。

### API-W005 项目删除

> N/A（项目不支持删除，仅支持启用/停用，通过 `/update` 更新 `fieldStatus`）

### API-W006 项目启用/停用

| 项 | 值 |
|----|-----|
| API ID | API-W006 |
| 路径 | `/api/v1/admin/wiki/update` |
| 方法 | PATCH |
| 鉴权 | `admin-wiki::ADMIN` |

**请求报文**：

```json
{
  "data": {
    "fieldId": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "fieldStatus": 0
  }
}
```

### API-W007 页面初始化

| 项 | 值 |
|----|-----|
| API ID | API-W007 |
| 路径 | `/api/v1/admin/wiki/init` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` |

**响应报文**：`data` 为页面初始化数据（维护人员候选用户列表等）。

---

## 5. 数据项管理接口

### API-W101 数据项列表查询

| 项 | 值 |
|----|-----|
| API ID | API-W101 |
| 路径 | `/api/v1/admin/wiki/data/list` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |
| 文档 ID | `fieldMainId`（项目 id，在请求中传递） |

**请求报文**：

```json
{
  "data": {
    "fieldMainId": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
  },
  "query": {
    "pageNum": 1,
    "pageSize": 15,
    "needPage": false,
    "sortField": "fieldId",
    "sortOrder": "desc"
  }
}
```

**响应报文**：

```json
{
  "success": true,
  "list": [
    {
      "fieldId": "数据项id",
      "fieldMainId": "项目id",
      "fieldName": "道具",
      "fieldDataName": "item",
      "fieldDataType": "data",
      "fieldDataJson": [
        {"name": "名称", "dataName": "name", "type": "text"},
        {"name": "编号", "dataName": "code", "type": "text"},
        {"name": "价格", "dataName": "price", "type": "number"}
      ]
    }
  ],
  "query": {"total": 1, "pages": 1, "pageNum": 1, "pageSize": 15}
}
```

### API-W102 数据项新建

| 项 | 值 |
|----|-----|
| API ID | API-W102 |
| 路径 | `/api/v1/admin/wiki/data/save` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |
| 副作用 | 创建 `wiki_${simpleName}_${dataName}` 动态表 |

**请求报文**：

```json
{
  "data": {
    "fieldMainId": "项目id",
    "fieldName": "道具",
    "fieldDataName": "item",
    "fieldDataType": "data",
    "fieldDataJson": [
      {"name": "价格", "dataName": "price", "type": "number"},
      {"name": "描述", "dataName": "desc", "type": "text"}
    ]
  }
}
```

> 图鉴类固定列 `name`（名称）、`code`（编号）由后端自动补充，前端不传。文档类无 `fieldDataJson`。

**响应报文**：`data` 为新建数据项 id（`fieldId`）。

### API-W103 数据项加载

| 项 | 值 |
|----|-----|
| API ID | API-W103 |
| 路径 | `/api/v1/admin/wiki/data/load` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldId`（数据项 id）

### API-W104 数据项更新

| 项 | 值 |
|----|-----|
| API ID | API-W104 |
| 路径 | `/api/v1/admin/wiki/data/update` |
| 方法 | PATCH |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |
| 副作用 | 明细表变更时执行 `ALTER TABLE ADD COLUMN` |

> `fieldDataName`、`fieldDataType` 不可更新；首版仅允许新增明细列（`ALTER TABLE ADD COLUMN`），禁止删除/修改已存在列。

### API-W105 数据项删除

| 项 | 值 |
|----|-----|
| API ID | API-W105 |
| 路径 | `/api/v1/admin/wiki/data/delete` |
| 方法 | DELETE |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |
| 副作用 | 删除 `wiki_${simpleName}_${dataName}` 动态表 |

**请求参数**：`fieldId`（数据项 id）

> 删除前校验该数据项是否被其他关联项明细引用，存在引用时返回 `CONFLICT`。

### API-W106 数据项初始化

| 项 | 值 |
|----|-----|
| API ID | API-W106 |
| 路径 | `/api/v1/admin/wiki/data/init` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldMainId`（项目 id）

**响应报文**：`data` 含数据项类型枚举、该项目已有图鉴类/文档类数据项列表（关联项选择源）。

---

## 6. 数据明细维护接口

> 数据明细接口以 `fieldDataId`（数据项 id）为入口，后端加载数据项元数据后根据 `fieldDataType` 分发处理。动态表数据通过通用动态查询层访问，报文 `data` 对象的动态列字段由元数据决定。

### API-W201 数据明细列表查询

| 项 | 值 |
|----|-----|
| API ID | API-W201 |
| 路径 | `/api/v1/admin/wiki/data-item/list` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |
| 文档 ID | `fieldDataId`（数据项 id） |

**请求报文**（图鉴类，按名称/编号模糊查询）：

```json
{
  "data": {
    "fieldDataId": "数据项id"
  },
  "query": {
    "pageNum": 1,
    "pageSize": 15,
    "needPage": true,
    "data": {
      "or": [
        {"like": {"fieldName": "长剑"}},
        {"like": {"fieldCode": "sword"}}
      ]
    }
  }
}
```

> 图鉴类/文档类排序固定 `fieldCode DESC`；关联项排序固定 `fieldId DESC`。关联项筛选区按关联数据模糊筛选，查询条件树中使用关联列字段（如 `fieldEnemyId`），后端联表目标动态表查询 `field_name`/`field_code`。

**响应报文**：

```json
{
  "success": true,
  "list": [
    {
      "fieldId": "记录id",
      "fieldName": "长剑",
      "fieldCode": "sword001",
      "fieldData": {
        "fieldPrice": 100,
        "fieldDesc": "普通长剑"
      }
    }
  ],
  "query": {"total": 1, "pages": 1, "pageNum": 1, "pageSize": 15}
}
```

> 图鉴类/文档类返回 `fieldName`、`fieldCode` 固定列 + `fieldData`（动态列 Map）；关联项返回 `fieldData`（含动态列 + 关联列值）；文档类额外返回 `fieldContext`。

### API-W202 数据明细新建

| 项 | 值 |
|----|-----|
| API ID | API-W202 |
| 路径 | `/api/v1/admin/wiki/data-item/save` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：

```json
{
  "data": {
    "fieldDataId": "数据项id",
    "fieldName": "长剑",
    "fieldCode": "sword001",
    "fieldData": {
      "fieldPrice": 100,
      "fieldDesc": "普通长剑"
    }
  }
}
```

> 关联项不传 `fieldName`/`fieldCode`（无固定列）；文档类额外传 `fieldContext`（富文本内容）。

**响应报文**：`data` 为新建记录 id（动态表 `field_id`）。

### API-W203 数据明细加载

| 项 | 值 |
|----|-----|
| API ID | API-W203 |
| 路径 | `/api/v1/admin/wiki/data-item/load` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldId`（明细记录 id）、`fieldDataId`（数据项 id，用于鉴权与元数据加载）

### API-W204 数据明细更新

| 项 | 值 |
|----|-----|
| API ID | API-W204 |
| 路径 | `/api/v1/admin/wiki/data-item/update` |
| 方法 | PATCH |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：`data` 含 `fieldId`、`fieldDataId` 及可更新字段。

### API-W205 数据明细删除

| 项 | 值 |
|----|-----|
| API ID | API-W205 |
| 路径 | `/api/v1/admin/wiki/data-item/delete` |
| 方法 | DELETE |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldId`（明细记录 id）、`fieldDataId`（数据项 id）

### API-W206 数据明细批量删除（关联项）

| 项 | 值 |
|----|-----|
| API ID | API-W206 |
| 路径 | `/api/v1/admin/wiki/data-item/batch-delete` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：

```json
{
  "data": {
    "fieldDataId": "数据项id",
    "fieldIds": ["记录id1", "记录id2"]
  }
}
```

> 仅关联项支持批量删除。

### API-W207 导入模板下载

| 项 | 值 |
|----|-----|
| API ID | API-W207 |
| 路径 | `/api/v1/admin/wiki/data-item/template` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldDataId`（数据项 id）

**响应**：xlsx 文件流（首行标题行，显示所有数据明细，关联数据显示 `${关联数据}编号`，不列附件类明细）。

### API-W208 数据明细导入

| 项 | 值 |
|----|-----|
| API ID | API-W208 |
| 路径 | `/api/v1/admin/wiki/data-item/import` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：

```json
{
  "data": {
    "fieldDataId": "数据项id",
    "fieldAttachmentId": "附件id",
    "skipFail": false,
    "skipError": false
  }
}
```

> `fieldAttachmentId` 为通过附件组件上传 xlsx 后获得的附件信息 id；`skipFail` 为失败数据跳过，`skipError` 为异常数据跳过。

**响应报文**：

```json
{
  "success": true,
  "data": {
    "totalCount": 100,
    "successCount": 95,
    "skipCount": 5,
    "failDetails": [
      {"row": 3, "reason": "关联数据不存在"}
    ]
  }
}
```

### API-W209 数据明细导出

| 项 | 值 |
|----|-----|
| API ID | API-W209 |
| 路径 | `/api/v1/admin/wiki/data-item/export` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：

```json
{
  "data": {
    "fieldDataId": "数据项id"
  }
}
```

**响应**：xlsx 文件流。

---

## 7. wiki 页面维护接口

### API-W301 wiki 页面配置加载

| 项 | 值 |
|----|-----|
| API ID | API-W301 |
| 路径 | `/api/v1/admin/wiki/page/load` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldDataId`（数据项 id）

**响应报文**：

```json
{
  "success": true,
  "data": {
    "fieldId": "配置id",
    "fieldDataId": "数据项id",
    "fieldWikiPage": {
      "displayInfos": [
        {"type": "self", "fieldDataId": "数据项id", "fields": ["fieldName", "fieldCode", "fieldPrice"]}
      ],
      "displayFields": [
        {"fieldDataId": "关联项id", "fields": ["fieldName", "fieldCode"]}
      ]
    }
  }
}
```

### API-W302 wiki 页面配置保存

| 项 | 值 |
|----|-----|
| API ID | API-W302 |
| 路径 | `/api/v1/admin/wiki/page/save` |
| 方法 | POST |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求报文**：

```json
{
  "data": {
    "fieldDataId": "数据项id",
    "fieldWikiPage": {
      "displayInfos": [
        {"type": "self", "fieldDataId": "数据项id", "fields": ["fieldName", "fieldCode", "fieldPrice"]}
      ],
      "displayFields": [
        {"fieldDataId": "关联项id", "fields": ["fieldName", "fieldCode"]}
      ]
    }
  }
}
```

> 一个数据项对应一份 wiki 页面配置（唯一约束 `fieldDataId`）。已存在则更新，不存在则新建。

### API-W303 wiki 页面配置初始化

| 项 | 值 |
|----|-----|
| API ID | API-W303 |
| 路径 | `/api/v1/admin/wiki/page/init` |
| 方法 | GET |
| 鉴权 | `admin-wiki::ADMIN` 或 项目维护人员 |

**请求参数**：`fieldDataId`（数据项 id）

**响应报文**：`data` 含该数据项的数据明细列表（显示信息可选源）和所有包含该数据项的关联类数据项列表（显示字段可选源）。

---

## 8. 文档同步清单

| 变更项 | 需要同步的文档/契约 | 动作 |
|--------|----------------------|------|
| 路径/方法/参数/响应变化 | `openapi.yaml` 或等效契约文件 | 新增/更新 |
| 错误码变化 | 错误码台账 | 新增/更新 |
| 权限变化 | RBAC/权限矩阵 | 新增/更新 |
| 数据结构变化 | [wiki数据库设计.md](wiki数据库设计.md) | 新增/更新 |
| 业务流程变化 | [wiki业务逻辑.md](wiki业务逻辑.md) | 新增/更新 |
