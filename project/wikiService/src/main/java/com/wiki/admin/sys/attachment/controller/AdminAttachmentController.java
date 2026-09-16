package com.wiki.admin.sys.attachment.controller;

import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentFileDo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentListVo;
import com.wiki.admin.sys.attachment.model.dto.AdminAttachmentMainDo;
import com.wiki.admin.sys.attachment.properties.AttachmentProperties;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    private final AttachmentProperties attachmentProperties;

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
        return downloadResource(attachmentService.loadForDownload(fieldId));
    }

    /**
     * 按业务关联下载附件，取关联的第一条（如当前用户头像回显）。
     * <p>
     * 鉴权：仅要求登录态，不做权限过滤（与 /download 一致）；
     * 无关联附件时返回 404，由前端回退默认图标。
     */
    @GetMapping("/downloadByModel")
    public ResponseEntity<Resource> downloadByModel(@RequestParam("fieldModelName") String fieldModelName,
                                                    @RequestParam("fieldModelId") String fieldModelId,
                                                    @RequestParam(value = "fieldKey", required = false) String fieldKey) {
        attachmentResolver.requireLogin(fieldModelId);
        List<AdminAttachmentMainDo> mains = attachmentService.listByModel(fieldModelName, fieldModelId, fieldKey);
        if (mains == null || mains.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return downloadResource(attachmentService.loadForDownload(mains.get(0).getFieldId()));
    }

    /**
     * 将附件文件元数据转为下载响应（download / downloadByModel 共用）。
     */
    private ResponseEntity<Resource> downloadResource(AdminAttachmentFileDo fileDo) {
        Resource resource = new FileSystemResource(resolveFilePath(fileDo.getFieldFilePath()));
        String encodedName = URLEncoder.encode(
                StringUtil.isEmpty(fileDo.getFieldFileName()) ? "download" : fileDo.getFieldFileName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 解析附件物理路径：绝对路径直接使用；相对路径与上传落盘规则一致，
     * 优先按存储根目录（{@code wiki.attachment.storage-root}）拼接，再回退工作目录直连。
     */
    private Path resolveFilePath(String pathStr) {
        Path path = Paths.get(pathStr);
        if (path.isAbsolute()) {
            return path;
        }
        Path underRoot = Paths.get(attachmentProperties.getStorageRoot(), pathStr);
        if (Files.exists(underRoot)) {
            return underRoot;
        }
        // 兼容历史数据：尝试从当前工作目录解析
        return path;
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
