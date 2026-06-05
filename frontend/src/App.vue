<template>
  <main class="app">
    <section v-if="!user" class="login-page">
      <form class="login-card" @submit.prevent="login">
        <h1>{{ text.system }}</h1>
        <p>{{ text.position }}</p>
        <label>{{ text.account }}<input ref="loginUserRef" v-model.trim="loginForm.username" autocomplete="username"></label>
        <label>{{ text.password }}<input v-model.trim="loginForm.password" type="password" autocomplete="current-password"></label>
        <button class="primary wide" :disabled="loading.login">{{ loading.login ? text.loggingIn : text.login }}</button>
        <span class="hint">admin / 123456</span>
      </form>
    </section>

    <section v-else class="workspace">
      <header class="top">
        <div>
          <strong>{{ text.workbench }}</strong>
          <span>{{ user.displayName || user.username }} / {{ roleName(user.role) }}</span>
        </div>
        <div class="top-actions">
          <button v-if="view !== 'home'" @click="openHome">{{ text.backWorkbench }}</button>
          <button @click="logout">{{ text.logout }}</button>
        </div>
      </header>

      <section v-if="view === 'home'" class="overview">
        <div v-for="card in overviewCards" :key="card.label" class="metric">
          <span>{{ card.label }}</span>
          <b>{{ card.value }}</b>
        </div>
      </section>

      <section v-if="view === 'home'" class="module-grid">
        <button v-for="module in modules" :key="module.key" :class="{ active: activeModule === module.key }" @click="toggleModule(module.key)">
          <i>{{ module.icon }}</i>
          <span>{{ module.label }}</span>
        </button>
      </section>

      <section v-if="activeModule" class="submodule-panel">
        <button v-for="sub in currentSubmodules" :key="sub.key" :class="{ active: view === sub.key }" @click="openView(sub.key)">
          {{ sub.label }}
        </button>
      </section>

      <section class="content">
        <div v-if="view === 'home'" class="empty-state">{{ text.pickModule }}</div>
        <div v-else-if="placeholderView" class="empty-state">{{ placeholderView }} - {{ text.phaseHint }}</div>

        <section v-if="view === 'inbound-orders'" class="panel">
          <div class="panel-head">
            <h2>{{ labels.inboundOrders }}</h2>
            <div class="head-actions"><button class="primary" @click="openView('inbound-create')">{{ labels.createInbound }}</button><button @click="loadInboundOrders">{{ text.refresh }}</button></div>
          </div>
          <div class="cards">
            <article v-for="o in inboundOrders" :key="o.id" class="location-card">
              <b>{{ o.orderNo }}</b>
              <span>{{ typeName(o.type) }} / {{ orderStatus(o.status) }}</span>
              <em>{{ o.operatorName || '-' }} / {{ o.itemCount || 0 }} SKU</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'outbound-orders'" class="panel">
          <div class="panel-head">
            <h2>{{ labels.outboundOrders }}</h2>
            <div class="head-actions"><button class="primary" @click="openView('outbound-create')">{{ labels.createOutbound }}</button><button @click="loadOutboundOrders">{{ text.refresh }}</button></div>
          </div>
          <div class="cards">
            <article v-for="o in outboundOrders" :key="o.id" class="location-card">
              <b>{{ o.orderNo }}</b>
              <span>{{ typeName(o.type) }} / {{ orderStatus(o.status) }}</span>
              <em>{{ o.operatorName || '-' }} / {{ o.itemCount || 0 }} SKU</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'inbound-create' || view === 'outbound-create'" class="panel order-create-panel">
          <div class="panel-head">
            <h2>{{ isInboundCreate ? labels.createInbound : labels.createOutbound }}</h2>
            <button @click="openView(isInboundCreate ? 'inbound-orders' : 'outbound-orders')">{{ text.back }}</button>
          </div>
          <div class="form-grid order-main-fields">
            <label>{{ labels.orderType }}
              <select v-model="orderForm.type">
                <option v-for="option in currentOrderTypes" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <label>{{ labels.operator }}<input v-model.trim="orderForm.operatorName"></label>
            <label class="remark-field">{{ labels.remark }}<input v-model.trim="orderForm.remark"></label>
          </div>
          <div class="order-lines-head">
            <h3>{{ labels.orderItems }}</h3>
            <button @click="addOrderLine">{{ labels.addItem }}</button>
          </div>
          <div class="order-lines">
            <div v-for="(line, index) in orderForm.items" :key="line.key" class="order-line">
              <label>{{ labels.product }}
                <select v-model.number="line.productId">
                  <option :value="null">{{ text.select }}</option>
                  <option v-for="p in products" :key="p.id" :value="p.id">{{ p.sku }} / {{ p.name }}</option>
                </select>
              </label>
              <label>{{ labels.warehouse }}
                <select v-model.number="line.warehouseId" @change="line.locationId = null">
                  <option :value="null">{{ text.select }}</option>
                  <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
                </select>
              </label>
              <label>{{ labels.locationCode }}
                <select v-model.number="line.locationId">
                  <option :value="null">{{ text.select }}</option>
                  <option v-for="l in locationsForWarehouse(line.warehouseId)" :key="l.id" :value="l.id">{{ l.code }}</option>
                </select>
              </label>
              <label>{{ labels.qty }}<input v-model.number="line.quantity" type="number" min="1"></label>
              <button class="danger" :disabled="orderForm.items.length === 1" @click="removeOrderLine(index)">{{ labels.removeItem }}</button>
            </div>
          </div>
          <div class="actions">
            <button class="primary" :disabled="loading.order" @click="createOrder">{{ loading.order ? text.submitting : labels.submitOrder }}</button>
          </div>
        </section>

        <section v-if="view === 'shelf-create'" class="panel">
          <div class="panel-head"><h2>{{ labels.shelfCreate }}</h2><button @click="openHome">{{ text.back }}</button></div>
          <div class="form-grid">
            <label>{{ labels.warehouse }}
              <select v-model.number="shelfForm.warehouseId">
                <option :value="null">{{ text.select }}</option>
                <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
              </select>
            </label>
            <label>{{ labels.shelfCode }}<input v-model.trim="shelfForm.shelfCode" placeholder="A01"></label>
            <label>{{ labels.shelfName }}<input v-model.trim="shelfForm.shelfName" placeholder="A01"></label>
            <label>X<input v-model.number="shelfForm.xCount" type="number" min="1"></label>
            <label>Y<input v-model.number="shelfForm.yCount" type="number" min="1"></label>
            <label>Z<input v-model.number="shelfForm.zCount" type="number" min="1"></label>
            <label>{{ labels.capacity }}<input v-model.number="shelfForm.capacity" type="number" min="0"></label>
            <label>{{ labels.remark }}<input v-model.trim="shelfForm.remark"></label>
          </div>
          <div class="actions">
            <button @click="previewShelf" :disabled="loading.shelf">{{ labels.previewLocation }}</button>
            <button class="primary" @click="generateShelf" :disabled="loading.shelf">{{ loading.shelf ? text.submitting : labels.generate }}</button>
          </div>
          <div v-if="shelfPreview.length" class="code-list">
            <b>{{ labels.willGenerate }} {{ shelfPreview.length }}</b>
            <span v-for="code in shelfPreview" :key="code">{{ code }}</span>
          </div>
        </section>

        <section v-if="view === 'location-query'" class="panel">
          <div class="panel-head"><h2>{{ labels.locationQuery }}</h2><button @click="loadLocations">{{ text.refresh }}</button></div>
          <div class="filters">
            <select v-model.number="locationFilters.warehouseId" @change="loadLocations">
              <option :value="null">{{ labels.allWarehouse }}</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
            <input v-model.trim="locationFilters.code" :placeholder="labels.locationCode" @keyup.enter="loadLocations">
            <select v-model="locationFilters.status" @change="loadLocations">
              <option value="">{{ labels.allStatus }}</option>
              <option value="ENABLED">{{ labels.available }}</option>
              <option value="FULL">{{ labels.full }}</option>
              <option value="DISABLED">{{ labels.disabled }}</option>
            </select>
          </div>
          <div class="cards">
            <article v-for="l in locations" :key="l.id" class="location-card">
              <b>{{ l.code }}</b>
              <span>{{ l.warehouseName }} / {{ l.shelfCode || '-' }}</span>
              <em>{{ locationStatus(l) }} / {{ l.occupied || 0 }}/{{ l.capacity || 0 }}</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'receiving'" class="panel work-panel">
          <div class="panel-head">
            <h2>{{ inboundOrder ? labels.receiveOrder : labels.receiving }}</h2>
            <div class="head-actions">
              <button v-if="inboundOrder" @click="openReceivingEntry">{{ labels.changeOrder }}</button>
              <button @click="resetReceive">{{ text.clear }}</button>
            </div>
          </div>
          <section v-if="!inboundOrder" class="scan-box">
            <label :class="{ error: errorField === 'orderNo' }">{{ labels.inboundNo }}
              <input ref="orderNoRef" v-model.trim="receiveForm.orderNo" placeholder="IN202606010001" @focus="selectField('orderNo')" @keyup.enter="handleEnter('orderNo')">
            </label>
            <button class="primary wide" :disabled="loading.receive" @click="loadInboundOrder">
              {{ loading.receive ? text.submitting : labels.enterReceiveOrder }}
            </button>
          </section>

          <section v-if="inboundOrder" class="order-info">
            <div><span>{{ labels.inboundNo }}</span><b>{{ inboundOrder.orderNo }}</b></div>
            <div><span>{{ labels.supplier }}</span><b>{{ inboundOrder.supplier }}</b></div>
            <div><span>{{ labels.status }}</span><b>{{ orderStatus(inboundOrder.status) }}</b></div>
            <div><span>{{ labels.progress }}</span><b>{{ inboundOrder.receivedTotal }}/{{ inboundOrder.expectedTotal }} / {{ inboundOrder.progress }}%</b></div>
          </section>

          <section v-if="inboundOrder" class="item-list receive-items">
            <article v-for="item in inboundOrder.items" :key="item.itemId" :class="['receive-card', item.receiveStatus.toLowerCase()]">
              <div><b>{{ item.sku }} {{ item.productName }}</b><span>{{ receiveStatusName(item.receiveStatus) }}</span></div>
              <p>{{ item.modelSpec }} / {{ item.unitName }}</p>
              <footer>{{ labels.expectedQty }} {{ item.expectedQuantity }} / {{ labels.receivedQty }} {{ item.receivedQuantity }} / {{ labels.remainingQty }} {{ item.remainingQuantity }}</footer>
            </article>
          </section>

          <section v-if="inboundOrder" class="scan-box">
            <label :class="{ error: errorField === 'locationCode' }">{{ labels.locationCode }}
              <input ref="locationCodeRef" v-model.trim="receiveForm.locationCode" placeholder="LT-A01-1-1-1" @focus="selectField('locationCode')" @keyup.enter="handleEnter('locationCode')">
            </label>
            <label :class="{ error: errorField === 'productCode' }">{{ labels.productCode }}
              <input ref="productCodeRef" v-model.trim="receiveForm.productCode" placeholder="P001 / 697000000001" @focus="selectField('productCode')" @keyup.enter="handleEnter('productCode')">
            </label>
            <div class="mode-row">
              <button :class="{ active: qtyMode === 'fixed' }" @click="setQtyMode('fixed')">{{ labels.fixedQty }}</button>
              <button :class="{ active: qtyMode === 'custom' }" @click="setQtyMode('custom')">{{ labels.customQty }}</button>
            </div>
            <label :class="{ disabled: qtyMode === 'fixed', error: errorField === 'quantity' }">{{ labels.qty }}
              <input ref="quantityRef" v-model.number="receiveForm.quantity" type="number" min="1" :disabled="qtyMode === 'fixed'" @focus="selectField('quantity')" @keyup.enter="handleEnter('quantity')">
            </label>
            <button ref="confirmRef" class="primary wide" :disabled="loading.receiveSubmit" @click="confirmReceive">
              {{ loading.receiveSubmit ? text.submitting : labels.confirmReceive }}
            </button>
          </section>

          <section v-if="scanResult.location || scanResult.product" class="scan-result">
            <h3>{{ labels.scanResult }}</h3>
            <div v-if="scanResult.location"><span>{{ labels.locationCode }}</span><b>{{ scanResult.location.code }}</b></div>
            <div v-if="scanResult.location"><span>{{ labels.warehouse }}</span><b>{{ scanResult.location.warehouseName }}</b></div>
            <div v-if="scanResult.location"><span>{{ labels.shelfCode }}</span><b>{{ scanResult.location.shelfCode }}</b></div>
            <div v-if="scanResult.location"><span>{{ labels.status }}</span><b>{{ scanResult.location.status }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.productCode }}</span><b>{{ scanResult.product.sku }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.productName }}</span><b>{{ scanResult.product.productName }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.spec }}</span><b>{{ scanResult.product.modelSpec }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.unit }}</span><b>{{ scanResult.product.unitName }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.expectedQty }}</span><b>{{ scanResult.product.expectedQuantity }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.receivedQty }}</span><b>{{ scanResult.product.receivedQuantity }}</b></div>
            <div v-if="scanResult.product"><span>{{ labels.remainingQty }}</span><b>{{ scanResult.product.remainingQuantity }}</b></div>
            <div><span>{{ labels.thisQty }}</span><b>{{ receiveForm.quantity || 0 }}</b></div>
          </section>

        </section>

        <section v-if="view === 'stock-query'" class="panel">
          <div class="panel-head"><h2>{{ labels.stockQuery }}</h2><button @click="loadStocks">{{ text.refresh }}</button></div>
          <div class="cards">
            <article v-for="s in stocks" :key="s.stockId" class="location-card">
              <b>{{ s.sku }} {{ s.productName }}</b>
              <span>{{ s.warehouseName }} / {{ s.locationCode }}</span>
              <em>{{ labels.qty }} {{ s.quantity }} / {{ stockStatusName(s.status) }}</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'stock-flow'" class="panel">
          <div class="panel-head"><h2>{{ labels.stockFlow }}</h2><button @click="loadMovements">{{ text.refresh }}</button></div>
          <div class="cards">
            <article v-for="m in movements" :key="m.id" class="location-card">
              <b>{{ movementName(m.type) }} / {{ m.quantity }}</b>
              <span>{{ m.product?.sku }} {{ m.product?.name }} / {{ m.location?.code }}</span>
              <em>{{ m.beforeQuantity }} -> {{ m.afterQuantity }} / {{ m.sourceNo }}</em>
            </article>
          </div>
        </section>
      </section>
    </section>

    <div v-if="toast.text" :class="['toast', toast.type]">{{ toast.text }}</div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { api } from './api'

const zh = s => s
const text = {
  system: '\u667a\u4ed3\u4e91 WMS',
  position: '\u79fb\u52a8\u4f18\u5148\u4ed3\u50a8\u4f5c\u4e1a\u7cfb\u7edf',
  account: '\u8d26\u53f7',
  password: '\u5bc6\u7801',
  login: '\u767b\u5f55',
  loggingIn: '\u767b\u5f55\u4e2d...',
  workbench: '\u667a\u4ed3\u4e91 WMS \u4f5c\u4e1a\u5de5\u4f5c\u53f0',
  logout: '\u9000\u51fa\u767b\u5f55',
  pickModule: '\u8bf7\u9009\u62e9\u4f5c\u4e1a\u6a21\u5757',
  phaseHint: '\u7b2c\u4e00\u7248\u9884\u7559\u5165\u53e3',
  refresh: '\u5237\u65b0',
  back: '\u8fd4\u56de',
  select: '\u8bf7\u9009\u62e9',
  submitting: '\u63d0\u4ea4\u4e2d...',
  clear: '\u6e05\u7a7a',
  backWorkbench: '\u8fd4\u56de\u5de5\u4f5c\u53f0'
}
const labels = {
  inboundOrders: '\u5165\u5e93\u8ba2\u5355',
  outboundOrders: '\u51fa\u5e93\u8ba2\u5355',
  createInbound: '\u65b0\u5efa\u5165\u5e93\u5355',
  createOutbound: '\u65b0\u5efa\u51fa\u5e93\u5355',
  orderType: '\u8ba2\u5355\u7c7b\u578b',
  operator: '\u64cd\u4f5c\u4eba',
  orderItems: '\u8ba2\u5355\u660e\u7ec6',
  addItem: '\u6dfb\u52a0\u660e\u7ec6',
  removeItem: '\u5220\u9664',
  submitOrder: '\u63d0\u4ea4\u8ba2\u5355',
  product: '\u5546\u54c1',
  shelfCreate: '\u521b\u5efa\u8d27\u67b6',
  warehouse: '\u6240\u5c5e\u4ed3\u5e93',
  shelfCode: '\u8d27\u67b6\u5927\u6807\u8bc6\u53f7',
  shelfName: '\u8d27\u67b6\u540d\u79f0',
  capacity: '\u5355\u4e2a\u8d27\u4f4d\u5bb9\u91cf',
  remark: '\u5907\u6ce8',
  previewLocation: '\u9884\u89c8\u8d27\u4f4d',
  generate: '\u786e\u8ba4\u751f\u6210',
  willGenerate: '\u5373\u5c06\u751f\u6210\u8d27\u4f4d',
  locationQuery: '\u8d27\u4f4d\u67e5\u8be2',
  allWarehouse: '\u5168\u90e8\u4ed3\u5e93',
  allStatus: '\u5168\u90e8\u72b6\u6001',
  available: '\u7a7a\u95f2/\u4f7f\u7528\u4e2d',
  full: '\u5df2\u6ee1',
  disabled: '\u7981\u7528',
  receiving: '\u6536\u8d27\u7ba1\u7406',
  receiveOrder: '\u8ba2\u5355\u6536\u8d27',
  enterReceiveOrder: '\u8fdb\u5165\u6536\u8d27',
  changeOrder: '\u66f4\u6362\u8ba2\u5355',
  inboundNo: '\u5165\u5e93\u5355\u53f7',
  supplier: '\u4f9b\u5e94\u5546',
  status: '\u72b6\u6001',
  progress: '\u8fdb\u5ea6',
  locationCode: '\u8d27\u4f4d\u7801',
  productCode: '\u5546\u54c1\u7f16\u53f7/\u6761\u7801',
  fixedQty: '\u56fa\u5b9a1\u4ef6',
  customQty: '\u81ea\u5b9a\u4e49\u6570\u91cf',
  qty: '\u6570\u91cf',
  confirmReceive: '\u786e\u8ba4\u6536\u8d27',
  scanResult: '\u5f53\u524d\u626b\u63cf\u7ed3\u679c',
  productName: '\u5546\u54c1\u540d\u79f0',
  spec: '\u89c4\u683c\u578b\u53f7',
  unit: '\u5355\u4f4d',
  expectedQty: '\u5e94\u6536\u6570\u91cf',
  receivedQty: '\u5df2\u6536\u6570\u91cf',
  remainingQty: '\u5269\u4f59\u6570\u91cf',
  thisQty: '\u672c\u6b21\u6536\u8d27\u6570\u91cf',
  stockQuery: '\u5e93\u5b58\u67e5\u8be2',
  stockFlow: '\u5e93\u5b58\u6d41\u6c34'
}
const modules = [
  module('inbound', '\u5165\u5e93\u6a21\u5757', '\u5165', [['inbound-orders', labels.inboundOrders], ['inbound-create', labels.createInbound], ['receiving', labels.receiving], ['inbound-records', '\u5165\u5e93\u8bb0\u5f55']]),
  module('outbound', '\u51fa\u5e93\u6a21\u5757', '\u51fa', [['outbound-orders', labels.outboundOrders], ['outbound-create', labels.createOutbound], ['picking', '\u62e3\u8d27\u51fa\u5e93'], ['outbound-records', '\u51fa\u5e93\u8bb0\u5f55']]),
  module('stock', '\u5e93\u5b58\u7ba1\u7406', '\u5b58', [['stock-query', labels.stockQuery], ['stock-flow', labels.stockFlow], ['stock-adjust', '\u5e93\u5b58\u8c03\u6574'], ['stock-distribution', '\u5546\u54c1\u5e93\u5b58\u5206\u5e03']]),
  module('shelf', '\u8d27\u67b6\u7ba1\u7406', '\u67b6', [['shelf-create', labels.shelfCreate], ['shelf-list', '\u8d27\u67b6\u5217\u8868'], ['location-query', labels.locationQuery], ['label-print', '\u8d27\u67b6\u7801\u6253\u5370']]),
  module('product', '\u5546\u54c1\u7ba1\u7406', '\u54c1', [['product-list', '\u5546\u54c1\u5217\u8868'], ['product-create', '\u65b0\u589e\u5546\u54c1'], ['product-category', '\u5546\u54c1\u5206\u7c7b'], ['barcode', '\u6761\u7801\u7ba1\u7406']]),
  module('check', '\u76d8\u70b9\u7ba1\u7406', '\u76d8', [['check-create', '\u65b0\u5efa\u76d8\u70b9'], ['check-task', '\u76d8\u70b9\u4efb\u52a1'], ['pda-check', 'PDA\u626b\u7801\u76d8\u70b9'], ['check-record', '\u76d8\u70b9\u8bb0\u5f55']]),
  module('alert', '\u5e93\u5b58\u9884\u8b66', '\u8b66', [['low-alert', '\u4f4e\u5e93\u5b58\u9884\u8b66'], ['over-alert', '\u79ef\u538b\u9884\u8b66'], ['alert-record', '\u9884\u8b66\u5904\u7406\u8bb0\u5f55']]),
  module('ai', 'AI\u4ed3\u50a8\u52a9\u624b', 'AI', [['ai-chat', '\u667a\u80fd\u95ee\u7b54'], ['replenish', '\u8865\u8d27\u5efa\u8bae'], ['ai-analysis', '\u5f02\u5e38\u5206\u6790'], ['ai-report', '\u4ed3\u50a8\u62a5\u544a']]),
  module('settings', '\u7cfb\u7edf\u8bbe\u7f6e', '\u8bbe', [['users', '\u7528\u6237\u7ba1\u7406'], ['roles', '\u89d2\u8272\u6743\u9650'], ['warehouse-setting', '\u4ed3\u5e93\u8bbe\u7f6e'], ['params', '\u7cfb\u7edf\u53c2\u6570']])
]
function module(key, label, icon, subs) {
  return { key, label: zh(label), icon, subs: subs.map(([key, label]) => ({ key, label: zh(label) })) }
}

const user = ref(JSON.parse(localStorage.getItem('wms-user') || 'null'))
const loginForm = reactive({ username: 'admin', password: '123456' })
const loginUserRef = ref(null)
const activeModule = ref('')
const view = ref('home')
const warehouses = ref([])
const locations = ref([])
const products = ref([])
const stocks = ref([])
const movements = ref([])
const inboundOrders = ref([])
const outboundOrders = ref([])
const dashboard = ref({})
const inboundOrder = ref(null)
const activeReceiveOrderNo = ref('')
const shelfPreview = ref([])
const qtyMode = ref('fixed')
const errorField = ref('')
const lastScan = reactive({ value: '', time: 0 })
const toast = reactive({ text: '', type: 'success' })
const loading = reactive({ login: false, shelf: false, receive: false, receiveSubmit: false, order: false })
const shelfForm = reactive({ warehouseId: null, shelfCode: 'A01', shelfName: 'A01\u8d27\u67b6', xCount: 3, yCount: 4, zCount: 2, capacity: 100, remark: '' })
const locationFilters = reactive({ warehouseId: null, code: '', status: '' })
const receiveForm = reactive({ orderNo: '', locationCode: '', productCode: '', quantity: 1 })
const orderForm = reactive({ type: 'PURCHASE', operatorName: '', remark: '', items: [] })
const scanResult = reactive({ location: null, product: null })
const refs = { orderNo: ref(null), locationCode: ref(null), productCode: ref(null), quantity: ref(null), confirm: ref(null) }
const orderNoRef = refs.orderNo
const locationCodeRef = refs.locationCode
const productCodeRef = refs.productCode
const quantityRef = refs.quantity
const confirmRef = refs.confirm

const currentSubmodules = computed(() => modules.find(m => m.key === activeModule.value)?.subs || [])
const isInboundCreate = computed(() => view.value === 'inbound-create')
const currentOrderTypes = computed(() => isInboundCreate.value
  ? [
      { value: 'PURCHASE', label: '\u91c7\u8d2d\u5165\u5e93' },
      { value: 'RETURN', label: '\u9000\u8d27\u5165\u5e93' },
      { value: 'TRANSFER', label: '\u8c03\u62e8\u5165\u5e93' },
      { value: 'INVENTORY_GAIN', label: '\u76d8\u76c8\u5165\u5e93' }
    ]
  : [
      { value: 'SALE', label: '\u9500\u552e\u51fa\u5e93' },
      { value: 'REQUISITION', label: '\u9886\u7528\u51fa\u5e93' },
      { value: 'TRANSFER', label: '\u8c03\u62e8\u51fa\u5e93' },
      { value: 'DAMAGE', label: '\u62a5\u635f\u51fa\u5e93' },
      { value: 'INVENTORY_LOSS', label: '\u76d8\u4e8f\u51fa\u5e93' }
    ])
const placeholderView = computed(() => {
  if (['home', 'inbound-orders', 'outbound-orders', 'inbound-create', 'outbound-create', 'shelf-create', 'location-query', 'receiving', 'stock-query', 'stock-flow'].includes(view.value)) return ''
  return currentSubmodules.value.find(s => s.key === view.value)?.label || ''
})
const overviewCards = computed(() => [
  { label: '\u4eca\u65e5\u5165\u5e93', value: dashboard.value.todayInbound || 0 },
  { label: '\u4eca\u65e5\u51fa\u5e93', value: dashboard.value.todayOutbound || 0 },
  { label: '\u5e93\u5b58\u9884\u8b66', value: dashboard.value.warningCount || 0 },
  { label: '\u5f85\u6536\u8d27\u8ba2\u5355', value: inboundOrders.value.filter(o => o.status === 'CREATED').length || 1 },
  { label: '\u5f85\u51fa\u5e93\u8ba2\u5355', value: 1 }
])

async function login() {
  loading.login = true
  try {
    user.value = await api.post('/users/login', loginForm)
    localStorage.setItem('wms-user', JSON.stringify(user.value))
    await bootstrap()
    await loadView(view.value)
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.login = false
  }
}
function logout() {
  localStorage.removeItem('wms-user')
  user.value = null
  openHome(true)
  nextTick(() => loginUserRef.value?.focus())
}
async function bootstrap() {
  await Promise.all([loadWarehouses(), loadProducts(), loadAllLocations()])
  dashboard.value = await api.get('/dashboard').catch(() => ({}))
  await Promise.all([loadInboundOrders().catch(() => {}), loadOutboundOrders().catch(() => {})])
}
function toggleModule(key) {
  activeModule.value = activeModule.value === key ? '' : key
}
function moduleForView(key) {
  return modules.find(m => m.subs.some(sub => sub.key === key))
}
function routeForView(key, orderNo = '') {
  const owner = moduleForView(key)
  if (!owner) return '/'
  const suffix = key === 'receiving' && orderNo ? `/${encodeURIComponent(orderNo)}` : ''
  return `/modules/${owner.key}/${key}${suffix}`
}
function viewFromRoute() {
  const parts = window.location.pathname.split('/').filter(Boolean)
  if (parts[0] !== 'modules' || parts.length < 3) return { key: 'home', orderNo: '' }
  const owner = modules.find(m => m.key === parts[1])
  const key = owner?.subs.some(sub => sub.key === parts[2]) ? parts[2] : 'home'
  return { key, orderNo: key === 'receiving' && parts[3] ? decodeURIComponent(parts[3]) : '' }
}
async function loadView(key) {
  if (!user.value) return
  if (key === 'inbound-orders') await loadInboundOrders()
  if (key === 'outbound-orders') await loadOutboundOrders()
  if (key === 'location-query') await loadLocations()
  if (key === 'stock-query') await loadStocks()
  if (key === 'stock-flow') await loadMovements()
  if (key === 'receiving') {
    if (activeReceiveOrderNo.value) await loadReceiveOrder(activeReceiveOrderNo.value)
    else nextTick(() => focusField('orderNo'))
  }
}
async function activateView(key, updateHistory = true, orderNo = '') {
  view.value = key
  activeModule.value = moduleForView(key)?.key || ''
  if (key === 'inbound-create' || key === 'outbound-create') resetOrderForm(key)
  if (key === 'receiving') {
    activeReceiveOrderNo.value = orderNo
    if (!orderNo) clearReceiveState()
  }
  else activeReceiveOrderNo.value = ''
  if (updateHistory) window.history.pushState({}, '', routeForView(key, orderNo))
  await loadView(key)
}
async function openView(key) {
  await activateView(key)
}
async function openReceivingEntry() {
  resetReceive()
  await activateView('receiving')
}
async function openReceiveOrder(orderNo) {
  await activateView('receiving', true, orderNo)
}
function openHome(replace = false) {
  view.value = 'home'
  activeModule.value = ''
  activeReceiveOrderNo.value = ''
  window.history[replace ? 'replaceState' : 'pushState']({}, '', '/')
}
async function syncRoute() {
  const route = viewFromRoute()
  await activateView(route.key, false, route.orderNo)
}
async function loadInboundOrders() { inboundOrders.value = await api.get('/inbound') }
async function loadOutboundOrders() { outboundOrders.value = await api.get('/outbound') }
async function loadWarehouses() { warehouses.value = await api.get('/warehouses') }
async function loadProducts() { products.value = await api.get('/products') }
async function loadAllLocations() { locations.value = await api.get('/locations') }
async function loadStocks() { stocks.value = await api.get('/stocks') }
async function loadMovements() { movements.value = await api.get('/movements') }
function newOrderLine() {
  return { key: `${Date.now()}-${Math.random()}`, productId: null, warehouseId: null, locationId: null, quantity: 1 }
}
function resetOrderForm(targetView = view.value) {
  orderForm.type = targetView === 'inbound-create' ? 'PURCHASE' : 'SALE'
  orderForm.operatorName = user.value?.displayName || user.value?.username || ''
  orderForm.remark = ''
  orderForm.items.splice(0, orderForm.items.length, newOrderLine())
}
function addOrderLine() { orderForm.items.push(newOrderLine()) }
function removeOrderLine(index) { if (orderForm.items.length > 1) orderForm.items.splice(index, 1) }
function locationsForWarehouse(warehouseId) {
  return locations.value.filter(l => l.warehouseId === warehouseId && l.status !== 'DISABLED')
}
async function createOrder() {
  const invalid = orderForm.items.some(line => !line.productId || !line.warehouseId || !line.locationId || !line.quantity || line.quantity < 1)
  if (invalid) return showToast('\u8bf7\u5b8c\u6574\u586b\u5199\u6240\u6709\u8ba2\u5355\u660e\u7ec6', 'error')
  loading.order = true
  try {
    const path = isInboundCreate.value ? '/inbound' : '/outbound'
    const order = await api.post(path, {
      type: orderForm.type,
      operatorName: orderForm.operatorName,
      remark: orderForm.remark,
      items: orderForm.items.map(({ productId, warehouseId, locationId, quantity }) => ({ productId, warehouseId, locationId, quantity: Number(quantity) }))
    })
    showToast(`\u8ba2\u5355 ${order.orderNo} \u521b\u5efa\u6210\u529f`)
    await openView(isInboundCreate.value ? 'inbound-orders' : 'outbound-orders')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.order = false
  }
}
async function previewShelf() {
  loading.shelf = true
  try {
    const data = await api.post('/shelves/preview', shelfForm)
    shelfPreview.value = data.codes
    showToast('\u8d27\u4f4d\u9884\u89c8\u751f\u6210\u6210\u529f')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shelf = false
  }
}
async function generateShelf() {
  loading.shelf = true
  try {
    const data = await api.post('/shelves/generate', shelfForm)
    shelfPreview.value = data.codes
    showToast('\u8d27\u67b6\u548c\u8d27\u4f4d\u751f\u6210\u6210\u529f')
    await openView('location-query')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shelf = false
  }
}
async function loadLocations() {
  const q = new URLSearchParams()
  if (locationFilters.warehouseId) q.set('warehouseId', locationFilters.warehouseId)
  if (locationFilters.code) q.set('code', locationFilters.code)
  if (locationFilters.status) q.set('status', locationFilters.status)
  locations.value = await api.get(`/locations?${q.toString()}`)
}
async function handleEnter(field) {
  if (loading.receive) return
  const value = receiveForm[field]
  if (handleScanInput(value)) return
  if (field === 'orderNo') return loadInboundOrder()
  if (field === 'locationCode') return scanLocation()
  if (field === 'productCode') return scanProduct()
  if (field === 'quantity') return focusField('confirm')
}
function handleScanInput(value) {
  const now = Date.now()
  if (value && lastScan.value === value && now - lastScan.time < 1000) {
    showToast('1\u79d2\u5185\u91cd\u590d\u626b\u63cf\u5df2\u5ffd\u7565', 'error')
    return true
  }
  lastScan.value = value
  lastScan.time = now
  return false
}
async function loadInboundOrder() {
  await runScan('orderNo', async () => {
    const order = await api.get(`/inbound/${receiveForm.orderNo}`)
    inboundOrder.value = order
    activeReceiveOrderNo.value = order.orderNo
    window.history.pushState({}, '', routeForView('receiving', order.orderNo))
    showToast('\u5165\u5e93\u5355\u8bc6\u522b\u6210\u529f')
    focusField('locationCode')
  })
}
async function loadReceiveOrder(orderNo) {
  receiveForm.orderNo = orderNo
  inboundOrder.value = await api.get(`/inbound/${encodeURIComponent(orderNo)}`)
  activeReceiveOrderNo.value = inboundOrder.value.orderNo
  nextTick(() => focusField('locationCode'))
}
async function scanLocation() {
  await runScan('locationCode', async () => {
    scanResult.location = await api.get(`/scan/location/${encodeURIComponent(receiveForm.locationCode)}`)
    showToast('\u8d27\u4f4d\u8bc6\u522b\u6210\u529f')
    focusNext('locationCode')
  })
}
async function scanProduct() {
  await runScan('productCode', async () => {
    scanResult.product = await api.get(`/scan/inbound-product?orderNo=${encodeURIComponent(receiveForm.orderNo)}&code=${encodeURIComponent(receiveForm.productCode)}`)
    receiveForm.quantity = qtyMode.value === 'fixed' ? 1 : null
    showToast('\u5546\u54c1\u8bc6\u522b\u6210\u529f')
    qtyMode.value === 'fixed' ? focusField('confirm') : focusField('quantity')
  })
}
async function confirmReceive() {
  if (loading.receiveSubmit) return
  loading.receiveSubmit = true
  try {
    inboundOrder.value = await api.post('/inbound/receive', {
      orderNo: receiveForm.orderNo,
      locationCode: receiveForm.locationCode,
      productCode: receiveForm.productCode,
      quantity: Number(receiveForm.quantity),
      allowOverReceive: false,
      operatorName: user.value?.displayName || user.value?.username
    })
    showToast('\u6536\u8d27\u6210\u529f')
    receiveForm.productCode = ''
    receiveForm.quantity = qtyMode.value === 'fixed' ? 1 : null
    scanResult.product = null
    await loadStocks().catch(() => {})
    await loadMovements().catch(() => {})
    await clearAndFocus('productCode')
  } catch (e) {
    showToast(e.message, 'error')
    const field = !receiveForm.locationCode ? 'locationCode' : !receiveForm.productCode ? 'productCode' : 'quantity'
    errorField.value = field
    selectField(field)
  } finally {
    loading.receiveSubmit = false
  }
}
async function runScan(field, fn) {
  loading.receive = true
  errorField.value = ''
  try {
    await fn()
  } catch (e) {
    errorField.value = field
    showToast(e.message, 'error')
    selectField(field)
  } finally {
    loading.receive = false
  }
}
function setQtyMode(mode) {
  qtyMode.value = mode
  receiveForm.quantity = mode === 'fixed' ? 1 : null
}
function resetReceive() {
  clearReceiveState()
  if (view.value === 'receiving') window.history.replaceState({}, '', routeForView('receiving'))
  focusField('orderNo')
}
function clearReceiveState() {
  Object.assign(receiveForm, { orderNo: '', locationCode: '', productCode: '', quantity: 1 })
  Object.assign(scanResult, { location: null, product: null })
  inboundOrder.value = null
  activeReceiveOrderNo.value = ''
  qtyMode.value = 'fixed'
}
function focusField(field) { nextTick(() => refs[field]?.value?.focus()) }
function focusNext(field) {
  const order = ['orderNo', 'locationCode', 'productCode', 'quantity', 'confirm']
  focusField(order[order.indexOf(field) + 1])
}
function selectField(field) { nextTick(() => refs[field]?.value?.select?.()) }
async function clearAndFocus(field) { await nextTick(); focusField(field) }
function showToast(text, type = 'success') {
  toast.text = text
  toast.type = type
  window.clearTimeout(showToast.timer)
  showToast.timer = window.setTimeout(() => { toast.text = '' }, 2200)
  if (type === 'success') navigator.vibrate?.(35)
}
function roleName(role) {
  return { SYSTEM_ADMIN: '\u7cfb\u7edf\u7ba1\u7406\u5458', WAREHOUSE_MANAGER: '\u4ed3\u5e93\u7ba1\u7406\u5458', PURCHASING_OPERATIONS: '\u91c7\u8d2d/\u8fd0\u8425\u4eba\u5458' }[role] || role
}
function locationStatus(l) {
  if (l.status === 'DISABLED') return labels.disabled
  if (l.status === 'FULL') return labels.full
  return (l.occupied || 0) > 0 ? '\u4f7f\u7528\u4e2d' : '\u7a7a\u95f2'
}
function orderStatus(status) {
  return { CREATED: '\u5f85\u5904\u7406', COMPLETED: '\u5df2\u5b8c\u6210', CANCELLED: '\u5df2\u53d6\u6d88' }[status] || status
}
function receiveStatusName(status) {
  return { NOT_RECEIVED: '\u672a\u6536\u8d27', PARTIAL: '\u90e8\u5206\u6536\u8d27', DONE: '\u5df2\u5b8c\u6210', OVER: '\u8d85\u51fa' }[status] || status
}
function stockStatusName(status) {
  return { NORMAL: '\u6b63\u5e38', LOW: '\u5e93\u5b58\u4e0d\u8db3', OVERSTOCK: '\u5e93\u5b58\u79ef\u538b' }[status] || status
}
function movementName(type) {
  return { INBOUND: '\u5165\u5e93', OUTBOUND: '\u51fa\u5e93', CHECK_GAIN: '\u76d8\u76c8', CHECK_LOSS: '\u76d8\u4e8f' }[type] || type
}
function typeName(type) {
  return {
    PURCHASE: '\u91c7\u8d2d\u5165\u5e93', RETURN: '\u9000\u8d27\u5165\u5e93', TRANSFER: '\u8c03\u62e8',
    INVENTORY_GAIN: '\u76d8\u76c8\u5165\u5e93', SALE: '\u9500\u552e\u51fa\u5e93', REQUISITION: '\u9886\u7528\u51fa\u5e93',
    DAMAGE: '\u62a5\u635f\u51fa\u5e93', INVENTORY_LOSS: '\u76d8\u4e8f\u51fa\u5e93'
  }[type] || type
}

onMounted(async () => {
  await syncRoute()
  window.addEventListener('popstate', syncRoute)
  if (user.value) {
    await bootstrap()
    await loadView(view.value)
  } else nextTick(() => loginUserRef.value?.focus())
})
onUnmounted(() => window.removeEventListener('popstate', syncRoute))
</script>
