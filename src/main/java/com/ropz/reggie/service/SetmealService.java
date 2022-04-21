package com.ropz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ropz.reggie.dto.SetmealDto;
import com.ropz.reggie.entity.Setmeal;

import java.util.List;


public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    public void removeWithDish(List<Long> ids);


    /**
     * 套餐批量启售、禁售
     * @param status
     * @param ids
     */
    void updateSetmealStatus(Integer status, List<Long> ids);


    /**
     * 根据id查询套餐信息,展示在修改页面
     * @param id
     * @return
     */
    SetmealDto getByIdWithSetmeal(Long id);


    /**
     * 更新修改套餐
     * @param setmealDto
     * @return
     */
    void updateSetmeal(SetmealDto setmealDto);
}
