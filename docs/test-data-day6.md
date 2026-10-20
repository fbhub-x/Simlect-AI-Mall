# 第 6 天学习结果

## 新增学习
### 为什么 HTTP 200 或外层 code=200，仍可能是确认失败？
HTTP 200、外层 code=200，都不代表确认操作执行成功。 
后端明确把操作过于频繁、token 过期或已执行等情况包装成正常响应，
但内层返回 success:false 和失败原因。
### 当前用户来自哪里？actionToken 从哪里取得？
当前用户来自 Depends(require_login) 的依赖注入。
FastAPI 在执行 confirm_action 函数体之前，
会先调用 require_login，优先从请求头的 token 字段获取登录凭证，
其次从 Cookie 的 token 字段获取，
再根据这个凭证查询 Redis 中保存的用户登录信息，
组装成 TokenUserInfo 对象并赋给 user 参数。
因此，user.user_id 是后端校验登录后取得的用户 ID。如果凭证缺失或失效，
就会抛出登录异常，不再进入确认逻辑。

actionToken 来自前端提交的表单字段，由 Form(...) 读取，且必须提供。
它最初由后端在创建待确认操作时生成，以 act_ 开头，与操作信息一起保存到 Redis，
并放入确认卡的 token 字段下发给前端。用户点击确认按钮后，
onConfirm 取出 props.card.token，
通过 agentApi.confirmAction 将它作为表单字段 actionToken 提交给后端。
后端用它查找具体的待确认操作，
再用当前用户的 user.user_id 检查操作归属。登录凭证用于确定“谁在确认”，
actionToken 用于确定“确认哪次操作”。

### 怎样确认这是本人的提案？怎样检查状态与限制重复执行？

确认提案归属时，后端使用前端提交的 actionToken 查询 Redis 中的提案记录，
再将记录里的 userId 与登录校验得到的当前用户 ID 比较。
记录不存在时，提示操作已过期或不存在；用户 ID 不一致时，提示无权操作。
只有记录存在且属于当前用户，才能继续检查状态。


限制重复执行时，confirm 先取得同一个 token 的 Redis 锁，
再校验提案归属和待确认状态，
随后设置执行标记，并通过 CAS 将 Redis 中的状态从待确认原子地改为已确认，
最后才调用业务执行器。 执行成功后删除提案；
业务明确拒绝时尝试回滚为待确认；系统异常时保留已确认状态，
避免操作可能已经完成却被再次执行。
因此，防重复依靠的是锁、执行标记、状态检查和执行前的原子状态变更共同配合。

### REFUND、CONFIRM_RECEIPT 等动作怎样分发？

execute 接收经过归属和状态校验的提案记录，以及用户登录凭证，
先读取提案中的 actionType，再将 paramsJson 解析成业务参数字典。
随后通过条件分支完成动作分发：REFUND 调用退款方法，
CONFIRM_RECEIPT 调用确认收货方法，PRODUCT_REVIEW 调用首次评价方法，
RECOMMENT 调用追评方法。各分支在调用前检查必要参数，
参数缺失或操作类型不受支持时抛出 ValueError。
具体请求由 JavaBridge 发送，它将业务参数作为表单提交到对应的 Java 接口，
并携带用户登录 token。
Java 返回后，桥接方法检查响应中的 status：成功时返回结果文案，业务失败时根据 info 抛出异常。
结果继续向上传递，由待确认操作服务和路由处理状态、组织响应，最终交给前端显示。


### 同一文件的 JavaBridge.refund_order、_post_form请求哪个 Java API？携带的是用户 token 还是内部令牌？

JavaBridge.refund_order 调用 _post_form，向配置的 Java 网关发送 POST /api/order/refundOrder 请求，并通过表单提交 orderItemId。网关将请求转发给订单服务，最终由 OrderController.refundOrder 处理退款。Java 返回响应后，Python 检查业务字段 status，成功时返回退款完成文案，失败时根据 info 抛出异常。
该请求携带的是当前用户的登录 token，由 confirm_action 从请求头、Cookie 或已校验的用户对象中取得，再逐层传给 JavaBridge，最终同时放入 Java 请求的请求头和 Cookie。内部令牌用于其他 /internal/** 调用，而 actionToken 用于定位待确认提案；退款接口使用用户登录凭证继续进行身份校验和业务处理。

### - Java 为什么再次从登录态取得 userId？
Java 退款接口根据用户登录 token 重新取得 userId，并校验订单是否属于当前用户
### - 哪些订单与明细状态允许退款？
只有订单处于已付款待发货、已发货或部分退款状态，且目标明细仍为正常状态时，才允许退款。
### - 退款金额如何取得？
退款金额根据数据库中的实付金额计算：普通明细按商品金额比例分摊，最后一个正常明细退剩余未退金额，避免累计超退。
### - 已退明细为什么不能再次退款？
状态检查、Redis 标记、数据库条件更新和退款单号复用共同限制重复退款。
### - 支付请求与库存回补分别发生在哪里？
支付退款在订单事务内调用，库存回补安排在事务成功提交之后。
### - 订单退款成功、库存回补失败时，代码怎样记录或补偿？
库存回补失败时，代码保留已完成的退款，记录错误并登记补偿任务，后续使用同一个库存操作编号重试，以限制重复回补。

## 遗留事项
仅列真正未完成的事项：