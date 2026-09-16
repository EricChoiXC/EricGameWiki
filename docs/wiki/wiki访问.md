# wiki 访问

> wiki 前端页面无登录校验，只允许发起 "/wiki/*" 的接口请求；除了特定说明以外，所有 "/wiki/*" 接口只允许读取操作；禁止使用 "/wiki/*" 接口进行修改操作。

## 页面

> `/home` : 首页
> `/wiki/${wikiMain.fieldSimpleName}` : 项目首页
> `/wiki/${wikiMain.fieldSimpleName}/${wikiMainData.fieldDataName}` : 数据项列表页
> `/wiki/${wikiMain.fieldSimpleName}/${wikiMainData.fieldDataName}/${fieldId}` : 数据项详情页

## wiki 页面公共局部

顶部公共布局固定占 80px 高；底部公共布局固定占 80px 高；中间为页面，无标签卡；

顶部公共布局：

> 左侧：`/home` 页面显示 `WIKI`；其他页面显示 项目中文名；
> 右侧：显示 "首页" 按钮；点击跳转 `/home` 页面；
> 中间: 项目内页面，显示一个以项目中文名显示的下拉菜单，下拉项为项目数据项，点击跳转对应数据项列表页；

底部公共布局：

> 中间：网站注册信息；