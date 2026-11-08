package com.macro.mall.mapper;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.Map;
public interface SystemFreightMapper {
 @Select("SELECT cn, usd, eur, usd_manual AS usdManual, eur_manual AS eurManual FROM system_freight WHERE id=1")
 Map<String,BigDecimal> read();
 @Update("UPDATE system_freight SET cn=#{cn},usd=#{usd},eur=#{eur},usd_manual=#{usdManual},eur_manual=#{eurManual},international_cny=#{cn} WHERE id=1")
 int save(Map<String,BigDecimal> values);
}
