package com.macro.mall.model;

import lombok.Data;

/** Editable bilingual storefront copy. The source key is stable across text edits. */
@Data
public class CmsStorefrontText {
    private String code;
    private String source;
    private String zh;
    private String pt;
}
