package com.macro.mall.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mapper.CmsSubjectMapper;
import com.macro.mall.mapper.CmsPrefrenceAreaMapper;
import com.macro.mall.model.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Routes inherit the existing subject/preference-area resource permissions. */
@RestController
public class CmsContentTranslationController {
    private final CmsSubjectMapper subjects;
    private final CmsPrefrenceAreaMapper areas;
    public CmsContentTranslationController(CmsSubjectMapper subjects, CmsPrefrenceAreaMapper areas) { this.subjects=subjects; this.areas=areas; }

    @GetMapping("/subject/content/list")
    public CommonResult<List<CmsSubject>> subjects() { return CommonResult.success(subjects.selectByExampleWithBLOBs(new CmsSubjectExample())); }

    @PostMapping("/subject/content/update/{id}")
    public CommonResult<Integer> updateSubject(@PathVariable Long id, @RequestBody CmsSubject source) {
        if (source.getTitle()==null || source.getTitle().isBlank()) return CommonResult.validateFailed("请填写专题标题");
        CmsSubject subject=new CmsSubject(); subject.setId(id); subject.setTitle(source.getTitle()); subject.setDescription(source.getDescription()); subject.setContent(source.getContent()); subject.setTranslations(source.getTranslations());
        int count=subjects.updateByPrimaryKeySelective(subject);
        return count>0 ? CommonResult.success(count) : CommonResult.failed("专题不存在");
    }

    @GetMapping("/prefrenceArea/content/list")
    public CommonResult<List<CmsPrefrenceArea>> areas() { return CommonResult.success(areas.selectByExampleWithBLOBs(new CmsPrefrenceAreaExample())); }

    @PostMapping("/prefrenceArea/content/update/{id}")
    public CommonResult<Integer> updateArea(@PathVariable Long id, @RequestBody CmsPrefrenceArea source) {
        if (source.getName()==null || source.getName().isBlank()) return CommonResult.validateFailed("请填写专区名称");
        CmsPrefrenceArea area=new CmsPrefrenceArea(); area.setId(id); area.setName(source.getName()); area.setSubTitle(source.getSubTitle()); area.setTranslations(source.getTranslations());
        int count=areas.updateByPrimaryKeySelective(area);
        return count>0 ? CommonResult.success(count) : CommonResult.failed("专区不存在");
    }
}
