package org.example.shareddocs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.result.Result;
import org.example.shareddocs.dto.request.AddMemberRequest;
import org.example.shareddocs.dto.response.MemberResponse;
import org.example.shareddocs.entity.Document;
import org.example.shareddocs.entity.DocumentMember;
import org.example.shareddocs.mapper.DocumentMapper;
import org.example.shareddocs.service.MemberService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成员管理控制器
 */
@RestController
@RequestMapping("/documents/{docId}/members")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    private final DocumentMapper documentMapper;
    
    /**
     * 根据docId（支持ID或UUID）获取文档ID
     */
    private Long resolveDocumentId(String docId) {
        try {
            // 尝试解析为数字ID
            return Long.parseLong(docId);
        } catch (NumberFormatException e) {
            // 按UUID查询
            LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Document::getDocUuid, docId)
                   .eq(Document::getIsDeleted, 0);
            Document document = documentMapper.selectOne(wrapper);
            if (document == null) {
                throw new RuntimeException("文档不存在");
            }
            return document.getId();
        }
    }
    
    /**
     * 获取成员列表
     */
    @GetMapping
    public Result<List<MemberResponse>> getMembers(@PathVariable String docId) {
        Long documentId = resolveDocumentId(docId);
        List<MemberResponse> members = memberService.getDocumentMembers(documentId);
        return Result.success(members);
    }
    
    /**
     * 添加成员
     */
    @PostMapping
    public Result<DocumentMember> addMember(@PathVariable String docId,
                                            @Valid @RequestBody AddMemberRequest request) {
        Long documentId = resolveDocumentId(docId);
        DocumentMember member = memberService.addMember(documentId, request);
        return Result.success(member);
    }
    
    /**
     * 移除成员
     */
    @DeleteMapping("/{userId}")
    public Result<Void> removeMember(@PathVariable String docId,
                                     @PathVariable Long userId) {
        Long documentId = resolveDocumentId(docId);
        memberService.removeMember(documentId, userId);
        return Result.successVoid("移除成功");
    }
    
    /**
     * 更新成员角色
     */
    @PutMapping("/{userId}")
    public Result<Void> updateMemberRole(@PathVariable String docId,
                                         @PathVariable Long userId,
                                         @RequestBody Map<String, String> request) {
        Long documentId = resolveDocumentId(docId);
        String role = request.get("role");
        memberService.updateMemberRole(documentId, userId, role);
        return Result.successVoid("更新成功");
    }
}
