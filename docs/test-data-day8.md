- 用 orderId 打开详情时，后端先查什么、后查什么？

  > 先查 userId，后查 orderItemQuery 得到 OrderItemList，返回 orderInfo

- 为什么不是拿到订单号就能访问任意人的订单？

  > 会泄露隐私，所以加一条 String userId = getTokenUserInfo().getUserId();，核对当前登录态用户 id 与被查订单号的用户 id 是否一致

- 同一张主单为什么有明细列表？

  > 因为主单保存订单整体信息，明细保存具体购买项，两者是一对多关系。

- 为什么支付单号可能关联多张主单？

  > ```
  > 因为项目支持拆单、合并支付：一次结算可能按不同 productId 生成多张主单，但用户只需要付款一次，所以这些主单共用一个 payOrderId。
  > ```

- 返回 JSON 有的字段，数据库为什么可能没有同名列？

  > 因为 JSON 来自 Java 对象的序列化，对象里的字段可以来自数据库、业务计算或手动组装