# wiki系统

> admin 服务下wiki系统标识为 `admin-wiki`

## 项目表

```mermaid
erDiagram
%%    项目表
    wiki_main {
        field_id varchar(32) "id"
        field_name varchar(200) "名称"
        field_en_name varchar(200) "英文名称"
        field_jp_name varchar(200) "日文名称"
        field_simple_name varchar(15) "简称 NOT NULL" %% 必填且唯一
        field_publish_date timestamp "发布时间"
        field_create_time timestamp "创建时间"
        field_status bit "开启状态"
        field_extend json "扩展信息"
        field_managers json "维护人员"
    }
%%    项目数据项表
    wiki_main_data {
        field_id varchar(32) "id"
        field_main_id varchar(32) "wiki项目id"
        field_name varchar(200) "名称"
        field_data_name varchar(15) "数据项简称"
        field_data_type varchar(10) "数据项类型"
        field_data_json json "数据项"
    }
%%    项目数据项wiki页面配置
    wiki_main_data_wiki_page {
        field_id varchar(32) "id"
        field_main_id varchar(32) "wiki项目id"
        field_data_id varchar(32) "wiki数据项id"
        field_wiki_page blob "wiki页面配置"
    }
```

## 权限

| 权限名 | 中文名 | 描述 |
| --- | --- | --- |
| admin-wiki::ADMIN | wiki管理员 | 可管理所有wiki项目 |

## 页面

### 菜单栏

```text
主菜单
  L wiki
    L 项目列表
```

### 项目列表

筛选区：名称（模糊，or 同时匹配名称、英文名称、日文名称）

排序：创建时间（默认降序）、发布日期

按钮：新建（ADMIN权限）

列表：名称（名称标准文本、英文名称，日文名称辅助文本另起行）、检查、发布时间、创建时间、开启状态、操作（启用/停用（ADMIN权限），编辑（ADMIN权限），维护内容（ADMIN权限或项目的维护人员））

### 项目新建/编辑页面

表单：名称（必填）、英文名称、日文名称、简称（必填、唯一、限小写英文和数字、仅新建页面可编辑）、发行日期、维护人员（整行，AdminOrgUser多选）

明细行：拓展信息；每行拓展信息包含名称-name，类型-type，值-value三项，均必填；类型可选项为：文本-text、数字-number（涵盖浮点和整数）、日期-date、日期时间-datetime、时间-time、是否-boolean；类型影响值的填写方式；

========================

> 以下所有页面和操作均为 ADMIN 权限和 项目的维护人员权限可使用，不做额外权限控制

### 项目数据项列表（项目列表-维护内容 进入页面）

筛选区：无

排序：无，固定 field_id 降序

按钮：新建

列表：名称、简称、数据项类型、操作（编辑，删除，数据维护，wiki页面维护（限 图鉴类、文档类））

### 项目数据项新建/编辑页面

表单：名称（必填）、简称（必填、同项目内唯一、限小写英文和数字、仅新建页面可编辑）、数据项类型（必填；仅新建页面可编辑；可选项：数据项-data、关联项-join、文档-doc）

在表单下方显示说明：

> 图鉴类：如道具、人物、技能等图鉴信息；
> 关联项：如敌人掉落的道具、人物可学技能的图鉴间关联信息，或需要明细表记录的数据信息；
> 文档类：流程攻略等非图鉴型信息；

当类型选择 图鉴类 或 关联项 时，在下方显示 数据项 明细表：

数据项明细表：名称-name、简称-data_name（限英文/数字/字符）、数据类型-type、枚举项-enums、关联项-join；

数据类型可选类型包括：文本-text、富文本-blob、数字-number（涵盖浮点和整数）、日期-date、日期时间-datetime、时间-time、布尔-boolean、枚举-enum、附件-attachment、关联数据-join；

数据项明细表的 名称、简称、数据类型固定显示且必填，第四行随各明细行的数据类型选择项改变而显示枚举项填写或关联项选择；

关联项选择源是该项目已有的 图鉴类 或 文档 数据类型的数据项

图鉴类 的数据项明细表，固定以下两列数据项，不可被修改：

> 名称-name-text; 编号-code-text

#### 项目数据项创建后

在数据库中创建新表 `wiki_${wikiMain.fieldSimpleName}_${wikiMainData.fieldDataName}`

```mermaid
erDiagram
    "wiki_${wikiMain.fieldSimpleName}_${wikiMainData.fieldDataName}" {
        field_id varchar(32) "id"
        field_name varchar(200) "名称"
        field_code varchar(200) "编号"
        field_data json "数据项" %% 图鉴类、关联项
        field_context blob "文档文本" %% 文档类
        "field_${data_name}_id" varchar(32) "关联项id" %% 关联项的关联数据，每个关联数据对应一个字段
    }
```

### 数据项明细列表-图鉴类（项目数据项列表 - 数据维护 进入）

筛选区：名称（模糊查询）、编号（模糊查询）

排序：固定编号降序

操作：新建

列表：名称、编号、操作（编辑、删除）

### 数据项明细列表-关联项（项目数据项列表 - 数据维护 进入）

筛选区：关联数据（按照数据表的所有关联项列出提供模糊筛选、模糊筛选关联数据的field_name或field_code）

排序：固定 field_id 降序

操作：导入、导出、批量删除

列表：多选框、所有数据明细字段（长文本除外）、操作（编辑、删除）

### 数据项明细列表-文档类（项目数据项列表 - 数据维护 进入）

筛选区：名称（模糊查询）、编号（模糊查询）

排序：固定编号降序

操作：新建

列表：名称、编号、操作（编辑、删除）

### 数据项明细新建/编辑-图鉴类

表格：按 数据项明细，逐项显示，每项占一行；

### 数据项明细新建/编辑-关联项

表格：按 数据项明细，逐项显示，每项占一行；不维护名称和编号字段；

### 数据项明细新建/编辑-文档类

表格：标题、编号、内容（副文本，独立占整行）

### 数据项wiki页面维护（项目数据项列表 - wiki页面维护 进入）

明细表：显示信息，显示字段；

显示信息可选项为该数据项的数据明细，和所有包含该数据项的 "关联类" 数据项；

显示字段为选择 "关联类" 数据项时选择，按序选择要显示的 "关联类" 数据项 的 数据明细 项；

## 业务

### 关联数据 - 导入

导入用文件固定使用 xlsx 格式；

打开导入弹窗，提供模板下载和导入文件上传；导入文件上传后可提交；

- 模板下载：文件模板首行为标题行，显示该数据项的所有 数据明细；关联数据显示 `${关联数据}编号`，不列出附件类明细；

- 数据上传：通过附件组件上传附件数据
- 在组件组件和提交按钮中间显示以下设定项（复选框勾选）：失败数据跳过、异常数据跳过；
- 提交后后端按一下步骤进行数据处理：

1. 读取文件，文件读取失败时返回错误；
2. 读取所有数据，并检查数据合理性；数据不合理，且未选择 `异常数据跳过` 时返回错误，选择 `异常数据跳过` 时移除异常项；
3. 分批次进行数据导入，每批次200条；数据导入失败，且未选择 `失败数据跳过` 时回滚并返回错误；选择 `失败数据跳过` 时移除失败项，继续完成导入；
