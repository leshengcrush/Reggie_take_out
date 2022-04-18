package com.ropz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ropz.reggie.dto.DishDto;
import com.ropz.reggie.entity.Dish;


public interface DishService extends IService<Dish> {

    //新增菜品，须同时操作两张表dish、dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新对应的口味信息
    public void updateWithFlavor(DishDto dishDto);

    //删除菜品
    void remove(Long id);
}
