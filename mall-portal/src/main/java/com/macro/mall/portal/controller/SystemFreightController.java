package com.macro.mall.portal.controller;
import com.macro.mall.mapper.SystemFreightMapper;
import com.macro.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
@RestController
public class SystemFreightController {
 private final SystemFreightMapper mapper;
 public SystemFreightController(SystemFreightMapper mapper){this.mapper=mapper;}
 @GetMapping("/home/freight") public CommonResult<Map<String,BigDecimal>> read(){return CommonResult.success(mapper.read());}
}
