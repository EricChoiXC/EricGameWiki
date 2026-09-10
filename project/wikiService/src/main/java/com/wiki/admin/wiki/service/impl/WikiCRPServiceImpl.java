package com.wiki.admin.wiki.service.impl;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import com.wiki.admin.sys.attachment.service.IAdminAttachmentService;
import com.wiki.admin.sys.org.model.dto.OrgUserDo;
import com.wiki.admin.sys.org.service.IOrgUserService;
import com.wiki.admin.sys.org.util.OrgConstants;
import com.wiki.admin.wiki.service.IWikiCRPService;
import com.wiki.common.exception.BusinessException;
import com.wiki.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * wiki 模块跨模块调用入口 Service 实现，对应 {@code docs/admin/wiki/wiki技术方案.md} 7。
 * <p>
 * 遵循 {@code docs/common/业务流转公共规范.md} 4.2：仅做跨模块调用入口与参数校验，
 * 不承载业务逻辑、不直接访问 Dao。
 * 跨模块调用方向：CRPService → sys.org IOrgUserService / sys.attachment IAdminAttachmentService。
 * 跨模块调用的异常由本实现统一捕获并封装为 BusinessException。
 * 跨模块调用参数禁止直接传递 Do 对象，使用 String 等值类型。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCRPServiceImpl implements IWikiCRPService {

    private final IOrgUserService orgUserService;
    private final IAdminAttachmentService adminAttachmentService;

    @Override
    public boolean checkUserValid(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }
        try {
            OrgUserDo user = orgUserService.load(userId);
            if (user == null) {
                return false;
            }
            String status = user.getFieldStatus();
            if (status == null) {
                status = OrgConstants.STATUS_ENABLED;
            }
            return OrgConstants.STATUS_ENABLED.equals(status);
        } catch (BusinessException e) {
            // 目标模块抛出的业务异常统一捕获并封装
            log.warn("[wiki-CRP] 校验用户有效性异常 userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "校验用户有效性失败", e);
        }
    }

    @Override
    public String loadAttachmentFilePath(String attachmentId) {
        if (attachmentId == null || attachmentId.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "附件ID不能为空");
        }
        try {
            AdminAttachmentFileDo file = adminAttachmentService.loadForDownload(attachmentId);
            if (file == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
            }
            return file.getFieldFilePath();
        } catch (BusinessException e) {
            // 目标模块抛出的业务异常统一捕获并封装
            log.warn("[wiki-CRP] 加载附件文件路径异常 attachmentId={}", attachmentId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加载附件文件路径失败", e);
        }
    }
}
