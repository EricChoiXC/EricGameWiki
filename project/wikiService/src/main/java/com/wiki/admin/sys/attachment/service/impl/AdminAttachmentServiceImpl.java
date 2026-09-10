package com.wiki.admin.sys.attachment.service.impl;

import com.wiki.admin.sys.attachment.dao.IAdminAttachmentFileDao;
import com.wiki.admin.sys.attachment.dao.IAdminAttachmentMainDao;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import com.wiki.admin.sys.attachment.properties.AttachmentProperties;
import com.wiki.admin.sys.attachment.service.IAdminAttachmentService;
import com.wiki.admin.sys.attachment.util.AttachmentConstants;
import com.wiki.admin.sys.attachment.util.AttachmentFieldMaps;
import com.wiki.admin.sys.attachment.util.AttachmentPathUtil;
import com.wiki.admin.sys.common.service.ICommonSettingService;
import com.wiki.common.constant.CommonConstants;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import com.wiki.common.model.query.QueryCondition;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.request.QueryRequest;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.model.response.QueryResponse;
import com.wiki.common.util.IDUtil;
import com.wiki.common.util.QueryConditionBuilder;
import com.wiki.common.util.SqlSortBuilder;
import com.wiki.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 附件 Service 实现，对应 {@code docs/admin/附件机制.md}。
 * <p>
 * 业务逻辑：
 * <ul>
 *   <li>附件关联：通过 field_model_name、field_model_id、field_key 三个字段关联</li>
 *   <li>附件保存路径：${file_path}/${year}/${month}/${fileId}</li>
 *   <li>附件大小限制：从配置项读取，为 0 时不限制</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAttachmentServiceImpl implements IAdminAttachmentService {

    private final IAdminAttachmentMainDao mainDao;
    private final IAdminAttachmentFileDao fileDao;
    private final ICommonSettingService commonSettingService;
    private final AttachmentProperties properties;

    @Override
    public ListResult<AdminAttachmentListVo> list(ApiRequest<AdminAttachmentMainDo> request) {
        QueryRequest query = request.getQuery() == null ? new QueryRequest() : request.getQuery();
        boolean needPage = query.getNeedPage() == null || query.getNeedPage();
        int pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        QueryCondition root = query.getData();
        QueryConditionBuilder.Built built = QueryConditionBuilder.build(root, AttachmentFieldMaps.ATTACHMENT_MAIN);
        String orderBy = SqlSortBuilder.buildOrderBy(
                query.getSortField(), query.getSortOrder(), AttachmentFieldMaps.ATTACHMENT_MAIN);
        if (orderBy == null) {
            orderBy = "ORDER BY m.field_create_time DESC";
        }

        long total = mainDao.countByCondition(built.getWhereSql(), built.getParams());
        long offset = needPage ? (long) (pageNum - 1) * pageSize : 0;
        List<AdminAttachmentListVo> records = mainDao.selectByCondition(
                built.getWhereSql(), orderBy, offset, pageSize, needPage, built.getParams());
        return new ListResult<>(records, QueryResponse.of(total, pageNum, pageSize));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(MultipartFile file, String modelName, String modelId, String key, String uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (StringUtil.isEmpty(modelName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型名称不能为空");
        }

        // 校验文件大小限制
        long sizeLimit = AttachmentPathUtil.getFileSizeLimit(commonSettingService);
        if (sizeLimit > 0 && file.getSize() > sizeLimit) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "文件大小超过限制，最大允许 " + sizeLimit + " byte");
        }

        // 1. 保存文件元数据到 admin_attachment_file
        LocalDateTime now = LocalDateTime.now();
        String fileId = IDUtil.initID();
        String rootPath = AttachmentPathUtil.getFilePath(commonSettingService);
        String relativePath = AttachmentPathUtil.buildRelativePath(rootPath, fileId, now);

        AdminAttachmentFileDo fileDo = new AdminAttachmentFileDo();
        fileDo.setFieldId(fileId);
        fileDo.setFieldFileName(file.getOriginalFilename());
        fileDo.setFieldFileType(extractFileType(file.getOriginalFilename()));
        fileDo.setFieldFileSize(file.getSize());
        fileDo.setFieldFilePath(relativePath);
        fileDao.insert(fileDo);

        // 2. 物理文件落地
        writeToDisk(file, relativePath);

        // 3. 保存关联信息到 admin_attachment_main
        String mainId = IDUtil.initID();
        AdminAttachmentMainDo mainDo = new AdminAttachmentMainDo();
        mainDo.setFieldId(mainId);
        mainDo.setFieldFileId(fileId);
        mainDo.setFieldName(file.getOriginalFilename());
        mainDo.setFieldCreateTime(now);
        mainDo.setFieldUploaderId(uploaderId);
        mainDo.setFieldDeleteFlag(AttachmentConstants.DELETE_FLAG_NORMAL);
        mainDo.setFieldOrder(0);
        mainDo.setFieldModelName(modelName);
        mainDo.setFieldModelId(modelId);
        mainDo.setFieldKey(key);
        mainDao.insert(mainDo);

        log.info("[附件] 上传成功 mainId={} fileId={} fileName={}", mainId, fileId, file.getOriginalFilename());
        return mainId;
    }

    @Override
    public AdminAttachmentFileDo loadForDownload(String fieldId) {
        AdminAttachmentMainDo main = mainDao.selectById(fieldId);
        if (main == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        if (main.getFieldDeleteFlag() != null
                && main.getFieldDeleteFlag() == AttachmentConstants.DELETE_FLAG_DELETED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件已删除");
        }
        AdminAttachmentFileDo fileDo = fileDao.selectById(main.getFieldFileId());
        if (fileDo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件文件不存在");
        }
        return fileDo;
    }

    @Override
    public AdminAttachmentMainDo loadMain(String fieldId) {
        AdminAttachmentMainDo main = mainDao.selectById(fieldId);
        if (main == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        return main;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fieldId) {
        AdminAttachmentMainDo main = mainDao.selectById(fieldId);
        if (main == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        mainDao.updateDeleteFlag(fieldId, AttachmentConstants.DELETE_FLAG_DELETED);
        log.info("[附件] 逻辑删除成功 mainId={}", fieldId);
    }

    @Override
    public List<AdminAttachmentMainDo> listByModel(String modelName, String modelId, String key) {
        return mainDao.selectByModel(modelName, modelId, key);
    }

    @Override
    public void updateOrder(String fieldId, Integer fieldOrder) {
        mainDao.updateOrder(fieldId, fieldOrder);
    }

    /**
     * 物理文件落地到存储目录。
     * <p>
     * 存储目录 = storageRoot + relativePath，relativePath 以 fileId 为文件名。
     */
    private void writeToDisk(MultipartFile file, String relativePath) {
        try {
            Path target = Paths.get(properties.getStorageRoot(), relativePath);
            if (properties.isAutoMkdir()) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("[附件] 文件写入磁盘失败 path={}", relativePath, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败", e);
        }
    }

    /**
     * 提取文件后缀类型（不含点），如 "png"、"pdf"；无后缀时返回空字符串。
     */
    private String extractFileType(String fileName) {
        if (StringUtil.isEmpty(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
