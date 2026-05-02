package org.example.shareddocs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.shareddocs.dto.request.AddMemberRequest;
import org.example.shareddocs.dto.response.MemberResponse;
import org.example.shareddocs.entity.DocumentMember;

import java.util.List;

/**
 * 成员服务接口
 */
public interface MemberService extends IService<DocumentMember> {
    
    /**
     * 添加成员
     */
    DocumentMember addMember(Long documentId, AddMemberRequest request);
    
    /**
     * 移除成员
     */
    void removeMember(Long documentId, Long userId);
    
    /**
     * 获取文档成员列表
     */
    List<MemberResponse> getDocumentMembers(Long documentId);
    
    /**
     * 更新成员角色
     */
    void updateMemberRole(Long documentId, Long userId, String role);
}
