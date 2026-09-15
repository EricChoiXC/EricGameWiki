package com.wiki.admin.wiki.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 关联数据导入结果 DTO，对应 {@code docs/admin/wiki/wikiAPI接口设计文档.md} API-W208 响应 data。
 * <p>
 * 由 {@code ImportExportProcessor}（TASK-W3-02）在导入流程中产出：
 * <ul>
 *   <li>{@code totalCount}：导入文件的数据总行数（不含标题行与空行）</li>
 *   <li>{@code successCount}：成功导入条数</li>
 *   <li>{@code skipCount}：跳过条数（异常数据跳过 + 失败批次跳过）</li>
 *   <li>{@code failDetails}：失败明细（{@code row} 为 Excel 行号，标题行计为第 1 行；{@code reason} 为失败原因）</li>
 * </ul>
 *
 * @author Eric
 * @date 2026/9/20
 */
@Data
public class ImportResult {

    /** 导入文件的数据总行数 */
    private int totalCount;

    /** 成功导入条数 */
    private int successCount;

    /** 跳过条数（异常数据跳过 + 失败批次跳过） */
    private int skipCount;

    /** 失败明细 */
    private List<FailDetail> failDetails = new ArrayList<>();

    /** 累计数据总行数 +1 */
    public void countTotal() {
        totalCount++;
    }

    /** 累计成功条数 */
    public void countSuccess(int count) {
        successCount += count;
    }

    /** 累计跳过条数 */
    public void countSkip(int count) {
        skipCount += count;
    }

    /** 追加失败明细 */
    public void addFail(Integer row, String reason) {
        FailDetail detail = new FailDetail();
        detail.setRow(row);
        detail.setReason(reason);
        failDetails.add(detail);
    }

    /**
     * 单条失败明细：{@code row} 为 Excel 行号，{@code reason} 为失败原因。
     */
    @Data
    public static class FailDetail {

        /** Excel 行号（标题行计为第 1 行） */
        private Integer row;

        /** 失败原因 */
        private String reason;
    }
}
