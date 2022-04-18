package com.ropz.reggie.dto;

import com.ropz.reggie.entity.Setmeal;
import com.ropz.reggie.entity.SetmealDish;
import com.ropz.reggie.entity.Setmeal;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
