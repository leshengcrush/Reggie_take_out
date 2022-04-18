package com.ropz.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ropz.reggie.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}
