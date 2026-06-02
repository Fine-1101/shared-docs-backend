package org.example.shareddocs.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.shareddocs.dto.response.VersionCompareResponse;
import org.example.shareddocs.dto.response.VersionDiffItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本差异工具类
 * 使用简单的LCS（最长公共子序列）算法计算文本差异
 */
@Slf4j
public class DiffUtils {
    
    /**
     * 计算两个文本的差异
     * 
     * @param textA 版本A的文本
     * @param textB 版本B的文本
     * @return 差异列表
     */
    public static List<VersionDiffItem> computeDiff(String textA, String textB) {
        if (textA == null) textA = "";
        if (textB == null) textB = "";
        
        // 如果文本相同，返回无差异
        if (textA.equals(textB)) {
            List<VersionDiffItem> result = new ArrayList<>();
            result.add(VersionDiffItem.builder()
                    .type("unchanged")
                    .position(0)
                    .length(textA.length())
                    .contentA(textA)
                    .contentB(textB)
                    .build());
            return result;
        }
        
        // 按行分割进行对比
        String[] linesA = textA.split("\n", -1);
        String[] linesB = textB.split("\n", -1);
        
        return computeLineDiff(linesA, linesB);
    }
    
    /**
     * 计算行级别的差异
     */
    private static List<VersionDiffItem> computeLineDiff(String[] linesA, String[] linesB) {
        List<VersionDiffItem> diffs = new ArrayList<>();
        
        int m = linesA.length;
        int n = linesB.length;
        
        // 构建LCS表
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (linesA[i - 1].equals(linesB[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        
        // 回溯找出差异
        int i = m, j = n;
        int position = 0;
        List<VersionDiffItem> tempDiffs = new ArrayList<>();
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && linesA[i - 1].equals(linesB[j - 1])) {
                // 相同的行
                tempDiffs.add(0, VersionDiffItem.builder()
                        .type("unchanged")
                        .position(position)
                        .length(linesA[i - 1].length())
                        .contentA(linesA[i - 1])
                        .contentB(linesB[j - 1])
                        .build());
                position += linesA[i - 1].length() + 1; // +1 for newline
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                // 版本B新增的行
                tempDiffs.add(0, VersionDiffItem.builder()
                        .type("added")
                        .position(position)
                        .length(linesB[j - 1].length())
                        .contentA(null)
                        .contentB(linesB[j - 1])
                        .build());
                position += linesB[j - 1].length() + 1;
                j--;
            } else {
                // 版本A删除的行
                tempDiffs.add(0, VersionDiffItem.builder()
                        .type("removed")
                        .position(position)
                        .length(linesA[i - 1].length())
                        .contentA(linesA[i - 1])
                        .contentB(null)
                        .build());
                position += linesA[i - 1].length() + 1;
                i--;
            }
        }
        
        // 合并连续的同类型差异
        return mergeConsecutiveDiffs(tempDiffs);
    }
    
    /**
     * 合并连续的相同类型差异
     */
    private static List<VersionDiffItem> mergeConsecutiveDiffs(List<VersionDiffItem> diffs) {
        if (diffs.isEmpty()) {
            return diffs;
        }
        
        List<VersionDiffItem> merged = new ArrayList<>();
        VersionDiffItem current = diffs.get(0);
        
        for (int i = 1; i < diffs.size(); i++) {
            VersionDiffItem next = diffs.get(i);
            
            if (current.getType().equals(next.getType())) {
                // 合并相同类型的差异
                current.setLength(current.getLength() + next.getLength() + 1);
                if (current.getContentA() != null && next.getContentA() != null) {
                    current.setContentA(current.getContentA() + "\n" + next.getContentA());
                }
                if (current.getContentB() != null && next.getContentB() != null) {
                    current.setContentB(current.getContentB() + "\n" + next.getContentB());
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    /**
     * 计算差异摘要
     */
    public static VersionCompareResponse.DiffSummary calculateSummary(List<VersionDiffItem> diffs) {
        int addedCount = 0;
        int removedCount = 0;
        int modifiedCount = 0;
        
        for (VersionDiffItem diff : diffs) {
            switch (diff.getType()) {
                case "added":
                    addedCount += diff.getLength();
                    break;
                case "removed":
                    removedCount += diff.getLength();
                    break;
                case "modified":
                    modifiedCount += diff.getLength();
                    break;
            }
        }
        
        return VersionCompareResponse.DiffSummary.builder()
                .addedCount(addedCount)
                .removedCount(removedCount)
                .modifiedCount(modifiedCount)
                .build();
    }
}
