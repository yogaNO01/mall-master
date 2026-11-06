package com.macro.mall.mapper;

import com.macro.mall.model.CmsStorefrontText;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface CmsStorefrontTextMapper {
    @Select("SELECT code, source, zh, pt FROM cms_storefront_text ORDER BY source")
    List<CmsStorefrontText> listAll();

    @Update("UPDATE cms_storefront_text SET zh=#{zh}, pt=#{pt} WHERE code=#{code}")
    int update(CmsStorefrontText content);
}
