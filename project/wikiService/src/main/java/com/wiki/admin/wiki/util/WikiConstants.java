package com.wiki.admin.wiki.util;

/**
 * wiki 模块常量，对应 {@code docs/admin/wiki/wiki业务逻辑.md}。
 * <p>
 * 集中维护权限码、数据项类型、状态码值等。
 *
 * @author Eric
 * @date 2026/9/20
 */
public final class WikiConstants {

    private WikiConstants() {
    }

    /** 权限码：wiki 管理员 */
    public static final String PERMISSION_ADMIN = "admin-wiki::ADMIN";

    /** 数据项类型：图鉴类 */
    public static final String DATA_TYPE_DATA = "data";
    /** 数据项类型：关联项 */
    public static final String DATA_TYPE_JOIN = "join";
    /** 数据项类型：文档类 */
    public static final String DATA_TYPE_DOC = "doc";

    /** 页面配置显示信息来源：本数据项明细（displayInfos.type） */
    public static final String PAGE_SOURCE_SELF = "self";

    /** 状态：开启 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;
}
