package com.ropz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ropz.reggie.common.R;
import com.ropz.reggie.dto.DishDto;
import com.ropz.reggie.entity.Category;
import com.ropz.reggie.entity.Dish;
import com.ropz.reggie.entity.DishFlavor;
import com.ropz.reggie.service.CategoryService;
import com.ropz.reggie.service.DishFlavorService;
import com.ropz.reggie.service.DishService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "dishCache" , allEntries = true)
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清理所有菜品缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清楚某个分类的缓存
        //String keys = "dish_" + dishDto.getCategoryId() + "_1";
        //redisTemplate.delete(keys);

        return R.success("新增菜品成功");
    }


    /**
     * 根据分页条件查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();//页面需要categoryName，Dish不满足要求

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");//不拷records,因为它对应的List集合对应的是页面的列表数据

        //自己处理records
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) ->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类Id
            //根据Id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                //获取分类名称
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);

            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);

    }


    /**
     * 根据id查询菜品信息和对应的口味信息,展示在修改页面
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 更新菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "dishCache", allEntries = true)
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理所有菜品缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清楚某个分类的缓存
        //String keys = "dish_" + dishDto.getCategoryId() + "_1";
        //redisTemplate.delete(keys);

        return R.success("更新菜品成功");
    }

    /**
     * 根据id条件查询对应菜品,展示在新建套餐页面
     * @param dish
     * @return

    @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //状态为1（起售）的菜品
        queryWrapper.eq(Dish::getStatus,1);

        //排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }
    */

    /**
     * 根据id条件查询对应菜品,展示在新建套餐页面
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){

        List<DishDto> dishDtoList  = null;
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //1.从Redis中查询数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //2.如过存在，直接返回数据，无需查询数据库
        if (dishDtoList != null){
            return R.success(dishDtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //状态为1（起售）的菜品
        queryWrapper.eq(Dish::getStatus,1);

        //排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList  = list.stream().map((item) ->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类Id
            //根据Id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                //获取分类名称
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);

            }
            //当前菜品Id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL: select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //3.不存在，查询数据库，将缓存数据存入Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 批量/删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "dishCache",allEntries = true)
    public R<String> deleteDish(@RequestParam List<Long>  ids){
        log.info("删除菜品，Id为：{}",ids);

        //DELETE * FROM dish WHERE id in (1,2,3)
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);

        dishService.remove(queryWrapper);

        //清理所有菜品缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //清楚某个分类的缓存
        String keys = "dish_" + ids + "_1";
        redisTemplate.delete(keys);

        return R.success("删除成功");
    }


    /**
     * 批量启售用，启用
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "dishCache",allEntries = true)
    public R<String> updateStatus(@PathVariable Integer status,@RequestParam List<Long> ids){
        log.info("批量启售用，启用");
        dishService.updateDishStatus(status,ids);

        return R.success("操作成功");
    }

}
