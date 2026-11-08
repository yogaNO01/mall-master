package com.macro.mall.portal.service.impl;
import com.macro.mall.portal.domain.CartPromotionItem;
import com.macro.mall.portal.domain.ConfirmOrderResult;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
class FreightCalculationTest {
 @Test void freightIsPerPieceAndAddedToPayment() throws Exception {
  CartPromotionItem item=new CartPromotionItem();item.setQuantity(3);item.setPrice(new BigDecimal("100"));item.setReduceAmount(new BigDecimal("5"));
  var method=OmsPortalOrderServiceImpl.class.getDeclaredMethod("calcCartAmount",List.class);method.setAccessible(true);
  var result=(ConfirmOrderResult.CalcAmount)method.invoke(new OmsPortalOrderServiceImpl(),List.of(item));
  assertEquals(0,new BigDecimal("60").compareTo(result.getFreightAmount()));
  assertEquals(0,new BigDecimal("345").compareTo(result.getPayAmount()));
 }
 @Test void emptyCartHasNoFreight() throws Exception {
  var method=OmsPortalOrderServiceImpl.class.getDeclaredMethod("calcCartAmount",List.class);method.setAccessible(true);
  var result=(ConfirmOrderResult.CalcAmount)method.invoke(new OmsPortalOrderServiceImpl(),List.of());
  assertEquals(0,BigDecimal.ZERO.compareTo(result.getFreightAmount()));
 }
}
