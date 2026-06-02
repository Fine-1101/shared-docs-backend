package org.example.shareddocs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shareddocs.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
