package com.wiki.admin.sys.attachment.controller;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import com.wiki.admin.sys.attachment.resolver.AdminAttachmentResolver;
import com.wiki.admin.sys.attachment.service.IAdminAttachmentService;
import com.wiki.common.model.request.ApiRequest;
import com.wiki.common.model.response.ApiResponse;
import com.wiki.common.model.response.ListResult;
import com.wiki.common.security.UserContext;
import com.wiki.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 附件 Controller，对应 {@code docs/admin/附件机制.md} 附件列表页面。
 * <p>
 * 路径前缀 {@code /api/v1/admin/attachment}，遵循 {@code AGENTS.md} 4.2。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/attachment")
@RequiredArgsConstructor
public class AdminAttachmentController {

    private final IAdminAttachmentService attachmentService;
    private final AdminAttachmentResolver attachmentResolver;

    /**
     * 附件列表分页查询。
     * <p>
     * 鉴权：要求 admin-attachment::ADMIN 权限（附件管理员可查看附件列表）。
     */
    @PostMapping("/list")
    public ApiResponse<AdminAttachmentListVo> list(@RequestBody ApiRequest<AdminAttachmentMainDo> request) {
        attachmentResolver.requireAdmin();
        ListResult<AdminAttachmentListVo> result = attachmentService.list(request);
        return ApiResponse.success(result.getRecords(), result.getQuery());
    }

    /**
     * 上传附件。
     * <p>
     * 对应前端附件组件：fieldModelName 必填，fieldModelId / fieldKey 可选。
     * 鉴权：仅要求登录态，不做权限过滤（对应 docs/admin/附件机制.md 接口鉴权约定）。
     */
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "fieldModelName", required = false) String fieldModelName,
                                      @RequestParam(value = "fieldModelId", required = false) String fieldModelId,
                                      @RequestParam(value = "fieldKey", required = false) String fieldKey) {
        attachmentResolver.requireLogin(null);
        String uploaderId = currentUserId();
        String mainId = attachmentService.upload(file, fieldModelName, fieldModelId, fieldKey, uploaderId);
        return ApiResponse.success(mainId);
    }

    /**
     * 下载附件。
     * <p>
     * 鉴权：仅要求登录态，不做权限过滤。
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("fieldId") String fieldId) {
        attachmentResolver.requireLogin(fieldId);
        AdminAttachmentFileDo fileDo = attachmentService.loadForDownload(fieldId);
        Path target = Paths.get(fileDo.getFieldFilePath());
        // 若相对路径不含盘符，尝试从当前工作目录解析；实际物理文件由存储目录决定
        Resource resource = new FileSystemResource(target);
        if (!resource.exists()) {
            // 尝试从存储根目录拼接解析
            resource = new FileSystemResource(Paths.get(".", fileDo.getFieldFilePath()));
        }
        String encodedName = URLEncoder.encode(
                StringUtil.isEmpty(fileDo.getFieldFileName()) ? "download" : fileDo.getFieldFileName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 逻辑删除附件。
     * <p>
     * 鉴权：限制为上传者本人或具备 admin-attachment::ADMIN 的用户（对应 docs/admin/附件机制.md 接口鉴权约定）。
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("fieldId") String fieldId) {
        AdminAttachmentMainDo main = attachmentService.loadMain(fieldId);
        attachmentResolver.requireDelete(fieldId, main.getFieldUploaderId());
        attachmentService.delete(fieldId);
        return ApiResponse.success();
    }

    private String currentUserId() {
        UserContext context = UserContext.current();
        return context == null ? null : context.getUserId();
    }
}
