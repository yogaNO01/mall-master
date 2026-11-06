package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mapper.CmsStorefrontTextMapper;
import com.macro.mall.model.CmsStorefrontText;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class StorefrontTranslationController {
    private final CmsStorefrontTextMapper mapper;

    public StorefrontTranslationController(CmsStorefrontTextMapper mapper) { this.mapper = mapper; }

    @GetMapping("/home/translations")
    public CommonResult<List<CmsStorefrontText>> list() { return CommonResult.success(mapper.listAll()); }
}
