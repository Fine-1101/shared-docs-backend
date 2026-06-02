package org.example.shareddocs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shareddocs.entity.DocumentOperation;

@Mapper
public interface DocumentOperationMapper extends BaseMapper<DocumentOperation> {
}
