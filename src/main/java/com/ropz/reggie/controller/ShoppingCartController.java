package com.ropz.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ropz.reggie.common.BaseContext;
import com.ropz.reggie.common.R;
import com.ropz.reggie.entity.ShoppingCart;
import com.ropz.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车
 */
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @RequestMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据:{}",shoppingCart);

        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);



        //判断当前添加的是菜品还是套餐，是否在购物车中
        Long dishId = shoppingCart.getDishId();
        //SQL: select * from shopping_cart where user_id = ? and dish_id/seteal_id = ?
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);//where user_id = ?
        if (dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);

        }else {
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());

        }
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        //查询当前菜品或者套餐是否在购物车中
        if (cartServiceOne != null){
            //如果已经存在，就在原来数量基础上加一
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else {
            //如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        log.info("出查看购物车。。。");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        //SQl: delete from shopping_cart where user_id = ?

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空成功");
    }


    /**
     * 删减购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> subtract(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车删减物品");
        //1.查询当前用户购物车是否存在菜品,将用户的id从threadLocal获取
        shoppingCart.setUserId(BaseContext.getCurrentId());
        //2.根据Id查询购物车
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId());
        //3.判断删除菜品还是套餐，如果菜品不为空
        if (shoppingCart.getDishId() != null){
            //获取菜品id
            queryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        }else {
            //获取套餐id
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        //用户id是唯一，调用getOne得到ShoppingCart对象
        ShoppingCart cart = shoppingCartService.getOne(queryWrapper);
        //4.判断购物车数量
        if (cart.getNumber() == 1){
            //如果购物车数量为1, 则直接删除购物车
            shoppingCartService.remove(queryWrapper);
            cart = shoppingCart;
        }else {
            //购物车数量不为1
            cart.setNumber(cart.getNumber()-1);
            shoppingCartService.updateById(cart);
        }
        return R.success(cart);
    }
}
