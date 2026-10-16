<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div v-if="isMobileAdmin" class="m-sku-list-wrap">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="glass-card m-sku-block">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="block-title">SKU 列表</h3>
      <!-- [zh] 开始标签 `<p>` -->
      <p class="block-desc">{{ showType === 1 ? '按规格增加或减少可售库存' : '为每个规格组合设置价格与库存；已有 SKU 请在商品列表中更新库存' }}</p>

      <!-- [zh] 开始标签 `<div>` -->
      <div v-if="productEditStore.skuList.length" class="sku-mobile-list">
        <!-- [zh] 开始标签 `<div>` -->
        <div
          v-for="(row, index) in productEditStore.skuList"
          :key="index"
          class="glass-card m-sku-list-card"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-card-head">
            <!-- [zh] 开始标签 `<span>` -->
            <span class="sku-index">SKU #{{ index + 1 }}</span>
            <!-- [zh] 开始标签 `<button>` -->
            <button
              v-if="showType === 0 && productEditStore.skuList.length > 1"
              type="button"
              class="op-btn sm danger"
              @click="removeSku(index)"
            >
              <!-- [zh] 模板内容：`删除` -->
              删除
            <!-- [zh] 闭合标签 `</button>` -->
            </button>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-specs">
            <!-- [zh] 开始标签 `<span>` -->
            <span
              v-for="property in productEditStore.productPropertyList"
              :key="property.propertyId"
              class="sku-spec-pill"
            >
              <!-- [zh] Mustache 插值表达式 -->
              {{ property.propertyName }}:
              <!-- [zh] 开始标签 `<strong>` -->
              <strong>{{ row[property.propertyId]?.propertyValue || '—' }}</strong>
            <!-- [zh] 闭合标签 `</span>` -->
            </span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-field">
            <!-- [zh] 开始标签 `<label>` -->
            <label>售价（元）</label>
            <!-- [zh] 开始标签 `<el-input-number>` -->
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="1" :disabled="showType === 1" style="width: 100%" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="sku-field">
            <!-- [zh] 开始标签 `<label>` -->
            <label>{{ showType === 1 ? '当前库存' : '库存' }}</label>
            <span v-if="showType === 1">{{ row.stock }}</span>
            <!-- [zh] 开始标签 `<el-input-number>` -->
            <el-input-number
              v-else
              v-model="row.stock"
              :min="0"
              :precision="0"
              style="width: 100%"
              :disabled="!!row.productId"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <div v-if="showType === 1" class="sku-field">
            <label>库存增减</label>
            <el-radio-group v-model="row.stockUpdateType" :disabled="row.updating">
              <el-radio-button label="增加" value="1" />
              <el-radio-button label="减少" value="-1" />
            </el-radio-group>
            <el-input-number v-model="row.changeStock" :min="1" :max="2147483647" :precision="0" :disabled="row.updating" placeholder="请输入数量" style="width: 100%" />
            <el-button type="primary" :loading="row.updating" @click="updateSkuStock(row)">确认更新库存</el-button>
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<p>` -->
      <p v-else class="m-empty-tip">请先在上方设置 SKU 属性</p>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>

  <!-- [zh] 开始标签 `<el-table>` -->
  <el-table v-else :data="productEditStore.skuList" border stripe height="100%">
    <!-- [zh] 开始标签 `<el-table-column>` -->
    <el-table-column type="index" width="50" />
    <!-- [zh] 开始标签 `<el-table-column>` -->
    <el-table-column
      v-for="property in productEditStore.productPropertyList"
      :key="property.id"
      :label="property.propertyName"
    >
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ row[property.propertyId]?.propertyValue || '' }}
      </template>
    </el-table-column>

    <el-table-column label="价格" width="180">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number
          v-model="row.price"
          :min="0"
          :precision="2"
          :step="1"
          placeholder="价格"
          :disabled="showType == 1"
        ></el-input-number>
      </template>
    </el-table-column>

    <el-table-column v-if="showType === 1" label="当前库存" :width="100">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] Mustache 插值表达式 -->
        {{ row.stock }}
      </template>
    </el-table-column>

    <el-table-column :label="showType == 0 ? '库存' : '库存增减'" :width="showType == 0 ? 155 : 350">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ row }">
        <!-- [zh] 开始标签 `<el-input-number>` -->
        <el-input-number
          v-model="row.stock"
          :min="0"
          :precision="0"
          placeholder="库存"
          :style="{ width: '130px' }"
          :disabled="!!row.productId"
          v-if="showType === 0"
        ></el-input-number>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="stock-update-panel" v-if="showType === 1">
          <!-- [zh] 开始标签 `<el-radio-group>` -->
          <el-radio-group v-model="row.stockUpdateType" fill="#F00033" :disabled="row.updating">
            <!-- [zh] 开始标签 `<el-radio-button>` -->
            <el-radio-button label="增加" value="1" />
            <!-- [zh] 开始标签 `<el-radio-button>` -->
            <el-radio-button label="减少" value="-1" />
          <!-- [zh] 闭合标签 `</el-radio-group>` -->
          </el-radio-group>
          <!-- [zh] 开始标签 `<el-input>` -->
          <el-input clearable placeholder="请输入数量" v-model="row.changeStock" :disabled="row.updating" class="stock-input"></el-input>
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" :loading="row.updating" @click="updateSkuStock(row)">确定</el-button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="53" v-if="showType == 0">
      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template #default="{ $index }">
        <!-- [zh] 开始标签 `<span>` -->
        <span class="iconfont icon-delete" @click="removeSku($index)"></span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { computed, getCurrentInstance } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute } from 'vue-router'
const route = useRoute()
import { useProductEditStore } from '@/stores/productEditStore'
const productEditStore = useProductEditStore()
import { mitter } from '@/eventbus/eventBus.js'

const props = defineProps({
  showType: {
    type: Number,
    default: 0,
  },
})

const isMobileAdmin = computed(() => route.path.startsWith('/m/'))

const removeSku = (index) => {
  if (productEditStore.skuList.length <= 1) {
    proxy.Message.warning('至少保留一个 SKU')
    return
  }
  const row = productEditStore.skuList[index]
  if (row?.propertyValueIdHash) {
    productEditStore.excludedSkuHashes.add(row.propertyValueIdHash)
  }
  productEditStore.skuList.splice(index, 1)
}

const updateSkuStock = async (row) => {
  if (row.updating) return
  if (!row.stockUpdateType) {
    proxy.Message.warning('请选择库存增减类型（增加/减少）')
    return
  }
  const amount = Number(row.changeStock)
  if (!Number.isInteger(amount) || amount <= 0 || amount > 2147483647) {
    proxy.Message.warning('库存数量必须是大于 0 的整数，且不能超过 2147483647')
    return
  }
  const changeStock = Number(row.stockUpdateType) * amount
  row.updating = true
  try {
    await proxy.ConfirmSensitive({
      message: `确定要${changeStock > 0 ? '增加' : '减少'}库存 ${amount} 吗？`,
      url: proxy.Api.updateSkuStock,
      okfun: async (sensitiveConfirmToken) => {
        const result = await proxy.Request({
          url: proxy.Api.updateSkuStock,
          sensitiveConfirmToken,
          params: {
            productId: row.productId,
            propertyValueIdHash: row.propertyValueIdHash,
            changeStock,
          },
        })
        if (!result) return
        row.stock = Number(row.stock) + changeStock
        proxy.Message.success('库存更新成功')
        row.stockUpdateType = null
        row.changeStock = null
        // 重新读取所有 SKU，避免并发下单或只展示部分 SKU 时总库存失真。
        const latest = await proxy.Request({
          url: proxy.Api.getProductInfo,
          params: { productId: row.productId },
          showLoading: false,
        })
        if (latest) {
          const stocks = new Map(latest.data.skuList.map((sku) => [sku.propertyValueIdHash, sku.stock]))
          for (const sku of productEditStore.skuList) {
            if (stocks.has(sku.propertyValueIdHash)) sku.stock = stocks.get(sku.propertyValueIdHash)
          }
          mitter.emit('updateStockCallback', {
            productId: row.productId,
            totalStock: latest.data.skuList.reduce((sum, sku) => sum + Number(sku.stock), 0),
          })
        }
      },
    })
  } finally {
    row.updating = false
  }
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.icon-delete {` */
.icon-delete {
  /* [zh] 样式规则 `cursor: pointer;` */
  cursor: pointer;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.stock-update-panel {` */
.stock-update-panel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;

  /* [zh] 样式规则 `.stock-input {` */
  .stock-input {
    /* [zh] 样式规则 `margin: 0px 10px;` */
    margin: 0px 10px;
    /* [zh] 样式规则 `width: 120px;` */
    width: 120px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sku-list-wrap {` */
.m-sku-list-wrap {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-mobile-list {` */
.sku-mobile-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.sku-card-head {` */
.sku-card-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;

  /* [zh] 样式规则 `.sku-index {` */
  .sku-index {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
