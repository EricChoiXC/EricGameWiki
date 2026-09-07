# 配置文件内容规范

## 命名规范

### 路径和文件命名规范

所有配置文件按模块保存在 `/resources/${模块}` 下，无模块的配置文件统一保存在 `/resources/common` 下；

### 配置项命名规范

所有配置项均使用全大写 + 下划线命名，所有配置项值均使用字符串进行管理；

例：

```yaml
FIRST_LEVEL:
    SECOND_LEVEL:
        STRING_VALUE: "string"
        INT_VALUE: "1"
        BOOLEAN_VALUE: "true"
```

## 系统配置项

```yaml
SYSTEM:
    NAME: "wiki-web" # 系统名称
    VERSION: "V1.0.20200101.0" # 系统版本，格式：V{主版本号}.{次版本号}.{日期}.{构建编号}
    AUTHORIZATION: "Copyright © 2020 ${year} wiki. All rights reserved." # 授权信息
    LICENSE: "" # 许可证信息
```

## 通用配置项

### 模块信息配置

> properties.yml
```yaml
MODULE: # 模块信息
    MODEL_NAME: "sys-org" # 模块标识
    MODEL_DESC: "系统组织模块" # 模块名
    VERSION: "V1.0.20200101.0" # 模块版本，格式：V{主版本号}.{次版本号}.{日期}.{构建编号}
```

### 权限配置标准

> roles.yml
```yaml
ROLES: # 权限信息
  - KEY: "SYS_ORG_ADMIN" # 权限标识
    NAME: "系统组织管理员" # 权限名称
    DESC: "拥有所有 `SYS_ORG_*` 权限" # 权限描述
  # ... ...
```

### 码值配置标准

> dict.yml
```yaml
DICT:
  - KEY: "common.yesno" # 码值标识
    NAME: "是/否" # 码值名称
    DESC: "通用的 `是/否` 码值" # 码值描述
    VALUES: # 码值项
      - KEY: "Y"
        NAME: "是"
      - KEY: "N"
        NAME: "否"
  # ... ...
```

### 流程内容配置标准

>process.yml
```yaml
PROCESS:
  - KEY: "WIKI_PROJECT_APPROVE" # 流程类型标识
    NAME: "WIKI项目审批" # 流程类型名称
    MODEL: "com.eric.wiki.wiki.model.dto.WikiProjectDTO" # 绑定的文档模型
    EXPAND_STEP: # 拓展步骤
      - KEY: "WRITE_OPINION" # 步骤标识
        NAME: "填写意见" # 步骤名称
  # ... ...
```
