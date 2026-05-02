package org.example.shareddocs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.shareddocs.common.exception.BusinessException;
import org.example.shareddocs.dto.request.AddMemberRequest;
import org.example.shareddocs.dto.response.MemberResponse;
import org.example.shareddocs.entity.DocumentMember;
import org.example.shareddocs.entity.User;
import org.example.shareddocs.mapper.DocumentMemberMapper;
import org.example.shareddocs.mapper.UserMapper;
import org.example.shareddocs.service.MemberService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员服务实现类
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<DocumentMemberMapper, DocumentMember> implements MemberService {
    
    private final UserMapper userMapper;
    
    @Override
    public DocumentMember addMember(Long documentId, AddMemberRequest request) {
        // 检查是否已是成员
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, documentId)
               .eq(DocumentMember::getUserId, request.getUserId());
        
        if (count(wrapper) > 0) {
            throw new BusinessException("用户已是文档成员");
        }
        
        // 添加新成员
        DocumentMember member = new DocumentMember();
        member.setDocumentId(documentId);
        member.setUserId(request.getUserId());
        member.setRole(request.getRole());
        member.setJoinedAt(LocalDateTime.now());
        member.setLastAccessAt(LocalDateTime.now());
        
        save(member);
        return member;
    }
    
    @Override
    public void removeMember(Long documentId, Long userId) {
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, documentId)
               .eq(DocumentMember::getUserId, userId);
        
        remove(wrapper);
    }
    
    @Override
    public List<MemberResponse> getDocumentMembers(Long documentId) {
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, documentId);
        
        List<DocumentMember> members = list(wrapper);
        
        return members.stream()
                .map(member -> {
                    User user = userMapper.selectById(member.getUserId());
                    return MemberResponse.builder()
                            .id(member.getId())
                            .userId(member.getUserId())
                            .username(user != null ? user.getUsername() : "未知用户")
                            .nickname(user != null ? user.getNickname() : "未知用户")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .role(member.getRole())
                            .joinedAt(member.getJoinedAt())
                            .lastAccessAt(member.getLastAccessAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateMemberRole(Long documentId, Long userId, String role) {
        LambdaQueryWrapper<DocumentMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentMember::getDocumentId, documentId)
               .eq(DocumentMember::getUserId, userId);
        
        DocumentMember member = getOne(wrapper);
        if (member == null) {
            throw new BusinessException("用户不是文档成员");
        }
        
        member.setRole(role);
        updateById(member);
    }
}
