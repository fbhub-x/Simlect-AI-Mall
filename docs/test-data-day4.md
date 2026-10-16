# 第四天：测试数据与操作证据

测试日期：2026-10-16（Asia/Shanghai）。

## 1. 记录范围与证据来源

- 学习分支：`study/baseline`。
- 采集时 HEAD：`27d0bb8acf670acc9c8dd58b273792078d4862b1`。当天修复包含未提交的工作区改动，不能把 HEAD 当作全部修复代码的版本。
- 数据库快照时间：`2026-10-16 23:38:06`，数据库版本 `MySQL 8.3.0`。
- 快照使用同一连接中的 `REPEATABLE READ / READ ONLY` 一致性读，没有修改业务数据。
- 证据包括：数据库快照、用户在本次会话提供的日志摘录、代码调用链、自动化测试报告、前端构建日志。
- 未取得本次浏览器 Network 原始请求/响应和第四天页面截图；这些项目明确标为“待补”，不以代码推导代替实测。

原始数据：[database-snapshot.jsonl](evidence/day4/database-snapshot.jsonl)。采集 SQL：[database-snapshot.sql](evidence/day4/database-snapshot.sql)。

## 2. 测试账号、商品和当前库存

账号对应关系按数据库中的测试昵称记录：

| 测试角色 | 昵称 | user_id | 状态 |
|---|---|---|---|
| 用户 A | 学习用户A | `9000000001` | 1，正常 |
| 用户 B | 学习用户B | `9000000002` | 1，正常 |

下表中的 SKU 别名仅用于本记录，真正定位数据时使用商品 ID 和 SKU hash。

| SKU 别名 | 商品 | product_id | property_value_ids | SKU hash | 单价 | 快照时可售库存 |
|---|---|---|---|---|---|---|
| N1 | 学习笔记本 | `214442725974568` | `17921510163420` | `dc105430dcaa2c43d1da7dbc8111b61b` | 19.90 | 30 |
| N2 | 学习笔记本 | `214442725974568` | `1792151065169` | `f0db8a709d553b4a04ce23d0a674095d` | 19.90 | 28 |
| P1 | 中性笔套装 | `376730323956700` | `17921511979430` | `1b368b5112e7bd04808555a1ee0b9a7c` | 9.90 | 27 |

两个商品的 `product_info.status` 均为 1；三个 SKU 均存在独立的 `simlect_stock.sku_stock` 记录。

**上述库存是采集时的实测现值。当天操作开始前的库存截图/快照未留存，不把 30 写成所有 SKU 已实测的初始库存。**

当前仍存在的购物车记录：用户 B，`cart_id=834346936887432`，SKU N2，数量 1，加购单价 19.90，创建/更新时间均为 `2026-10-16 21:44:19`。其他已从购物车移除的记录无法从当前表还原其历史页面状态。

## 3. 当天实际订单与支付结果

快照中共有 7 笔当天创建的订单、7 条订单明细、7 条支付流水。

订单状态：`0=待付款`、`1=已付款待发货`、`4=交易取消`、`5=交易关闭`。
支付流水状态：`0=待支付`、`1=成功`、`2=关闭`、`3=退款`。

| 编号 | 创建时间 | 用户 | SKU/数量 | 金额 | 快照时订单状态 | order_id |
|---|---|---|---|---|---|---|
| O1 | 22:05:49 | A | N1 × 2 | 39.80 | 5，关闭 | `20261016220549073qrqfgrEjlOCcBSm` |
| O2 | 22:26:35 | B | P1 × 1 | 9.90 | 5，关闭 | `20261016222635154fxcThPBPdGHGhs3` |
| O3 | 22:28:31 | A | N2 × 2 | 39.80 | 5，关闭 | `20261016222831204cR2SWvNON8zXhL4` |
| O4 | 22:53:36 | A | N2 × 2 | 39.80 | 1，已付款 | `20261016225335776zySSzUboGTXTxmE` |
| O5 | 22:55:15 | B | P1 × 2 | 19.80 | 1，已付款 | `20261016225515392Kzlbc5nXC9oOWuk` |
| O6 | 22:58:23 | A | P1 × 1 | 9.90 | 1，已付款 | `20261016225822588chiB0iaVTzxMwIp` |
| O7 | 23:11:08 | A | N1 × 1 | 19.90 | 4，取消 | `20261016231108331KxLmztQtnzdegNP` |

- O4/O5/O6 的支付流水均为成功，渠道流水号以 `LOCAL-` 开头，属于本地模拟付款，没有接入真实支付宝扣款。
- 这三笔模拟付款发生在最终“点击确认付款”改动之前，**不能把它们作为新版确认付款按钮已人工验收的证据**。
- O1/O2/O3/O7 的支付流水均为关闭，`pay_time=NULL`。
- 快照时没有待付款订单。这不代表新版流程会自动付款；快照反映的是这些历史订单在采集时的最终状态。
- 订单、支付单、支付流水的完整关联 ID 均保存在 JSONL 的 `orders`、`payments` 数据集中。

### 可追溯的取消订单样本 O7

```text
用户：学习用户A / 9000000001
order_id：20261016231108331KxLmztQtnzdegNP
pay_order_id：616340429480852026144360947403
商品：学习笔记本 N1，1 件，19.90 元

23:11:08 订单创建，库存扣减操作完成
23:11:31 库存回补操作完成
采集时 order_status=4；trade_status=2；pay_time=NULL
```

对应幂等记录：

```text
order-create-deduct:616340429480852026144360947403
order-close-restock:616340429480852026144360947403
```

两条记录的 `affected_rows` 均为 1，表示影响一个 SKU 行；**它不是购买件数**。该订单的购买件数来自 `order_item.buy_count=1`。

## 4. 库存变更证据

当天保存了 11 条批量库存操作记录：7 条下单扣减、4 条关单/取消回补。它们可以通过操作键中的 `pay_order_id` 与订单明细关联。

| SKU | 可追溯的下单扣减件数 | 可追溯的回补件数 | 这些操作的净变化 | 快照现值 |
|---|---|---|---|---|
| N1 | 3 | 3 | 0 | 30 |
| N2 | 4 | 2 | -2 | 28 |
| P1 | 4 | 1 | -3 | 27 |

净变化由当前保存的成功批量操作记录与对应订单明细计算，不是操作前后库存截图的差值；手工补库存等未进入该操作表的调整不包含在内。

实际业务规则：

1. 加入购物车不扣库存。
2. 提交订单成功时扣库存，订单保持待付款。
3. 确认模拟付款改变订单及支付流水状态，不再次扣库存。
4. 取消待付款订单或超时关单会回补库存。
5. 已支付订单不会按未支付超时关单再次回补。

## 5. 浏览器请求到数据库的操作记录

以下方法、参数及调用链已经核对源码；“预期业务 code”来自代码，实际 Network 响应仍待补录。

| 操作 | 浏览器请求 | 主要参数/编码 | Java 入口及业务实现 | 主要业务库/表 | 预期成功 code | 实测 Network |
|---|---|---|---|---|---|---|
| 加入购物车 | `POST /api/productCart/add2Cart` | FormData：`productId`、`propertyValueIds`、`buyCount` | `ProductCartController.add2Cart` → `ProductCartServiceImpl.add2Cart` | 读写 `simlect_cart.product_cart`；按需远程读商品规格/价格 | 200 | 待补 |
| 提交订单 | `POST /api/order/postOrder` | JSON：`payMethod`、`addressId`、`orderFrom`、`orderList`，可选 `userCouponId` | `OrderController.postOrder` → `OrderInfoServiceImpl.postOrder` | 写 `simlect_order.order_info/order_item/order_logistics_info`；变更 `simlect_stock` 库存；创建 `simlect_pay.pay_trade_record`；按来源清购物车/处理券 | 200 | 待补 |
| 获取支付信息 | `POST /api/order/getPayInfo` | FormData：`orderId` | `OrderController.getPayInfo` → `OrderInfoServiceImpl.getPayInfo` | 查订单、获取支付服务响应；最终版本不再由此调用支付成功处理 | 200 | 待补 |
| 确认测试付款 | `POST /api/order/confirmLocalPay` | FormData：`orderId` | `OrderController.confirmLocalPay` → `getPayInfo` 校验归属/模式 → `paySuccess` | 更新订单、物流及支付记录；不重复扣库存 | 200 | 待补 |
| 取消待付款订单 | `POST /api/order/cancelOrder` | FormData：实际业务 `orderId` | `OrderController.cancelOrder` → `OrderInfoServiceImpl.cancelOrder` | 取消订单、关闭支付流水、回补库存，按需释放券 | 200 | 待补 |

加购完整追踪示例：

```text
前端 cartApi.add2Cart
  → http.ts：baseURL=/api，postForm 构造 FormData
  → Vite :6001 代理到 Gateway :8080
  → Gateway：/api/productCart/** → lb://simlect-cart，移除 /api
  → Cart :8084：ProductCartController.add2Cart
  → 根据登录信息补入 userId
  → ProductCartServiceImpl.add2Cart
  → ProductCartMapper / ProductCartMapper.xml
  → simlect_cart.product_cart：SELECT 后 INSERT 或 UPDATE
```

同一用户、商品、规格再次加购时，`buy_count = buy_count + buyCount`，参数是增减量；加购价格按服务端商品数据取得。

对应源码：

- [购物车 Controller](../Simlect-backend/Simlect-cart/app/src/main/java/com/simlect/controller/ProductCartController.java)
- [购物车 Service](../Simlect-backend/Simlect-cart/app/src/main/java/com/simlect/biz/impl/ProductCartServiceImpl.java)
- [购物车 Mapper XML](../Simlect-backend/Simlect-cart/app/src/main/resources/com/simlect/mappers/ProductCartMapper.xml)
- [订单 Controller](../Simlect-backend/Simlect-order/app/src/main/java/com/simlect/controller/OrderController.java)
- [订单 Service](../Simlect-backend/Simlect-order/app/src/main/java/com/simlect/biz/impl/OrderInfoServiceImpl.java)
- [支付页操作逻辑](../Simlect-front/Simlect-web/src/composables/usePaymentPage.ts)

## 6. 当天故障、处理与验证边界

原始关键报错摘录：[runtime-log-excerpts.txt](evidence/day4/runtime-log-excerpts.txt)，来源为用户在本次会话粘贴的日志，不是完整服务日志文件。

| 问题 | 查到的原因 | 处理 | 证据/限制 |
|---|---|---|---|
| 网关启动失败 | 8080 被原有 `GatewayApplication` 占用，PID 1692 | 定位重复进程，使用/停止旧实例后再启动 | 用户原始端口报错及当时进程排查；不是当前进程清单 |
| 初始库存显示 0、首次补库存失败 | 新 SKU 未初始化独立库存；缺少库存行时单纯 UPDATE 更新不到 | 新 SKU 初始化库存；首次增加库存支持创建记录 | 商品/库存单测报告；当前三个 SKU 均有库存记录 |
| 上架操作失败或无提示 | 二次确认接口路径、目标操作路径及异常展示不完整 | 修正密码确认路径、绑定操作路径并显示失败原因 | 代码修复；本记录没有上架成功的原始 Network 截图 |
| 邮箱验证码发送失败 | 阿里云返回 `MissingAccessKeyId` | 增加配置缺失校验及明确错误提示，补充环境变量说明 | 邮件与账号测试通过；真实邮件送达证据未取得 |
| 下单和库存回补失败 | Seata `lock_table.pk` 仅 36 字符，扣减/回补键为 50/54 字符 | 当前库扩到 `pk VARCHAR(128)`、`row_key VARCHAR(512)`；补初始化及升级脚本 | 快照 `seata_lock_schema` 确认为 128/512；升级脚本见下方链接 |
| 本地支付页误报失败 | 占位模式返回空支付表单，被前端当成错误 | 响应标明本地模式，前端展示相应操作 | 支付及前端测试报告 |
| 提交后自动付款 | 中间版本在获取支付信息时触发模拟付款 | 最终改为独立 `confirmLocalPay`，只有点击“确认付款”才执行 | 订单测试 135 项、前端测试 13 项通过；O7 有取消/回补数据，最终手动确认付款的 Network 截图待补 |

Seata 升级脚本：[20_seata_lock_key_capacity.sql](../sql/20_seata_lock_key_capacity.sql)。扩容属于先前已执行的修复；本次证据采集没有再修改表结构。

最终操作流程：**提交订单 → 待支付 → 选择确认付款 / 取消订单 / 稍后支付**。本地模式为 `SIMLECT_PAY_LOCAL_MOCK_ENABLED=true`，生产就绪模式禁止模拟付款。

## 7. 自动化验证归档

| 报告 | 执行时点（当天） | Tests run | Failures / Errors |
|---|---|---|---|
| [OrderInfoServiceImplTest](evidence/day4/reports/com.simlect.biz.impl.OrderInfoServiceImplTest.txt) | 23:05 | 109 | 0 / 0 |
| [OrderControllerTest](evidence/day4/reports/com.simlect.controller.OrderControllerTest.txt) | 23:05 | 26 | 0 / 0 |
| [SkuStockServiceTest](evidence/day4/reports/com.simlect.biz.SkuStockServiceTest.txt) | 20:15 | 29 | 0 / 0 |
| [ProductInfoServiceImplTest](evidence/day4/reports/com.simlect.biz.impl.ProductInfoServiceImplTest.txt) | 20:15 | 23 | 0 / 0 |
| [AliEmailServiceImplTest](evidence/day4/reports/com.simlect.biz.impl.AliEmailServiceImplTest.txt) | 20:52 | 12 | 0 / 0 |
| [AccountControllerTest](evidence/day4/reports/com.simlect.controller.AccountControllerTest.txt) | 20:52 | 19 | 0 / 0 |
| [PayChannel4AliPayTest](evidence/day4/reports/com.simlect.biz.impl.PayChannel4AliPayTest.txt) | 22:32 | 21 | 0 / 0 |
| [PayInternalControllerTest](evidence/day4/reports/com.simlect.controller.internal.PayInternalControllerTest.txt) | 22:32 | 9 | 0 / 0 |

上述 8 份后端报告共 248 项，来自当天不同修改阶段，不表示执行过一次全仓测试。最终待支付/确认/取消改动验证了其中的 135 项订单测试。

- [前端支付操作测试日志](evidence/day4/frontend-payment-tests.txt)：为本次归档重新执行，13 项通过，覆盖打开页面保持待支付、显式确认付款、取消订单、放弃取消、操作互斥及失败反馈。
- [前端构建日志](evidence/day4/frontend-build.txt)：保存最终支付流程改动后的已有成功构建输出；此前同一构建命令已通过 Vue/TypeScript 检查。
- 本机曾因内存不足中断后端验证；单独重跑后成功。归档使用最终成功的测试报告。

## 8. 仍需补充的人工证据

- [ ] Network：用户 A/B 加购请求的 URL、方法、Form Data、响应业务 `code/info`。
- [ ] 页面：提交订单后显示“待支付”，以及“确认付款 / 取消订单 / 稍后支付”按钮。
- [ ] Network：点击“确认付款”产生 `confirmLocalPay` 请求，随后订单查询返回已付款。
- [ ] 页面/Network：取消待付款订单前后的状态，和对应 `cancelOrder` 请求响应。
- [ ] 同一 SKU 操作前后的库存截图或 SQL 查询结果；目前只有现值及成功操作记录。
- [ ] 用户 A/B 的数据隔离操作：登录各自账号，仅看到各自购物车/订单的浏览器证据。

截图建议另存到 `docs/screenshots/day4/`，文件名包含操作、用户、时间。现有 `docs/screenshots/` 图片的时间为 2026-10-11，未作为第四天截图使用。

每条新证据按以下格式追加：

```text
时间：
用户 / user_id：
操作：
Request URL / Method：
参数名及实际值：
HTTP 状态：
响应业务 code / info：
order_id / pay_order_id / product_id / SKU hash：
操作前状态或库存：
操作后状态或库存：
截图或日志相对路径：
```

归档未包含账号密码、邮箱、收货地址、Cookie、Token 或云服务密钥。证据文件来源及 SHA-256 校验值见 [manifest.json](evidence/day4/manifest.json)。
