package com.ropz.reggie.dto;

import com.ropz.reggie.entity.User;
import lombok.Data;

@Data
public class UserDto extends User {
    private String code;
}
