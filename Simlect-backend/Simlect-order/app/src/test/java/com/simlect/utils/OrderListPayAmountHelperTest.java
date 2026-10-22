package com.simlect.utils;

import com.simlect.entity.po.OrderInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多子单最低支付额补差（参考 EShop OrderPayAmountUtilTest 思路）。
 */
class OrderListPayAmountHelperTest {

    @Test
    void couponDiscount_isProportionalToOriginalOrderAmounts() {
        OrderInfo orderA = order("A", "60.00");
        OrderInfo orderB = order("B", "40.00");

        OrderListPayAmountHelper.distributeCouponDiscount(List.of(orderA, orderB), new BigDecimal("20.00"));

        assertEquals(new BigDecimal("48.00"), orderA.getAmount());
        assertEquals(new BigDecimal("32.00"), orderB.getAmount());
    }

    @Test
    void couponDiscount_manySmallOrdersNeverGoNegativeAndPreserveExactTotal() {
        List<OrderInfo> orders = List.of(order("F", "0.01"), order("E", "0.01"),
                order("D", "0.01"), order("C", "0.01"), order("B", "0.01"), order("A", "0.01"));

        OrderListPayAmountHelper.distributeCouponDiscount(orders, new BigDecimal("0.04"));

        assertEquals(new BigDecimal("0.02"), orders.stream().map(OrderInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        for (OrderInfo order : orders) {
            assertTrue(order.getAmount().signum() >= 0);
        }
        assertEquals(new BigDecimal("0.00"), orders.get(5).getAmount());
        assertEquals(new BigDecimal("0.01"), orders.get(0).getAmount());
    }

    @Test
    void couponDiscount_tailCentUsesLargestRemainder() {
        OrderInfo orderA = order("A", "0.02");
        OrderInfo orderB = order("B", "0.03");
        OrderInfo orderC = order("C", "0.05");

        OrderListPayAmountHelper.distributeCouponDiscount(List.of(orderA, orderB, orderC), new BigDecimal("0.01"));

        assertEquals(new BigDecimal("0.02"), orderA.getAmount());
        assertEquals(new BigDecimal("0.03"), orderB.getAmount());
        assertEquals(new BigDecimal("0.04"), orderC.getAmount());
    }

    @Test
    void couponDiscount_equalRemaindersUseOrderIdRegardlessOfInputOrder() {
        OrderInfo firstA = order("A", "0.01");
        OrderInfo firstB = order("B", "0.01");
        OrderInfo firstC = order("C", "0.01");
        List<OrderInfo> first = List.of(firstC, firstA, firstB);
        OrderInfo secondA = order("A", "0.01");
        OrderInfo secondB = order("B", "0.01");
        OrderInfo secondC = order("C", "0.01");
        List<OrderInfo> second = List.of(secondB, secondC, secondA);

        OrderListPayAmountHelper.distributeCouponDiscount(first, new BigDecimal("0.02"));
        OrderListPayAmountHelper.distributeCouponDiscount(second, new BigDecimal("0.02"));

        assertEquals(new BigDecimal("0.00"), firstA.getAmount());
        assertEquals(new BigDecimal("0.00"), firstB.getAmount());
        assertEquals(new BigDecimal("0.01"), firstC.getAmount());
        assertEquals(firstA.getAmount(), secondA.getAmount());
        assertEquals(firstB.getAmount(), secondB.getAmount());
        assertEquals(firstC.getAmount(), secondC.getAmount());
        assertSame(firstC, first.get(0));
        assertSame(firstA, first.get(1));
        assertSame(secondB, second.get(0));
    }

    @Test
    void couponDiscount_missingOrNonPositiveDiscountIsNoop() {
        OrderInfo order = order("A", "60.00");
        List<OrderInfo> orders = List.of(order);

        OrderListPayAmountHelper.distributeCouponDiscount(null, BigDecimal.ONE);
        OrderListPayAmountHelper.distributeCouponDiscount(List.of(), BigDecimal.ONE);
        OrderListPayAmountHelper.distributeCouponDiscount(orders, null);
        OrderListPayAmountHelper.distributeCouponDiscount(orders, BigDecimal.ZERO);
        OrderListPayAmountHelper.distributeCouponDiscount(orders, new BigDecimal("-1.00"));

        assertEquals(new BigDecimal("60.00"), order.getAmount());
    }

    @Test
    void couponDiscount_zeroAndNullAmountsHaveNoShare() {
        OrderInfo zero = order("A", "0.00");
        OrderInfo missing = order("B", null);
        OrderInfo positive = order("C", "10.00");

        OrderListPayAmountHelper.distributeCouponDiscount(List.of(zero, missing), new BigDecimal("2.00"));
        OrderListPayAmountHelper.distributeCouponDiscount(List.of(zero, missing, positive), new BigDecimal("2.00"));

        assertEquals(new BigDecimal("0.00"), zero.getAmount());
        assertEquals(new BigDecimal("0.00"), missing.getAmount());
        assertEquals(new BigDecimal("8.00"), positive.getAmount());
    }

    @Test
    void couponDiscount_exceedingOriginalTotalIsCapped() {
        OrderInfo orderA = order("A", "60.00");
        OrderInfo orderB = order("B", "40.00");

        OrderListPayAmountHelper.distributeCouponDiscount(List.of(orderA, orderB), new BigDecimal("120.00"));

        assertEquals(new BigDecimal("0.00"), orderA.getAmount());
        assertEquals(new BigDecimal("0.00"), orderB.getAmount());
    }

    private OrderInfo order(String orderId, String amount) {
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setAmount(amount == null ? null : new BigDecimal(amount));
        return order;
    }

    @Test
    void emptyOrNullList_noop() {
        OrderListPayAmountHelper.ensureOrderListMinTotalPay(null);
        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of());
    }

    @Test
    void sumAboveMinPay_unchanged() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("60.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("40.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        assertEquals(new BigDecimal("60.00"), o1.getAmount());
        assertEquals(new BigDecimal("40.00"), o2.getAmount());
    }

    @Test
    void sumBelowMinPay_lastOrderGetsDiff() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("0.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        // 最小支付 0.01，总金额 0 → 最后一单补 0.01
        assertEquals(new BigDecimal("0.00"), o1.getAmount());
        assertEquals(new BigDecimal("0.01"), o2.getAmount());
    }

    @Test
    void nullAmountTreatedAsZero() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(null);

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        assertEquals(new BigDecimal("0.01"), o2.getAmount());
    }

    @Test
    void singleOrderBelowMinPay_raisedToMinPay() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.005"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1));

        assertEquals(new BigDecimal("0.01"), o1.getAmount());
    }

    @Test
    void discountDrivenScenario() {
        OrderInfo o1 = new OrderInfo();
        o1.setAmount(new BigDecimal("0.00"));
        OrderInfo o2 = new OrderInfo();
        o2.setAmount(new BigDecimal("5.00"));

        OrderListPayAmountHelper.ensureOrderListMinTotalPay(List.of(o1, o2));

        // 合计 5.00 >= 0.01，不补差
        assertEquals(new BigDecimal("0.00"), o1.getAmount());
        assertEquals(new BigDecimal("5.00"), o2.getAmount());
    }
}
