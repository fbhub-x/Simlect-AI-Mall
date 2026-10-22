package com.simlect.utils;

import com.simlect.entity.po.OrderInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OrderListPayAmountHelper {

    private OrderListPayAmountHelper() {
    }

    /**
     * 按各主单的优惠前金额分摊优惠，以分为单位按最大余数法补齐尾差。
     * 金额相同时使用订单号确定尾差归属，不改变调用方的订单列表顺序。
     */
    public static void distributeCouponDiscount(List<OrderInfo> orderInfoList, BigDecimal discount) {
        if (orderInfoList == null || orderInfoList.isEmpty()
                || discount == null || discount.signum() <= 0) {
            return;
        }
        List<CouponShare> shares = new ArrayList<>();
        BigInteger totalCents = BigInteger.ZERO;
        for (OrderInfo order : orderInfoList) {
            BigDecimal original = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
            BigInteger originalCents = toCents(original.max(BigDecimal.ZERO));
            shares.add(new CouponShare(order, originalCents));
            totalCents = totalCents.add(originalCents);
        }
        if (totalCents.signum() == 0) {
            return;
        }
        BigInteger discountCents = toCents(discount).min(totalCents);
        if (discountCents.signum() == 0) {
            return;
        }
        BigInteger allocatedCents = BigInteger.ZERO;
        for (CouponShare share : shares) {
            BigInteger[] allocation = discountCents.multiply(share.originalCents)
                    .divideAndRemainder(totalCents);
            share.discountCents = allocation[0];
            share.remainder = allocation[1];
            allocatedCents = allocatedCents.add(share.discountCents);
        }
        List<CouponShare> remainderOrder = new ArrayList<>(shares);
        remainderOrder.sort(Comparator.comparing((CouponShare share) -> share.remainder).reversed()
                .thenComparing(share -> share.order.getOrderId(), Comparator.nullsLast(String::compareTo)));
        int remainingCents = discountCents.subtract(allocatedCents).intValueExact();
        for (int i = 0; i < remainingCents; i++) {
            CouponShare share = remainderOrder.get(i);
            share.discountCents = share.discountCents.add(BigInteger.ONE);
        }
        for (CouponShare share : shares) {
            share.order.setAmount(new BigDecimal(share.originalCents.subtract(share.discountCents), 2));
        }
    }

    private static BigInteger toCents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toBigIntegerExact();
    }

    private static final class CouponShare {
        private final OrderInfo order;
        private final BigInteger originalCents;
        private BigInteger discountCents;
        private BigInteger remainder;

        private CouponShare(OrderInfo order, BigInteger originalCents) {
            this.order = order;
            this.originalCents = originalCents;
        }
    }

    public static void ensureOrderListMinTotalPay(List<OrderInfo> orderInfoList) {
        if (orderInfoList == null || orderInfoList.isEmpty()) {
            return;
        }
        BigDecimal minTotal = OrderPayAmountUtil.minOrderPayAmount();
        BigDecimal sum = orderInfoList.stream()
                .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(minTotal) >= 0) {
            return;
        }
        OrderInfo target = orderInfoList.get(orderInfoList.size() - 1);
        BigDecimal current = target.getAmount() == null ? BigDecimal.ZERO : target.getAmount();
        BigDecimal next = current.add(minTotal.subtract(sum)).setScale(2, RoundingMode.HALF_UP);
        target.setAmount(next);
    }
}
