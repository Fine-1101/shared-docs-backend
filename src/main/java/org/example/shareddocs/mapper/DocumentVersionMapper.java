package org.example.shareddocs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shareddocs.entity.DocumentVersion;

@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
}
