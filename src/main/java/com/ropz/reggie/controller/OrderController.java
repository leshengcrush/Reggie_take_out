package com.ropz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ropz.reggie.common.BaseContext;
import com.ropz.reggie.common.R;
import com.ropz.reggie.dto.OrdersDto;
import com.ropz.reggie.entity.OrderDetail;
import com.ropz.reggie.entity.Orders;
import com.ropz.reggie.service.OrderDetailService;
import com.ropz.reggie.service.OrdersService;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("order")

public class OrderController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;


    /**
     * 提交订单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        ordersService.submit(orders);
        return R.success("下单成功");
    }


    /**
     * 后台订单信息分页查询
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number,
                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime){
        log.info("page: {}, pageSize: {}, number: {}",page,pageSize,number);

        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //构造查询条件
        queryWrapper.like(StringUtils.isNotEmpty(number),Orders::getNumber,number);
        queryWrapper.between(beginTime != null,Orders::getOrderTime,beginTime,endTime);
        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //执行分页查询
        ordersService.page(pageInfo,queryWrapper);


        return R.success(pageInfo);
    }


    /**
     * 修改订单状态更新派送信息
     * @param orders
     * @return
     */
    @PutMapping
    public R updateStatus(@RequestBody Orders orders){
        log.info("更新派送信息");

        ordersService.updateById(orders);

        return R.success(orders);
    }

    /**
     * 用户端订单页面
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize){
        log.info("page: {},pageSize: {}",page,pageSize);

        //构造分页条件
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //构造查询条件
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        //根据下单时间倒序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //执行分页查询
        ordersService.page(pageInfo,queryWrapper);

        //完善分页订单显示
        Page<OrdersDto> dtoPage = new Page<>();

        List<Orders> ordersList = pageInfo.getRecords();
        List<OrdersDto> ordersDtoList = ordersList.stream().map(orders->{
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(orders,ordersDto);
            //设置查询条件
            LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(OrderDetail::getOrderId,orders.getNumber());
            //获取菜品订单集合
            List<OrderDetail> orderDetailList = orderDetailService.list(lambdaQueryWrapper);
            //设置到ordersDto
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());
        //设置结果
        dtoPage.setRecords(ordersDtoList);
        return R.success(dtoPage);
    }
}
