package com.macro.mall.controller;
import com.macro.mall.mapper.SystemFreightMapper;
import com.macro.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
@RestController
@RequestMapping("/system/freight")
public class SystemFreightController {
 private final SystemFreightMapper mapper;
 public SystemFreightController(SystemFreightMapper mapper){this.mapper=mapper;}
 @GetMapping public CommonResult<Map<String,BigDecimal>> read(){return CommonResult.success(mapper.read());}
 @PostMapping public CommonResult<Integer> save(@RequestBody Map<String,BigDecimal> values){
  for(String key:java.util.List.of("cn","usd","eur")) {
   BigDecimal value=values.get(key);
   if(value==null || value.signum()<0 || value.compareTo(new BigDecimal("999999"))>0 || value.scale()>2) return CommonResult.validateFailed("运费应为非负金额，最多两位小数");
  }
  values.putIfAbsent("usdManual",BigDecimal.ZERO);
  values.putIfAbsent("eurManual",BigDecimal.ZERO);
  return CommonResult.success(mapper.save(values));
 }
}
