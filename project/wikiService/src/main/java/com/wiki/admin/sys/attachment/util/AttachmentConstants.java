package com.wiki.admin.sys.attachment.util;

/**
 * 附件模块常量，对应 {@code docs/admin/附件机制.md}。
 * <p>
 * 集中维护权限码、配置项 key、删除标识等。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class AttachmentConstants {

    private AttachmentConstants() {
    }

    /** 权限码：附件管理员 */
    public static final String PERMISSION_ADMIN = "admin-attachment::ADMIN";

    /** 配置项：附件路径 */
    public static final String SETTING_FILE_PATH = "admin-attachment::file_path";
    /** 配置项：附件大小限制 */
    public static final String SETTING_FILE_SIZE_LIMIT = "admin-attachment::file_size_limit";

    /** 附件路径默认值 */
    public static final String DEFAULT_FILE_PATH = "/attachment";
    /** 附件大小限制默认值（byte） */
    public static final String DEFAULT_FILE_SIZE_LIMIT = "10485760";

    /** 删除标识：未删除 */
    public static final int DELETE_FLAG_NORMAL = 0;
    /** 删除标识：已删除 */
    public static final int DELETE_FLAG_DELETED = 1;
}
