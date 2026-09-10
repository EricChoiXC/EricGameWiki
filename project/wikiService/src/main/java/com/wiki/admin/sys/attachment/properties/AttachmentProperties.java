package com.wiki.admin.sys.attachment.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 附件模块文件存储配置属性。
 * <p>
 * 物理存储根目录通过 {@code wiki.attachment.storage-root} 配置，
 * 默认 {@code ./data/attachment}，仅在文件落地时使用；
 * 逻辑路径由配置项 {@code admin-attachment::file_path} 管理。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Data
@Component
@ConfigurationProperties(prefix = "wiki.attachment")
public class AttachmentProperties {

    /** 物理存储根目录（文件落地根目录） */
    private String storageRoot = "./data/attachment";

    /** 是否自动创建存储目录 */
    private boolean autoMkdir = true;
}
