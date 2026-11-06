package com.macro.mall.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mapper.CmsStorefrontTextMapper;
import com.macro.mall.model.CmsStorefrontText;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/home/advertise/translations")
@Tag(name = "商城中葡文案管理")
public class CmsStorefrontTextController {
    private final CmsStorefrontTextMapper mapper;

    public CmsStorefrontTextController(CmsStorefrontTextMapper mapper) { this.mapper = mapper; }

    @GetMapping("/list")
    @Operation(summary = "获取商城固定文案、介绍及帮助内容的中葡版本")
    public CommonResult<List<CmsStorefrontText>> list() { return CommonResult.success(mapper.listAll()); }

    @PostMapping("/update/{code}")
    @Operation(summary = "修改商城中文及葡萄牙语文案")
    public CommonResult<Integer> update(@PathVariable String code, @RequestBody CmsStorefrontText content) {
        if (!code.matches("[a-fA-F0-9]{64}") || content.getZh() == null || content.getZh().isBlank()
                || content.getZh().length() > 20000 || (content.getPt() != null && content.getPt().length() > 20000)) {
            return CommonResult.validateFailed("请填写有效中文内容；每种语言最多20000字");
        }
        content.setCode(code);
        int count = mapper.update(content);
        return count > 0 ? CommonResult.success(count) : CommonResult.failed("该文案不存在");
    }
}
