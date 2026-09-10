package com.wiki.admin.sys.attachment.util;

import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.common.util.StringUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 附件存储路径工具，对应 {@code docs/admin/附件机制.md} 附件保存路径约定。
 * <p>
 * 保存路径规则：{@code ${admin-attachment::file_path}/${year}/${month}/${AdminAttachmentFile.fieldId}}
 * <p>
 * 以 {@code AdminAttachmentFile.fieldId} 为文件名保存。
 *
 * @author Eric
 * @date 2026/9/8
 */
public final class AttachmentPathUtil {

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");

    private AttachmentPathUtil() {
    }

    /**
     * 构建附件相对存储路径：{@code ${rootPath}/${year}/${month}/${fileId}}。
     *
     * @param rootPath 附件根路径（从配置项读取）
     * @param fileId   附件文件表 id（作为文件名）
     * @param now      上传时间（用于拆分年/月目录）
     * @return 相对存储路径
     */
    public static String buildRelativePath(String rootPath, String fileId, LocalDateTime now) {
        String root = StringUtil.isEmpty(rootPath) ? AttachmentConstants.DEFAULT_FILE_PATH : rootPath;
        root = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        return root + "/" + now.format(YEAR_FORMAT) + "/" + now.format(MONTH_FORMAT) + "/" + fileId;
    }

    /**
     * 从配置项读取附件大小限制（byte），为 0 时表示不限制。
     *
     * @param commonSettingService 公共配置服务
     * @return 附件大小限制（byte），0 表示不限制
     */
    public static long getFileSizeLimit(ICommonSettingService commonSettingService) {
        String value = commonSettingService.getValueOrDefault(
                AttachmentConstants.SETTING_FILE_SIZE_LIMIT, AttachmentConstants.DEFAULT_FILE_SIZE_LIMIT);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 从配置项读取附件根路径。
     *
     * @param commonSettingService 公共配置服务
     * @return 附件根路径
     */
    public static String getFilePath(ICommonSettingService commonSettingService) {
        return commonSettingService.getValueOrDefault(
                AttachmentConstants.SETTING_FILE_PATH, AttachmentConstants.DEFAULT_FILE_PATH);
    }
}
