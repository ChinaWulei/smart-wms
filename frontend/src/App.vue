<template>
  <main class="app">
    <section v-if="!user" class="login-page">
      <form class="login-card" @submit.prevent="login">
        <select class="language-select" v-model="locale" @change="switchLanguage">
          <option value="zh">中文</option>
          <option value="en">English</option>
        </select>
        <h1>{{ text.system }}</h1>
        <p>{{ text.position }}</p>
        <label>{{ text.account }}<input ref="loginUserRef" v-model.trim="loginForm.username" autocomplete="username"></label>
        <label>{{ text.password }}<input v-model.trim="loginForm.password" type="password" autocomplete="current-password"></label>
        <button class="primary wide" :disabled="loading.login">{{ loading.login ? text.loggingIn : text.login }}</button>
        <span class="hint">admin / 123456</span>
      </form>
    </section>

    <section v-else-if="!selectedWarehouseId" class="warehouse-select-page">
      <div class="warehouse-select-card">
        <header>
          <div>
            <span>{{ user.displayName || user.username }}</span>
            <h1>{{ labels.selectWarehouse }}</h1>
            <p>{{ labels.selectWarehouseHint }}</p>
          </div>
          <div class="head-actions">
            <select v-model="locale" @change="switchLanguage"><option value="zh">中文</option><option value="en">English</option></select>
            <button @click="logout">{{ text.logout }}</button>
          </div>
        </header>
        <div class="warehouse-choice-grid">
          <button v-for="warehouse in warehouses" :key="warehouse.id" @click="selectWarehouse(warehouse.id)">
            <i>{{ warehouse.code.slice(0, 2) }}</i>
            <b>{{ warehouse.code }}</b>
            <span>{{ labels.enterWarehouse }}</span>
          </button>
        </div>
        <div v-if="!warehouses.length" class="empty-state">{{ labels.noWarehouse }}</div>
      </div>
    </section>

    <section v-else class="workspace">
      <header class="top">
        <div>
          <strong>{{ text.workbench }}</strong>
          <span>{{ user.displayName || user.username }} / {{ roleName(user.role) }} / {{ currentWarehouse?.code }}</span>
        </div>
        <div class="top-actions">
          <label>{{ labels.language }}
            <select v-model="locale" @change="switchLanguage">
              <option value="zh">中文</option>
              <option value="en">English</option>
            </select>
          </label>
          <label class="warehouse-switcher">{{ labels.currentWarehouse }}
            <select v-model.number="selectedWarehouseId" @change="switchWarehouse">
              <option v-for="warehouse in warehouses" :key="warehouse.id" :value="warehouse.id">{{ warehouse.code }}</option>
            </select>
          </label>
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

        <section v-if="view === 'order-search'" class="panel order-search-panel">
          <div class="panel-head">
            <div>
              <h2>{{ labels.orderSearch }}</h2>
              <p class="panel-description">{{ labels.orderSearchHint }}</p>
            </div>
            <button @click="resetOrderSearch">{{ labels.reset }}</button>
          </div>
          <form class="order-search-form" @submit.prevent="searchOrders">
            <label>{{ labels.orderNo }}
              <input v-model.trim="orderSearchFilters.orderNo" :placeholder="labels.orderNoPlaceholder">
            </label>
            <label>{{ labels.direction }}
              <select v-model="orderSearchFilters.direction">
                <option value="">{{ labels.allDirections }}</option>
                <option value="INBOUND">{{ labels.inbound }}</option>
                <option value="OUTBOUND">{{ labels.outbound }}</option>
              </select>
            </label>
            <label>{{ labels.status }}
              <select v-model="orderSearchFilters.status">
                <option value="">{{ labels.allStatus }}</option>
                <option value="IN_QUEUE">{{ orderStatus('IN_QUEUE') }}</option>
                <option value="RECEIVING">{{ orderStatus('RECEIVING') }}</option>
                <option value="RECEIVED">{{ orderStatus('RECEIVED') }}</option>
                <option value="ALLOCATED">{{ orderStatus('ALLOCATED') }}</option>
                <option value="NOT_ENOUGH_INV">{{ orderStatus('NOT_ENOUGH_INV') }}</option>
                <option value="READY_TO_PICK">{{ orderStatus('READY_TO_PICK') }}</option>
                <option value="PICKING">{{ orderStatus('PICKING') }}</option>
                <option value="PICKED">{{ orderStatus('PICKED') }}</option>
                <option value="COMPLETED">{{ orderStatus('COMPLETED') }}</option>
                <option value="CANCELLED">{{ orderStatus('CANCELLED') }}</option>
              </select>
            </label>
            <label>{{ labels.creator }}
              <input v-model.trim="orderSearchFilters.operatorName" :placeholder="labels.creatorPlaceholder">
            </label>
            <label>{{ labels.createdFrom }}
              <input v-model="orderSearchFilters.createdFrom" type="date">
            </label>
            <label>{{ labels.createdTo }}
              <input v-model="orderSearchFilters.createdTo" type="date">
            </label>
            <button class="primary search-submit" :disabled="loading.orderSearch">
              {{ loading.orderSearch ? labels.searching : labels.search }}
            </button>
          </form>

          <div class="result-summary">{{ labels.searchResult }} <b>{{ orderSearchResults.length }}</b> {{ labels.records }}</div>
          <div class="table-wrap">
            <table class="order-table">
              <thead>
                <tr>
                  <th>{{ labels.orderNo }}</th>
                  <th>{{ labels.direction }}</th>
                  <th>{{ labels.orderType }}</th>
                  <th>{{ labels.status }}</th>
                  <th>{{ labels.creator }}</th>
                  <th>{{ labels.itemCount }}</th>
                  <th>{{ labels.createdAt }}</th>
                  <th>{{ labels.remark }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orderSearchResults" :key="`${order.direction}-${order.id}`">
                  <td><button v-if="order.direction === 'INBOUND'" class="link-button" @click="openOrderDetail(order.orderNo)">{{ order.orderNo }}</button><b v-else>{{ order.orderNo }}</b></td>
                  <td><span :class="['direction-tag', order.direction.toLowerCase()]">{{ directionName(order.direction) }}</span></td>
                  <td>{{ typeName(order.type) }}</td>
                  <td><span :class="['status-tag', order.status.toLowerCase()]">{{ order.direction === 'OUTBOUND' ? outboundStatus(order.status) : orderStatus(order.status) }}</span></td>
                  <td>{{ order.operatorName || '-' }}</td>
                  <td>{{ order.itemCount || 0 }}</td>
                  <td>{{ formatTime(order.createdAt) }}</td>
                  <td class="remark-cell">{{ order.remark || '-' }}</td>
                </tr>
                <tr v-if="!orderSearchResults.length">
                  <td colspan="8" class="table-empty">{{ labels.noOrders }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="view === 'inbound-orders'" class="panel">
          <div class="panel-head">
            <h2>{{ labels.inboundOrders }}</h2>
            <div class="head-actions"><button class="primary" @click="openView('inbound-create')">{{ labels.createInbound }}</button><button @click="loadInboundOrders">{{ text.refresh }}</button></div>
          </div>
          <div class="cards">
            <article v-for="o in inboundOrders" :key="o.id" class="location-card clickable" @click="openOrderDetail(o.orderNo)">
              <b>{{ o.orderNo }}</b>
              <span>{{ typeName(o.type) }} / {{ orderStatus(o.status) }}</span>
              <em>{{ o.operatorName || '-' }} / {{ o.itemCount || 0 }} SKU</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'inbound-detail'" class="panel order-detail-panel">
          <div class="panel-head">
            <h2>{{ labels.orderDetail }} {{ inboundOrderDetail?.orderNo || '' }}</h2>
            <div class="head-actions">
              <button v-if="['CREATED', 'IN_QUEUE', 'RECEIVING'].includes(inboundOrderDetail?.status)" class="primary" @click="openReceiveOrder(inboundOrderDetail.orderNo)">{{ labels.receiving }}</button>
              <button v-if="inboundOrderDetail?.status === 'RECEIVED'" class="primary" :disabled="loading.inboundAction" @click="confirmInboundOrder">{{ labels.confirmInbound }}</button>
              <button v-if="['CREATED', 'IN_QUEUE'].includes(inboundOrderDetail?.status)" class="danger" :disabled="loading.inboundAction" @click="cancelInboundOrder">{{ labels.cancelOrder }}</button>
              <button @click="openView('inbound-orders')">{{ text.back }}</button>
            </div>
          </div>
          <div v-if="inboundOrderDetail" class="detail-grid">
            <section class="detail-column">
              <h3>{{ labels.orderBasic }}</h3>
              <div class="detail-kv"><span>{{ labels.inboundNo }}</span><b>{{ inboundOrderDetail.orderNo }}</b></div>
              <div class="detail-kv"><span>{{ labels.orderType }}</span><b>{{ typeName(inboundOrderDetail.type) }}</b></div>
              <div class="detail-kv"><span>{{ labels.status }}</span><b>{{ orderStatus(inboundOrderDetail.status) }}</b></div>
              <div class="detail-kv"><span>{{ labels.operator }}</span><b>{{ inboundOrderDetail.supplier || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.progress }}</span><b>{{ inboundOrderDetail.receivedTotal }}/{{ inboundOrderDetail.expectedTotal }} / {{ inboundOrderDetail.progress }}%</b></div>
              <div class="detail-kv"><span>{{ labels.remark }}</span><b>{{ inboundOrderDetail.remark || '-' }}</b></div>
            </section>
            <section class="detail-column">
              <h3>{{ labels.productInfo }}</h3>
              <article v-for="item in inboundOrderDetail.items" :key="item.itemId" class="detail-item">
                <button class="link-button" @click="openReceiveByTracking(inboundOrderDetail.orderNo, item.trackingNo)">{{ item.trackingNo }}</button>
                <b>{{ item.sku }} {{ item.productName }}</b>
                <span>{{ item.modelSpec }} / {{ item.unitName }}</span>
                <em>{{ item.warehouseName }} / {{ item.locationCode }}</em>
                <footer>{{ labels.expectedQty }} {{ item.expectedQuantity }} / {{ labels.receivedQty }} {{ item.receivedQuantity }} / {{ labels.remainingQty }} {{ item.remainingQuantity }}</footer>
              </article>
            </section>
            <section class="detail-column">
              <h3>{{ labels.orderHistory }}</h3>
              <article v-for="(h, index) in inboundOrderDetail.histories" :key="index" class="history-item">
                <b>{{ historyName(h.operationType) }}</b>
                <span>{{ formatTime(h.operationTime) }}</span>
                <em>{{ h.operatorName || '-' }}</em>
                <p>{{ h.remark }}</p>
              </article>
            </section>
          </div>
        </section>

        <section v-if="view === 'outbound-orders'" class="panel">
          <div class="panel-head">
            <h2>{{ labels.outboundOrders }}</h2>
            <div class="head-actions"><button class="primary" @click="openView('outbound-create')">{{ labels.createOutbound }}</button><button @click="loadOutboundOrders">{{ text.refresh }}</button></div>
          </div>
          <div class="cards">
            <article v-for="o in outboundOrders" :key="o.id" class="location-card clickable" @click="openOutboundDetail(o.id)">
              <b>{{ o.orderNo }}</b>
              <span>{{ typeName(o.type) }} / {{ orderStatus(o.status) }}</span>
              <em>{{ o.operatorName || '-' }} / {{ o.itemCount || 0 }} SKU</em>
            </article>
          </div>
        </section>

        <section v-if="view === 'shipping-jobs'" class="panel shipping-jobs-panel">
          <div class="panel-head">
            <div>
              <h2>{{ labels.shippingJobs }}</h2>
              <p class="panel-description">{{ labels.shippingJobsHint }}</p>
            </div>
            <button @click="loadShippingJobs">{{ text.refresh }}</button>
          </div>
          <form class="shipping-job-form" @submit.prevent="createShippingJob">
            <label>{{ labels.plannedShipDate }}
              <input v-model="shippingJobForm.plannedShipDate" type="date" required>
            </label>
            <label>{{ labels.truckNo }}
              <input v-model.trim="shippingJobForm.truckNo" :placeholder="labels.truckNoPlaceholder">
            </label>
            <label>{{ labels.driverName }}
              <input v-model.trim="shippingJobForm.driverName">
            </label>
            <label>{{ labels.driverPhone }}
              <input v-model.trim="shippingJobForm.driverPhone">
            </label>
            <label class="shipping-job-remark">{{ labels.remark }}
              <input v-model.trim="shippingJobForm.remark">
            </label>
            <fieldset class="shipping-order-picker">
              <legend>{{ labels.bindOutboundOrders }}</legend>
              <label v-for="order in bindableOutboundOrders" :key="order.id">
                <input v-model="shippingJobForm.outboundOrderIds" type="checkbox" :value="order.id">
                <span>{{ order.orderNo }} / {{ outboundStatus(order.status) }}</span>
              </label>
              <span v-if="!bindableOutboundOrders.length" class="hint">{{ labels.noBindableOrders }}</span>
            </fieldset>
            <button class="primary" :disabled="loading.shippingJob">
              {{ loading.shippingJob ? text.submitting : labels.createShippingJob }}
            </button>
          </form>
          <div class="shipping-job-layout">
            <div class="shipping-job-list">
              <article v-for="job in shippingJobs" :key="job.id"
                       :class="['location-card', 'clickable', { active: selectedShippingJob?.id === job.id }]"
                       @click="selectedShippingJob = job">
                <b>{{ job.jobNo }}</b>
                <span>{{ shippingJobStatus(job.status) }} / {{ job.plannedShipDate }}</span>
                <em>{{ job.truckNo || '-' }} / {{ job.orders?.length || 0 }} {{ labels.ordersUnit }}</em>
              </article>
              <div v-if="!shippingJobs.length" class="empty-state">{{ labels.noShippingJobs }}</div>
            </div>
            <section v-if="selectedShippingJob" class="detail-column shipping-job-detail">
              <div class="panel-head">
                <div>
                  <h3>{{ selectedShippingJob.jobNo }}</h3>
                  <span class="status-tag">{{ shippingJobStatus(selectedShippingJob.status) }}</span>
                </div>
                <div class="head-actions">
                  <button v-if="selectedShippingJob.status === 'DRAFT'" class="primary" @click="runShippingJobAction('schedule')">{{ labels.scheduleJob }}</button>
                  <button v-if="selectedShippingJob.status === 'SCHEDULED'" class="primary" @click="runShippingJobAction('ship')">{{ labels.markShipped }}</button>
                  <button v-if="!['SHIPPED', 'CANCELLED'].includes(selectedShippingJob.status)" class="danger" @click="runShippingJobAction('cancel')">{{ labels.cancelJob }}</button>
                </div>
              </div>
              <div class="detail-kv"><span>{{ labels.plannedShipDate }}</span><b>{{ selectedShippingJob.plannedShipDate }}</b></div>
              <div class="detail-kv"><span>{{ labels.truckNo }}</span><b>{{ selectedShippingJob.truckNo || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.driverName }}</span><b>{{ selectedShippingJob.driverName || '-' }} / {{ selectedShippingJob.driverPhone || '-' }}</b></div>
              <form v-if="selectedShippingJob.status === 'DRAFT' && bindableOutboundOrders.length"
                    class="shipping-add-orders" @submit.prevent="addShippingOrders">
                <label>{{ labels.addOutboundOrders }}
                  <select v-model="shippingOrdersToAdd" multiple>
                    <option v-for="order in bindableOutboundOrders" :key="order.id" :value="order.id">
                      {{ order.orderNo }} / {{ outboundStatus(order.status) }}
                    </option>
                  </select>
                </label>
                <button :disabled="!shippingOrdersToAdd.length || loading.shippingJob">{{ labels.addSelectedOrders }}</button>
              </form>
              <h3>{{ labels.boundOrders }}</h3>
              <article v-for="order in selectedShippingJob.orders" :key="order.orderId" class="detail-item shipping-order-ref">
                <div>
                  <b>{{ order.orderNo }}</b>
                  <span>{{ outboundStatus(order.orderStatus) }} / {{ order.receiverName || '-' }}</span>
                </div>
                <button v-if="selectedShippingJob.status === 'DRAFT'" class="danger" @click="removeShippingOrder(order.orderId)">{{ labels.removeItem }}</button>
              </article>
            </section>
          </div>
        </section>

        <section v-if="view === 'picking'" class="panel">
          <div class="panel-head">
            <div>
              <h2>{{ labels.pickingManagement }}</h2>
              <p class="panel-description">{{ labels.pickingHint }}</p>
            </div>
            <button @click="loadOutboundOrders">{{ text.refresh }}</button>
          </div>
          <form class="picking-search" @submit.prevent="searchPickingOrder">
            <input v-model.trim="pickingOrderNo" :placeholder="labels.searchPickingPlaceholder">
            <button class="primary">{{ labels.search }}</button>
          </form>
          <div class="cards">
            <article v-for="order in filteredPickingOrders" :key="order.id" class="location-card clickable" @click="enterPickingOrder(order)">
              <b>{{ order.orderNo }}</b>
              <span>{{ typeName(order.type) }} / {{ outboundStatus(order.status) }}</span>
              <em>{{ order.itemCount || 0 }} SKU / {{ order.operatorName || '-' }}</em>
            </article>
          </div>
          <div v-if="!filteredPickingOrders.length" class="empty-state">{{ labels.noPickingOrders }}</div>
        </section>

        <section v-if="view === 'outbound-detail'" class="panel order-detail-panel">
          <div class="panel-head">
            <div>
              <h2>{{ labels.outboundDetail }} {{ outboundOrderDetail?.orderNo || '' }}</h2>
              <p class="panel-description">{{ outboundOrderDetail ? outboundStatus(outboundOrderDetail.status) : '' }}</p>
            </div>
            <div class="head-actions">
              <button v-if="['CREATED', 'IN_QUEUE'].includes(outboundOrderDetail?.status)" class="primary" :disabled="loading.outboundAction" @click="generatePickList">{{ labels.generatePickList }}</button>
              <button v-if="['ALLOCATED', 'NOT_ENOUGH_INV'].includes(outboundOrderDetail?.status)" class="primary" :disabled="loading.outboundAction" @click="assignOutboundPicking">{{ labels.assignPicking }}</button>
              <button v-if="outboundOrderDetail?.status === 'READY_TO_PICK'" class="primary" :disabled="loading.outboundAction" @click="startOutboundPicking">{{ labels.enterPicking }}</button>
              <button v-if="outboundOrderDetail?.status === 'PICKED'" class="primary" :disabled="loading.outboundAction" @click="confirmOutboundOrder">{{ labels.confirmOutbound }}</button>
              <button v-if="['CREATED', 'IN_QUEUE'].includes(outboundOrderDetail?.status)" class="danger" :disabled="loading.outboundAction" @click="cancelOutboundOrder">{{ labels.cancelOutbound }}</button>
              <button @click="openView('outbound-orders')">{{ text.back }}</button>
            </div>
          </div>
          <div v-if="outboundOrderDetail" class="outbound-detail-grid">
            <section class="detail-column">
              <h3>{{ labels.orderBasic }}</h3>
              <div class="detail-kv"><span>{{ labels.orderNo }}</span><b>{{ outboundOrderDetail.orderNo }}</b></div>
              <div class="detail-kv"><span>{{ labels.orderType }}</span><b>{{ typeName(outboundOrderDetail.type) }}</b></div>
              <div class="detail-kv"><span>{{ labels.status }}</span><b>{{ outboundStatus(outboundOrderDetail.status) }}</b></div>
              <div class="detail-kv"><span>{{ labels.createdAt }}</span><b>{{ formatTime(outboundOrderDetail.createdAt) }}</b></div>
              <div class="detail-kv"><span>{{ labels.pickingStartedAt }}</span><b>{{ formatTime(outboundOrderDetail.pickingStartedAt) }}</b></div>
              <div class="detail-kv"><span>{{ labels.pickedAt }}</span><b>{{ formatTime(outboundOrderDetail.pickedAt) }}</b></div>
              <div class="detail-kv"><span>{{ labels.outboundAt }}</span><b>{{ formatTime(outboundOrderDetail.completedAt) }}</b></div>
              <div class="detail-kv"><span>{{ labels.reason }}</span><b>{{ outboundOrderDetail.reason || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.remark }}</span><b>{{ outboundOrderDetail.remark || '-' }}</b></div>
              <div v-if="outboundOrderDetail.shortageDetails" class="detail-kv shortage-detail"><span>{{ labels.shortageDetails }}</span><b>{{ outboundOrderDetail.shortageDetails }}</b></div>
              <div v-if="outboundOrderDetail.backOrderNo" class="detail-kv"><span>{{ labels.backOrderNo }}</span><b>{{ outboundOrderDetail.backOrderNo }}</b></div>
            </section>
            <section class="detail-column">
              <h3>{{ labels.receiverInfo }}</h3>
              <div class="detail-kv"><span>{{ labels.receiverName }}</span><b>{{ outboundOrderDetail.receiverName || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.receiverPhone }}</span><b>{{ outboundOrderDetail.receiverPhone || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.address }}</span><b>{{ outboundOrderDetail.address || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.trackingNo }}</span><b>{{ outboundOrderDetail.trackingNo || '-' }}</b></div>
              <div class="detail-kv"><span>{{ labels.operator }}</span><b>{{ outboundOrderDetail.operatorName || '-' }}</b></div>
            </section>
            <section class="detail-column outbound-items-column">
              <h3>{{ labels.orderItems }}</h3>
              <article v-for="item in outboundOrderDetail.items" :key="item.itemId" class="detail-item">
                <b>{{ item.sku }} · {{ item.productName }}</b>
                <span>{{ item.modelSpec || '-' }} / {{ item.unitName || '-' }}</span>
                <em>{{ item.warehouseName }} / {{ item.locationCode }}</em>
                <footer>{{ labels.outboundQty }} {{ item.quantity }} / {{ labels.pickedQty }} {{ item.pickedQuantity || 0 }} / {{ labels.availableQty }} {{ item.availableQuantity }}</footer>
              </article>
            </section>
            <section class="detail-column outbound-history-column">
              <h3>{{ labels.orderHistory }}</h3>
              <article v-for="(history, index) in outboundOrderDetail.histories" :key="index" class="history-item">
                <b>{{ historyName(history.operationType) }}</b>
                <span>{{ formatTime(history.operationTime) }}</span>
                <em>{{ history.operatorName || '-' }}</em>
                <p>{{ history.remark }}</p>
              </article>
            </section>
          </div>
          <section v-if="outboundOrderDetail?.status === 'PICKING'" class="picking-workspace">
            <div class="panel-head">
              <div><h3>{{ labels.pickingTask }}</h3><p class="panel-description">{{ labels.pickingTaskHint }}</p></div>
              <button class="primary" :disabled="loading.outboundAction" @click="completeOutboundPicking">{{ labels.completePicking }}</button>
            </div>
            <div class="picking-lines">
              <article v-for="item in outboundOrderDetail.items" :key="item.itemId" :class="['picking-line', pickingItemClass(item)]">
                <div>
                  <b>{{ item.sku }} · {{ item.productName }}</b>
                  <span>{{ labels.shelfCode }} {{ item.shelfCode || '-' }} / {{ labels.locationCode }} {{ item.locationCode }}</span>
                  <em>{{ pickingStatusName(item) }}</em>
                </div>
                <label>{{ labels.requiredQty }}<input :value="item.quantity" disabled></label>
                <label>{{ labels.actualPickedQty }}<input v-model.number="pickingQuantities[item.itemId]" type="number" min="0" :max="item.quantity"></label>
                <button type="button" @click="markItemPicked(item)">{{ labels.markPicked }}</button>
              </article>
            </div>
          </section>
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
          <div v-if="!isInboundCreate" class="form-grid outbound-recipient-fields">
            <label>{{ labels.receiverName }}<input v-model.trim="orderForm.receiverName" :placeholder="labels.receiverNamePlaceholder"></label>
            <label>{{ labels.receiverPhone }}<input v-model.trim="orderForm.receiverPhone" type="tel"></label>
            <label class="address-field">{{ labels.address }}<input v-model.trim="orderForm.address"></label>
            <label>{{ labels.reason }}<input v-model.trim="orderForm.reason"></label>
            <label>{{ labels.trackingNo }}<input v-model.trim="orderForm.trackingNo"></label>
          </div>
          <div class="order-lines-head">
            <h3>{{ labels.orderItems }}</h3>
            <button @click="addOrderLine">{{ labels.addItem }}</button>
          </div>
          <div class="order-lines">
            <div v-for="(line, index) in orderForm.items" :key="line.key" class="order-line">
              <div class="product-picker">
                <label>{{ labels.productCode }}
                  <input v-model.trim="line.productKeyword" :placeholder="labels.productSearchPlaceholder" autocomplete="off" @focus="line.showProductOptions = true" @input="handleProductKeyword(line)" @keydown.enter.prevent="selectFirstProduct(line)">
                </label>
                <div v-if="line.showProductOptions && line.productKeyword" class="product-options">
                  <button v-for="p in productMatches(line)" :key="p.id" type="button" @click="selectLineProduct(line, p)">
                    <b>{{ p.sku }}</b>
                    <span>{{ p.name }} · {{ p.modelSpec || '-' }} · {{ p.unitName || '-' }}</span>
                  </button>
                  <p v-if="!productMatches(line).length">{{ labels.noProductMatch }}</p>
                </div>
                <small v-if="line.productId" class="selected-product">{{ selectedProductText(line) }}</small>
              </div>
              <label>{{ labels.warehouse }}
                <input :value="currentWarehouse?.code || '-'" disabled>
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
              <input :value="currentWarehouse?.code || '-'" disabled>
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
            <input :value="currentWarehouse?.code || '-'" disabled>
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
              <input ref="orderNoRef" v-model.trim="receiveForm.orderNo" placeholder="IN202606010001" @focus="selectField('orderNo')" @input="scheduleScan('orderNo')" @keyup.enter="handleEnter('orderNo')">
            </label>
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
              <button class="link-button tracking-button" @click="selectTracking(item)">{{ item.trackingNo }}</button>
              <p>{{ item.modelSpec }} / {{ item.unitName }}</p>
              <footer>{{ labels.expectedQty }} {{ item.expectedQuantity }} / {{ labels.receivedQty }} {{ item.receivedQuantity }} / {{ labels.remainingQty }} {{ item.remainingQuantity }}</footer>
            </article>
          </section>

          <section v-if="inboundOrder" class="scan-box">
            <label :class="{ error: errorField === 'locationCode' }">{{ labels.locationCode }}
              <input ref="locationCodeRef" v-model.trim="receiveForm.locationCode" placeholder="LT-A01-1-1-1" @focus="selectField('locationCode')" @input="scheduleScan('locationCode')" @keyup.enter="handleEnter('locationCode')">
            </label>
            <label :class="{ error: errorField === 'productCode' }">{{ labels.productCode }}
              <input ref="productCodeRef" v-model.trim="receiveForm.productCode" placeholder="P001 / 697000000001" @focus="selectField('productCode')" @input="scheduleScan('productCode')" @keyup.enter="handleEnter('productCode')">
            </label>
            <div class="mode-row">
              <button :class="{ active: qtyMode === 'fixed' }" @click="setQtyMode('fixed')">{{ labels.fixedQty }}</button>
              <button :class="{ active: qtyMode === 'custom' }" @click="setQtyMode('custom')">{{ labels.customQty }}</button>
            </div>
            <label :class="{ disabled: qtyMode === 'fixed', error: errorField === 'quantity' }">{{ labels.qty }}
              <input ref="quantityRef" v-model.number="receiveForm.quantity" type="number" min="1" :disabled="qtyMode === 'fixed'" @focus="selectField('quantity')" @keyup.enter="handleEnter('quantity')">
            </label>
            <p class="auto-receive-hint">{{ loading.receiveSubmit ? labels.receivingNow : labels.autoReceiveHint }}</p>
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

        <section v-if="view === 'stock-distribution'" class="panel distribution-panel">
          <div class="panel-head distribution-head">
            <div>
              <h2>{{ labels.stockDistribution }}</h2>
              <p class="panel-description">{{ labels.stockDistributionHint }}</p>
            </div>
            <button @click="loadStockDistribution">{{ text.refresh }}</button>
          </div>
          <div class="distribution-toolbar">
            <label>{{ labels.warehouse }}
              <input :value="currentWarehouse?.code || '-'" disabled>
            </label>
            <div class="distribution-level">
              <span>{{ labels.currentLevel }}</span>
              <b>{{ selectedDistributionShelf ? `${labels.area} ${selectedDistributionShelf.code}` : labels.warehouseOverview }}</b>
            </div>
            <button v-if="selectedDistributionShelf" @click="backToDistributionOverview">{{ labels.backToOverview }}</button>
          </div>
          <div class="distribution-legend">
            <span><i class="empty"></i>{{ labels.emptyLocation }}</span>
            <span><i class="occupied"></i>{{ labels.occupiedLocation }}</span>
            <span><i class="full"></i>{{ labels.fullLocation }}</span>
            <span><i class="disabled"></i>{{ labels.disabled }}</span>
          </div>

          <div v-if="distributionShelves.length && !selectedDistributionShelf" class="warehouse-overview">
            <button
              v-for="shelf in distributionShelves"
              :key="shelf.code"
              class="warehouse-area"
              :style="{ '--usage': `${shelf.utilization}%` }"
              @click="openDistributionShelf(shelf)"
            >
              <span>{{ labels.area }}</span>
              <b>{{ shelf.code }}</b>
              <em>{{ shelf.totalQuantity }} {{ labels.pieces }}</em>
              <small>{{ shelf.locations.length - shelf.emptyCount }}/{{ shelf.locations.length }} {{ labels.occupiedSlots }}</small>
              <i><u></u></i>
            </button>
          </div>

          <section v-else-if="selectedDistributionShelf" class="area-detail">
            <div class="area-detail-head">
              <div>
                <span>{{ labels.area }}</span>
                <h3>{{ selectedDistributionShelf.code }}</h3>
              </div>
              <p>{{ selectedDistributionShelf.locations.length }} {{ labels.locations }} / {{ selectedDistributionShelf.emptyCount }} {{ labels.emptyLocations }}</p>
            </div>
            <div class="location-grid">
              <button
                v-for="location in selectedDistributionShelf.locations"
                :key="location.id"
                :class="['location-cell', distributionLocationClass(location), { selected: selectedDistributionLocation?.id === location.id }]"
                :title="distributionLocationTitle(location)"
                @click="selectedDistributionLocation = location"
              >
                <b>{{ shortLocationCode(location.code) }}</b>
                <span v-if="locationStockTotal(location.code)">{{ locationStockTotal(location.code) }} {{ labels.pieces }}</span>
                <span v-else>{{ labels.emptySlot }}</span>
              </button>
            </div>
          </section>
          <div v-if="!distributionShelves.length" class="empty-state">{{ labels.noDistributionData }}</div>

          <aside v-if="selectedDistributionLocation" class="location-inspector">
            <div class="panel-head">
              <div><h3>{{ selectedDistributionLocation.code }}</h3><p class="panel-description">{{ selectedDistributionLocation.shelfCode || labels.unshelved }}</p></div>
              <button @click="selectedDistributionLocation = null">{{ text.clear }}</button>
            </div>
            <div class="inspector-metrics">
              <div><span>{{ labels.stockQuantity }}</span><b>{{ locationStockTotal(selectedDistributionLocation.code) }}</b></div>
              <div><span>{{ labels.capacity }}</span><b>{{ selectedDistributionLocation.capacity || 0 }}</b></div>
              <div><span>{{ labels.utilization }}</span><b>{{ locationUtilization(selectedDistributionLocation) }}%</b></div>
            </div>
            <div class="inspector-products">
              <article v-for="stock in locationStocks(selectedDistributionLocation.code)" :key="stock.stockId">
                <b>{{ stock.sku }} · {{ stock.productName }}</b><span>{{ stock.quantity }} {{ labels.pieces }}</span>
              </article>
              <p v-if="!locationStocks(selectedDistributionLocation.code).length">{{ labels.emptyLocation }}</p>
            </div>
          </aside>
        </section>
      </section>
    </section>

    <aside v-if="user && selectedWarehouseId" ref="aiAssistantRef"
           :class="['ai-assistant', { open: aiChat.open, dragging: aiDrag.active }]"
           :style="aiChat.open ? aiAssistantStyle : undefined">
      <button v-if="!aiChat.open" class="ai-assistant-launcher" @click="openAiAssistant" aria-label="Open AI assistant">
        AI
      </button>
      <section v-else class="ai-assistant-panel">
        <header class="ai-assistant-drag-handle" @pointerdown="startAiDrag">
          <div>
            <b>{{ locale === 'en' ? 'WMS AI Assistant' : 'WMS AI 助手' }}</b>
            <span>{{ locale === 'en' ? 'Rules · Data · Reports' : '规则 · 数据 · 报表' }}</span>
          </div>
          <button @pointerdown.stop @click="aiChat.open = false">×</button>
        </header>
        <div ref="aiMessagesRef" class="ai-assistant-messages">
          <article v-for="(message, index) in aiChat.messages" :key="index" :class="message.role">
            <span>{{ message.role === 'user' ? (user.displayName || user.username) : agentName(message.agent) }}</span>
            <p>{{ message.content }}</p>
            <a v-if="message.reportId" :href="api.pdf(message.reportId)" target="_blank">
              {{ locale === 'en' ? 'Download PDF report' : '下载 PDF 报表' }}
            </a>
          </article>
          <article v-if="aiChat.loading" class="assistant">
            <span>AI</span>
            <p>{{ locale === 'en' ? 'Thinking…' : '正在分析…' }}</p>
          </article>
        </div>
        <div class="ai-assistant-suggestions">
          <button @click="askAiSuggestion(locale === 'en' ? 'What are the outbound rules?' : '出库流程有什么规则？')">
            {{ locale === 'en' ? 'Rules' : '规则' }}
          </button>
          <button @click="askAiSuggestion(locale === 'en' ? 'Summarize current warehouse inventory' : '分析当前仓库库存情况')">
            {{ locale === 'en' ? 'Analyze' : '分析库存' }}
          </button>
          <button @click="askAiSuggestion(locale === 'en' ? 'Export a warehouse operations report' : '导出一份仓储运营报表')">
            {{ locale === 'en' ? 'Report' : '导出报表' }}
          </button>
        </div>
        <form @submit.prevent="sendAiMessage">
          <textarea v-model.trim="aiChat.input" rows="2"
                    :placeholder="locale === 'en' ? 'Ask about rules, data, or reports…' : '询问规则、业务数据或导出报表…'"
                    @keydown.enter.exact.prevent="sendAiMessage"></textarea>
          <button class="primary" :disabled="aiChat.loading || !aiChat.input">
            {{ locale === 'en' ? 'Send' : '发送' }}
          </button>
        </form>
      </section>
    </aside>

    <div v-if="toast.text" :class="['toast', toast.type]">{{ toast.text }}</div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { api } from './api'

const locale = ref(localStorage.getItem('wms-locale') || 'zh')
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
  orderSearch: '\u8ba2\u5355\u641c\u7d22',
  orderSearchHint: '\u6309\u8ba2\u5355\u72b6\u6001\u3001\u521b\u5efa\u65e5\u671f\u548c\u521b\u5efa\u4eba\u7b5b\u9009\u5165\u5e93\u53ca\u51fa\u5e93\u8ba2\u5355',
  orderNo: '\u8ba2\u5355\u53f7',
  orderNoPlaceholder: '\u8f93\u5165\u8ba2\u5355\u53f7',
  direction: '\u8ba2\u5355\u65b9\u5411',
  allDirections: '\u5168\u90e8\u65b9\u5411',
  inbound: '\u5165\u5e93',
  outbound: '\u51fa\u5e93',
  creator: '\u521b\u5efa\u4eba',
  creatorPlaceholder: '\u8f93\u5165\u521b\u5efa\u4eba',
  createdFrom: '\u521b\u5efa\u8d77\u59cb\u65e5\u671f',
  createdTo: '\u521b\u5efa\u7ed3\u675f\u65e5\u671f',
  createdAt: '\u521b\u5efa\u65f6\u95f4',
  itemCount: 'SKU \u6570',
  search: '\u67e5\u8be2',
  searching: '\u67e5\u8be2\u4e2d...',
  reset: '\u91cd\u7f6e',
  searchResult: '\u67e5\u8be2\u7ed3\u679c',
  records: '\u6761',
  noOrders: '\u6682\u65e0\u7b26\u5408\u6761\u4ef6\u7684\u8ba2\u5355',
  selectWarehouse: '\u9009\u62e9\u4ed3\u5e93',
  selectWarehouseHint: '\u8bf7\u9009\u62e9\u8981\u8fdb\u5165\u7684\u4ed3\u5e93\uff0c\u8fdb\u5165\u540e\u4e5f\u53ef\u5728\u53f3\u4e0a\u89d2\u5207\u6362',
  enterWarehouse: '\u8fdb\u5165\u4ed3\u5e93',
  currentWarehouse: '\u5f53\u524d\u4ed3\u5e93',
  noWarehouse: '\u6682\u65e0\u53ef\u7528\u4ed3\u5e93',
  inboundOrders: '\u5165\u5e93\u8ba2\u5355',
  outboundOrders: '\u51fa\u5e93\u8ba2\u5355',
  shippingJobs: 'Shipping Job',
  shippingJobsHint: '\u5c06\u540c\u4e00\u4ed3\u5e93\u3001\u540c\u4e00\u8f66\u6b21\u53d1\u8fd0\u7684\u591a\u4e2a\u51fa\u5e93\u8ba2\u5355\u7ec4\u6210\u4e00\u4e2a Shipping Job',
  createShippingJob: '\u65b0\u5efa Shipping Job',
  plannedShipDate: '\u8ba1\u5212\u53d1\u8fd0\u65e5\u671f',
  truckNo: '\u8f66\u724c\u53f7/\u8f66\u8f86\u7f16\u53f7',
  truckNoPlaceholder: '\u6392\u8f66\u524d\u5fc5\u586b',
  driverName: '\u53f8\u673a',
  driverPhone: '\u53f8\u673a\u7535\u8bdd',
  bindOutboundOrders: '\u7ed1\u5b9a\u51fa\u5e93\u8ba2\u5355',
  noBindableOrders: '\u6682\u65e0\u53ef\u7ed1\u5b9a\u7684\u51fa\u5e93\u8ba2\u5355',
  noShippingJobs: '\u6682\u65e0 Shipping Job',
  ordersUnit: '\u4e2a\u8ba2\u5355',
  scheduleJob: '\u786e\u8ba4\u6392\u8f66',
  markShipped: '\u6807\u8bb0\u5df2\u53d1\u8fd0',
  cancelJob: '\u53d6\u6d88 Job',
  boundOrders: '\u5df2\u7ed1\u5b9a\u51fa\u5e93\u8ba2\u5355',
  addOutboundOrders: '\u7ee7\u7eed\u6dfb\u52a0\u51fa\u5e93\u8ba2\u5355',
  addSelectedOrders: '\u6dfb\u52a0\u9009\u4e2d\u8ba2\u5355',
  createInbound: '\u65b0\u5efa\u5165\u5e93\u5355',
  createOutbound: '\u65b0\u5efa\u51fa\u5e93\u5355',
  outboundDetail: '\u51fa\u5e93\u8ba2\u5355\u8be6\u60c5',
  confirmOutbound: '\u786e\u8ba4\u51fa\u5e93',
  cancelOutbound: '\u53d6\u6d88\u51fa\u5e93',
  pickingManagement: '\u62e3\u8d27\u51fa\u5e93',
  pickingHint: '\u5f85\u62e3\u8d27\u3001\u62e3\u8d27\u4e2d\u548c\u62e3\u8d27\u5b8c\u6210\u7684\u51fa\u5e93\u4efb\u52a1',
  noPickingOrders: '\u6682\u65e0\u5f85\u5904\u7406\u7684\u62e3\u8d27\u4efb\u52a1',
  startPicking: '\u5f00\u59cb\u62e3\u8d27',
  completePicking: '\u5b8c\u6210\u62e3\u8d27',
  pickingStartedAt: '\u5f00\u59cb\u62e3\u8d27\u65f6\u95f4',
  pickedAt: '\u62e3\u8d27\u5b8c\u6210\u65f6\u95f4',
  pickedQty: '\u5df2\u62e3\u6570\u91cf',
  pickingTask: '\u62e3\u8d27\u4efb\u52a1',
  pickingTaskHint: '\u6309\u8d27\u4f4d\u62e3\u53d6\u5546\u54c1\uff0c\u6838\u5bf9\u5b9e\u9645\u62e3\u8d27\u6570\u91cf\u540e\u5b8c\u6210\u4efb\u52a1',
  requiredQty: '\u5e94\u62e3\u6570\u91cf',
  actualPickedQty: '\u5b9e\u9645\u62e3\u8d27\u6570\u91cf',
  outboundAt: '\u51fa\u5e93\u65f6\u95f4',
  receiverInfo: '\u6536\u8d27\u4fe1\u606f',
  receiverName: '\u6536\u8d27\u4eba/\u5ba2\u6237\u540d\u79f0',
  receiverNamePlaceholder: '\u8bf7\u8f93\u5165\u6536\u8d27\u4eba\u6216\u5ba2\u6237\u540d\u79f0',
  receiverPhone: '\u8054\u7cfb\u7535\u8bdd',
  address: '\u6536\u8d27\u5730\u5740',
  reason: '\u51fa\u5e93\u539f\u56e0',
  trackingNo: '\u7269\u6d41\u5355\u53f7/\u8ffd\u8e2a\u7f16\u53f7',
  outboundQty: '\u51fa\u5e93\u6570\u91cf',
  availableQty: '\u5f53\u524d\u53ef\u7528\u5e93\u5b58',
  orderType: '\u8ba2\u5355\u7c7b\u578b',
  operator: '\u64cd\u4f5c\u4eba',
  orderItems: '\u8ba2\u5355\u660e\u7ec6',
  addItem: '\u6dfb\u52a0\u660e\u7ec6',
  removeItem: '\u5220\u9664',
  submitOrder: '\u63d0\u4ea4\u8ba2\u5355',
  product: '\u5546\u54c1',
  productSearchPlaceholder: '\u8f93\u5165 SKU\u3001\u6761\u7801\u6216\u5546\u54c1\u540d\u79f0',
  noProductMatch: '\u672a\u627e\u5230\u5339\u914d\u5546\u54c1',
  orderDetail: '\u8ba2\u5355\u8be6\u60c5',
  orderBasic: '\u8ba2\u5355\u57fa\u672c\u4fe1\u606f',
  productInfo: '\u5546\u54c1\u4fe1\u606f',
  orderHistory: '\u8ba2\u5355\u64cd\u4f5c\u5386\u53f2',
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
  receivingNow: '\u6b63\u5728\u6536\u8d27...',
  autoReceiveHint: '\u56fa\u5b9a\u6570\u91cf\u626b\u7801\u540e\u81ea\u52a8\u6536\u8d27\uff0c\u81ea\u5b9a\u4e49\u6570\u91cf\u8f93\u5165\u540e\u6309\u56de\u8f66\u6536\u8d27',
  scanResult: '\u5f53\u524d\u626b\u63cf\u7ed3\u679c',
  productName: '\u5546\u54c1\u540d\u79f0',
  spec: '\u89c4\u683c\u578b\u53f7',
  unit: '\u5355\u4f4d',
  expectedQty: '\u5e94\u6536\u6570\u91cf',
  receivedQty: '\u5df2\u6536\u6570\u91cf',
  remainingQty: '\u5269\u4f59\u6570\u91cf',
  thisQty: '\u672c\u6b21\u6536\u8d27\u6570\u91cf',
  stockQuery: '\u5e93\u5b58\u67e5\u8be2',
  stockFlow: '\u5e93\u5b58\u6d41\u6c34',
  stockDistribution: '\u4ed3\u5e93\u5e93\u5b58\u4fef\u89c6\u56fe',
  stockDistributionHint: '\u4ece\u4ed3\u5e93\u4fef\u89c6\u56fe\u9009\u62e9\u533a\u57df\uff0c\u8fdb\u5165\u540e\u67e5\u770b 1-1-1 \u7b49\u683c\u4f4d\u7684\u5e93\u5b58\u548c\u7a7a\u4f4d\u60c5\u51b5',
  horizontalAngle: '\u6c34\u5e73\u89c6\u89d2',
  pitchAngle: '\u4fef\u4ef0\u89c6\u89d2',
  resetView: '\u91cd\u7f6e\u89c6\u89d2',
  emptyLocation: '\u7a7a\u8d27\u4f4d',
  occupiedLocation: '\u6709\u5e93\u5b58',
  fullLocation: '\u5df2\u6ee1\u8f7d',
  locations: '\u4e2a\u8d27\u4f4d',
  pieces: '\u4ef6',
  noDistributionData: '\u5f53\u524d\u4ed3\u5e93\u6682\u65e0\u8d27\u4f4d\u6570\u636e',
  unshelved: '\u672a\u5206\u914d\u8d27\u67b6',
  stockQuantity: '\u5e93\u5b58\u6570\u91cf',
  utilization: '\u4f7f\u7528\u7387',
  language: '\u8bed\u8a00',
  confirmInbound: '\u786e\u8ba4\u5165\u5e93\u5b8c\u6210',
  cancelOrder: '\u53d6\u6d88\u8ba2\u5355',
  generatePickList: 'Generate Pick List',
  assignPicking: 'Assign Picking',
  enterPicking: '\u8fdb\u5165\u62e3\u8d27',
  searchPickingPlaceholder: '\u8f93\u5165\u51fa\u5e93\u8ba2\u5355\u53f7',
  markPicked: '\u6807\u8bb0\u5df2\u62e3',
  shortageDetails: '\u7f3a\u8d27\u660e\u7ec6',
  backOrderNo: '\u56de\u8865\u51fa\u5e93\u5355',
  emptyLocations: '\u4e2a\u7a7a\u4f4d',
  emptySlot: '\u7a7a\u8d27\u4f4d',
  currentLevel: '\u5f53\u524d\u89c6\u56fe',
  warehouseOverview: '\u4ed3\u5e93\u4fef\u89c6\u56fe',
  area: '\u533a\u57df',
  occupiedSlots: '\u683c\u5df2\u4f7f\u7528',
  backToOverview: '\u8fd4\u56de\u4ed3\u5e93\u4fef\u89c6\u56fe'
}
const modules = [
  module('order', '\u8ba2\u5355\u7ba1\u7406', '\u5355', [['order-search', labels.orderSearch]]),
  module('inbound', '\u5165\u5e93\u6a21\u5757', '\u5165', [['inbound-orders', labels.inboundOrders], ['inbound-create', labels.createInbound], ['receiving', labels.receiving], ['inbound-records', '\u5165\u5e93\u8bb0\u5f55']]),
  module('outbound', '\u51fa\u5e93\u6a21\u5757', '\u51fa', [['outbound-orders', labels.outboundOrders], ['outbound-create', labels.createOutbound], ['picking', labels.pickingManagement], ['shipping-jobs', labels.shippingJobs], ['outbound-records', '\u51fa\u5e93\u8bb0\u5f55']]),
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

const zhText = { ...text }
const zhLabels = { ...labels }
const englishText = {
  system: 'Smart WMS', position: 'Mobile-first warehouse operations', account: 'Account', password: 'Password',
  login: 'Log in', loggingIn: 'Logging in...', workbench: 'Smart WMS Workbench', logout: 'Log out',
  pickModule: 'Select an operation module', phaseHint: 'Reserved module', refresh: 'Refresh', back: 'Back',
  select: 'Select', submitting: 'Submitting...', clear: 'Clear', backWorkbench: 'Back to Workbench'
}
const englishLabels = {
  orderSearch: 'Order Search', orderSearchHint: 'Filter inbound and outbound orders by status, creation date, and creator',
  orderNo: 'Order No.', orderNoPlaceholder: 'Enter order number', direction: 'Direction', allDirections: 'All Directions',
  inbound: 'Inbound', outbound: 'Outbound', creator: 'Creator', creatorPlaceholder: 'Enter creator',
  createdFrom: 'Created From', createdTo: 'Created To', createdAt: 'Created At', itemCount: 'SKU Count',
  search: 'Search', searching: 'Searching...', reset: 'Reset', searchResult: 'Results', records: 'records',
  noOrders: 'No matching orders', selectWarehouse: 'Select Warehouse',
  selectWarehouseHint: 'Select a warehouse. You can switch it later in the top-right corner.',
  enterWarehouse: 'Enter Warehouse', currentWarehouse: 'Current Warehouse', noWarehouse: 'No warehouse available',
  inboundOrders: 'Inbound Orders', outboundOrders: 'Outbound Orders', createInbound: 'New Inbound Order',
  shippingJobs: 'Shipping Jobs', shippingJobsHint: 'Group outbound orders delivered by the same truck on the same day',
  createShippingJob: 'New Shipping Job', plannedShipDate: 'Planned Ship Date', truckNo: 'Truck No.',
  truckNoPlaceholder: 'Required before scheduling', driverName: 'Driver', driverPhone: 'Driver Phone',
  bindOutboundOrders: 'Bind Outbound Orders', noBindableOrders: 'No outbound orders available',
  noShippingJobs: 'No shipping jobs', ordersUnit: 'orders', scheduleJob: 'Schedule',
  markShipped: 'Mark Shipped', cancelJob: 'Cancel Job', boundOrders: 'Bound Outbound Orders',
  addOutboundOrders: 'Add Outbound Orders', addSelectedOrders: 'Add Selected',
  createOutbound: 'New Outbound Order', outboundDetail: 'Outbound Order Details', confirmOutbound: 'Confirm Outbound',
  cancelOutbound: 'Cancel Outbound', pickingManagement: 'Picking', pickingHint: 'Search and process picking tasks',
  noPickingOrders: 'No picking tasks', startPicking: 'Start Picking', completePicking: 'Complete Picking',
  pickingStartedAt: 'Picking Started At', pickedAt: 'Picked At', pickedQty: 'Picked Qty',
  pickingTask: 'Picking Task', pickingTaskHint: 'Pick each item from its shelf and verify the quantity',
  requiredQty: 'Required Qty', actualPickedQty: 'Actual Picked Qty', outboundAt: 'Outbound At',
  receiverInfo: 'Receiver Information', receiverName: 'Receiver / Customer', receiverNamePlaceholder: 'Enter receiver or customer',
  receiverPhone: 'Phone', address: 'Address', reason: 'Outbound Reason', trackingNo: 'Tracking No.',
  outboundQty: 'Outbound Qty', availableQty: 'Available Qty', orderType: 'Order Type', operator: 'Operator',
  orderItems: 'Order Items', addItem: 'Add Item', removeItem: 'Remove', submitOrder: 'Submit Order',
  product: 'Product', productSearchPlaceholder: 'Enter SKU, barcode, or product name', noProductMatch: 'No matching product',
  orderDetail: 'Order Details', orderBasic: 'Basic Information', productInfo: 'Product Information',
  orderHistory: 'Order History', warehouse: 'Warehouse', shelfCode: 'Shelf', capacity: 'Capacity', remark: 'Remark',
  allWarehouse: 'All Warehouses', allStatus: 'All Statuses', receiving: 'Receiving', receiveOrder: 'Receive Order',
  inboundNo: 'Inbound No.', supplier: 'Supplier', status: 'Status', progress: 'Progress', locationCode: 'Location',
  productCode: 'Product Code / Barcode', qty: 'Quantity', expectedQty: 'Expected Qty', receivedQty: 'Received Qty',
  remainingQty: 'Remaining Qty', stockQuery: 'Stock Query', stockFlow: 'Stock Movements',
  language: 'Language', confirmInbound: 'Confirm', cancelOrder: 'Cancel Order',
  generatePickList: 'Generate Pick List', assignPicking: 'Assign Picking', enterPicking: 'Enter Picking',
  searchPickingPlaceholder: 'Enter outbound order number', markPicked: 'Mark Picked',
  shortageDetails: 'Shortage Details', backOrderNo: 'Back Outbound Order',
  emptyLocations: 'empty', emptySlot: 'Empty',
  currentLevel: 'Current View', warehouseOverview: 'Warehouse Overview', area: 'Area',
  occupiedSlots: 'slots occupied', backToOverview: 'Back to Overview',
  shelfCreate: 'Create Shelf', shelfName: 'Shelf Name', previewLocation: 'Preview Locations', generate: 'Generate',
  willGenerate: 'Locations to generate', locationQuery: 'Location Query', available: 'Available / In Use',
  full: 'Full', disabled: 'Disabled', changeOrder: 'Change Order', fixedQty: 'Fixed 1 Item',
  customQty: 'Custom Quantity', receivingNow: 'Receiving...', autoReceiveHint: 'Scan to receive a fixed quantity, or enter a custom quantity and press Enter',
  scanResult: 'Current Scan Result', productName: 'Product Name', spec: 'Specification', unit: 'Unit',
  thisQty: 'Quantity This Time', stockDistribution: 'Warehouse Inventory Overview',
  stockDistributionHint: 'Select an area from the top-down warehouse view, then inspect inventory in cells such as 1-1-1',
  horizontalAngle: 'Horizontal Angle', pitchAngle: 'Pitch Angle', resetView: 'Reset View',
  emptyLocation: 'Empty Location', occupiedLocation: 'Occupied', fullLocation: 'Full',
  locations: 'locations', pieces: 'items', noDistributionData: 'No location data for this warehouse',
  unshelved: 'Unassigned Shelf', stockQuantity: 'Stock Quantity', utilization: 'Utilization'
}
const moduleEnglish = {
  order: 'Order Management', inbound: 'Inbound', outbound: 'Outbound', stock: 'Inventory',
  shelf: 'Shelf Management', product: 'Product Management', check: 'Stock Check',
  alert: 'Inventory Alerts', ai: 'AI Warehouse Assistant', settings: 'Settings'
}
const submoduleEnglish = {
  'order-search': 'Order Search', 'inbound-orders': 'Inbound Orders', 'inbound-create': 'New Inbound Order',
  receiving: 'Receiving', 'inbound-records': 'Inbound Records', 'outbound-orders': 'Outbound Orders',
  'outbound-create': 'New Outbound Order', picking: 'Picking', 'shipping-jobs': 'Shipping Jobs', 'outbound-records': 'Outbound Records',
  'stock-query': 'Stock Query', 'stock-flow': 'Stock Movements', 'stock-adjust': 'Stock Adjustment',
  'stock-distribution': 'Inventory Distribution', 'shelf-create': 'Create Shelf', 'shelf-list': 'Shelf List',
  'location-query': 'Location Query', 'label-print': 'Print Shelf Labels', 'product-list': 'Product List',
  'product-create': 'New Product', 'product-category': 'Product Categories', barcode: 'Barcode Management',
  'check-create': 'New Stock Check', 'check-task': 'Stock Check Tasks', 'pda-check': 'PDA Stock Check',
  'check-record': 'Stock Check Records', 'low-alert': 'Low Stock Alerts', 'over-alert': 'Overstock Alerts',
  'alert-record': 'Alert Records', 'ai-chat': 'AI Chat', replenish: 'Replenishment Suggestions',
  'ai-analysis': 'Exception Analysis', 'ai-report': 'Warehouse Reports', users: 'User Management',
  roles: 'Roles and Permissions', 'warehouse-setting': 'Warehouse Settings', params: 'System Parameters'
}
const moduleChinese = Object.fromEntries(modules.map(item => [item.key, item.label]))
const submoduleChinese = Object.fromEntries(modules.flatMap(item => item.subs.map(sub => [sub.key, sub.label])))
function switchLanguage() {
  localStorage.setItem('wms-locale', locale.value)
  Object.assign(text, locale.value === 'en' ? englishText : zhText)
  Object.assign(labels, locale.value === 'en' ? englishLabels : zhLabels)
  modules.forEach(item => {
    item.label = locale.value === 'en' ? (moduleEnglish[item.key] || item.label) : moduleChinese[item.key]
    item.subs.forEach(sub => {
      sub.label = locale.value === 'en' ? (submoduleEnglish[sub.key] || sub.label) : submoduleChinese[sub.key]
    })
  })
}

const user = ref(JSON.parse(localStorage.getItem('wms-user') || 'null'))
const loginForm = reactive({ username: 'admin', password: '123456' })
const loginUserRef = ref(null)
const activeModule = ref('')
const view = ref('home')
const warehouses = ref([])
const selectedWarehouseId = ref(Number(localStorage.getItem('wms-warehouse-id')) || null)
const locations = ref([])
const products = ref([])
const stocks = ref([])
const movements = ref([])
const inboundOrders = ref([])
const outboundOrders = ref([])
const shippingJobs = ref([])
const selectedShippingJob = ref(null)
const shippingOrdersToAdd = ref([])
const orderSearchResults = ref([])
const distributionWarehouseId = ref(null)
const selectedDistributionLocation = ref(null)
const selectedDistributionShelfCode = ref('')
const dashboard = ref({})
const inboundOrder = ref(null)
const inboundOrderDetail = ref(null)
const outboundOrderDetail = ref(null)
const pickingQuantities = reactive({})
const pickingOrderNo = ref('')
const activeReceiveOrderNo = ref('')
const activeDetailOrderNo = ref('')
const activeOutboundOrderId = ref(null)
const shelfPreview = ref([])
const qtyMode = ref('fixed')
const errorField = ref('')
const lastScan = reactive({ value: '', time: 0 })
const scanTimers = {}
const toast = reactive({ text: '', type: 'success' })
const aiMessagesRef = ref(null)
const aiAssistantRef = ref(null)
const savedAiPosition = loadAiPosition()
const aiPosition = reactive(savedAiPosition || { x: null, y: null })
const aiDrag = reactive({ active: false, offsetX: 0, offsetY: 0 })
const aiAssistantStyle = computed(() => aiPosition.x == null || aiPosition.y == null
  ? {}
  : { left: `${aiPosition.x}px`, top: `${aiPosition.y}px`, right: 'auto', bottom: 'auto' })
const aiChat = reactive({
  open: false,
  loading: false,
  input: '',
  sessionId: localStorage.getItem('wms-ai-session') || createAiSessionId(),
  messages: [{
    role: 'assistant',
    agent: 'supervisor',
    content: locale.value === 'en'
      ? 'I can explain WMS rules, analyze current warehouse data, and export PDF reports.'
      : '我可以解答仓储规则、分析当前仓库数据，也可以生成并导出 PDF 报表。'
  }]
})
localStorage.setItem('wms-ai-session', aiChat.sessionId)
const loading = reactive({ login: false, shelf: false, receive: false, receiveSubmit: false, order: false, orderSearch: false, inboundAction: false, outboundAction: false, shippingJob: false })
const shelfForm = reactive({ warehouseId: null, shelfCode: 'A01', shelfName: 'A01\u8d27\u67b6', xCount: 3, yCount: 4, zCount: 2, capacity: 100, remark: '' })
const locationFilters = reactive({ warehouseId: null, code: '', status: '' })
const orderSearchFilters = reactive({ orderNo: '', direction: '', status: '', operatorName: '', createdFrom: '', createdTo: '' })
const receiveForm = reactive({ orderNo: '', locationCode: '', productCode: '', quantity: 1 })
const orderForm = reactive({ type: 'PURCHASE', operatorName: '', receiverName: '', receiverPhone: '', address: '', reason: '', trackingNo: '', remark: '', items: [] })
const shippingJobForm = reactive({
  plannedShipDate: new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10),
  truckNo: '',
  driverName: '',
  driverPhone: '',
  remark: '',
  outboundOrderIds: []
})
const scanResult = reactive({ location: null, product: null })
const refs = { orderNo: ref(null), locationCode: ref(null), productCode: ref(null), quantity: ref(null) }
const orderNoRef = refs.orderNo
const locationCodeRef = refs.locationCode
const productCodeRef = refs.productCode
const quantityRef = refs.quantity

const currentSubmodules = computed(() => modules.find(m => m.key === activeModule.value)?.subs || [])
const pickingOrders = computed(() => outboundOrders.value.filter(order => ['READY_TO_PICK', 'PICKING', 'PICKED'].includes(order.status)))
const filteredPickingOrders = computed(() => {
  const keyword = pickingOrderNo.value.trim().toLowerCase()
  return keyword ? pickingOrders.value.filter(order => order.orderNo.toLowerCase().includes(keyword)) : pickingOrders.value
})
const boundShippingOrderIds = computed(() => new Set(
  shippingJobs.value
    .filter(job => job.status !== 'CANCELLED')
    .flatMap(job => (job.orders || []).map(order => order.orderId))
))
const bindableOutboundOrders = computed(() => outboundOrders.value.filter(order =>
  order.status !== 'CANCELLED' && !boundShippingOrderIds.value.has(order.id)
))
const currentWarehouse = computed(() => warehouses.value.find(warehouse => warehouse.id === selectedWarehouseId.value) || null)
const stockByLocation = computed(() => stocks.value.reduce((map, stock) => {
  if (!map[stock.locationCode]) map[stock.locationCode] = []
  map[stock.locationCode].push(stock)
  return map
}, {}))
const distributionShelves = computed(() => {
  const groups = new Map()
  locations.value
    .filter(location => !distributionWarehouseId.value || location.warehouseId === distributionWarehouseId.value)
    .forEach(location => {
      const code = location.shelfCode || labels.unshelved
      if (!groups.has(code)) groups.set(code, [])
      groups.get(code).push(location)
    })
  return [...groups.entries()].map(([code, shelfLocations]) => {
    const sortedLocations = [...shelfLocations].sort(compareLocationPosition)
    const totalCapacity = sortedLocations.reduce((total, location) => total + Number(location.capacity || 0), 0)
    const totalQuantity = sortedLocations.reduce((total, location) => total + locationStockTotal(location.code), 0)
    return {
      code,
      locations: sortedLocations,
      totalQuantity,
      emptyCount: sortedLocations.filter(location => locationStockTotal(location.code) === 0).length,
      utilization: totalCapacity ? Math.min(100, Math.round(totalQuantity / totalCapacity * 100)) : 0
    }
  })
})
const selectedDistributionShelf = computed(() =>
  distributionShelves.value.find(shelf => shelf.code === selectedDistributionShelfCode.value) || null
)
const isInboundCreate = computed(() => view.value === 'inbound-create')
const currentOrderTypes = computed(() => isInboundCreate.value
  ? [
      { value: 'PURCHASE', label: typeName('PURCHASE') },
      { value: 'RETURN', label: typeName('RETURN') },
      { value: 'TRANSFER', label: typeName('TRANSFER') },
      { value: 'INVENTORY_GAIN', label: typeName('INVENTORY_GAIN') }
    ]
  : [
      { value: 'SALE', label: typeName('SALE') },
      { value: 'REQUISITION', label: typeName('REQUISITION') },
      { value: 'TRANSFER', label: typeName('TRANSFER') },
      { value: 'DAMAGE', label: typeName('DAMAGE') },
      { value: 'INVENTORY_LOSS', label: typeName('INVENTORY_LOSS') }
    ])
const placeholderView = computed(() => {
  if (['home', 'order-search', 'inbound-orders', 'inbound-detail', 'outbound-orders', 'outbound-detail', 'inbound-create', 'outbound-create', 'picking', 'shipping-jobs', 'shelf-create', 'location-query', 'receiving', 'stock-query', 'stock-flow', 'stock-distribution'].includes(view.value)) return ''
  return currentSubmodules.value.find(s => s.key === view.value)?.label || ''
})
const overviewCards = computed(() => [
  { label: '\u4eca\u65e5\u5165\u5e93', value: dashboard.value.todayInbound || 0 },
  { label: '\u4eca\u65e5\u51fa\u5e93', value: dashboard.value.todayOutbound || 0 },
  { label: '\u5e93\u5b58\u9884\u8b66', value: dashboard.value.warningCount || 0 },
  { label: locale.value === 'en' ? 'Inbound Queue' : '\u5f85\u6536\u8d27\u8ba2\u5355', value: inboundOrders.value.filter(o => ['CREATED', 'IN_QUEUE', 'RECEIVING', 'RECEIVED'].includes(o.status)).length },
  { label: locale.value === 'en' ? 'Outbound Queue' : '\u5f85\u51fa\u5e93\u8ba2\u5355', value: outboundOrders.value.filter(o => !['COMPLETED', 'CANCELLED'].includes(o.status)).length }
])

async function login() {
  loading.login = true
  try {
    user.value = await api.post('/users/login', loginForm)
    localStorage.setItem('wms-user', JSON.stringify(user.value))
    localStorage.removeItem('wms-warehouse-id')
    selectedWarehouseId.value = null
    await Promise.all([loadWarehouses(), loadProducts()])
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.login = false
  }
}
function logout() {
  localStorage.removeItem('wms-user')
  localStorage.removeItem('wms-warehouse-id')
  user.value = null
  selectedWarehouseId.value = null
  openHome(true)
  nextTick(() => loginUserRef.value?.focus())
}
async function bootstrap() {
  if (!selectedWarehouseId.value) return
  await Promise.all([loadProducts(), loadAllLocations(), loadStocks()])
  dashboard.value = await api.get(`/dashboard?warehouseId=${selectedWarehouseId.value}`).catch(() => ({}))
  await Promise.all([loadInboundOrders().catch(() => {}), loadOutboundOrders().catch(() => {})])
}
async function selectWarehouse(warehouseId) {
  selectedWarehouseId.value = warehouseId
  localStorage.setItem('wms-warehouse-id', String(warehouseId))
  applyWarehouseDefaults()
  openHome(true)
  await bootstrap()
}
async function switchWarehouse() {
  localStorage.setItem('wms-warehouse-id', String(selectedWarehouseId.value))
  applyWarehouseDefaults()
  clearWarehouseData()
  openHome(true)
  await bootstrap()
}
function applyWarehouseDefaults() {
  shelfForm.warehouseId = selectedWarehouseId.value
  locationFilters.warehouseId = selectedWarehouseId.value
  distributionWarehouseId.value = selectedWarehouseId.value
  orderForm.items.forEach(line => {
    line.warehouseId = selectedWarehouseId.value
    line.locationId = null
  })
}
function clearWarehouseData() {
  locations.value = []
  stocks.value = []
  movements.value = []
  inboundOrders.value = []
  outboundOrders.value = []
  shippingJobs.value = []
  selectedShippingJob.value = null
  shippingOrdersToAdd.value = []
  orderSearchResults.value = []
  dashboard.value = {}
  inboundOrder.value = null
  inboundOrderDetail.value = null
  outboundOrderDetail.value = null
  selectedDistributionLocation.value = null
  selectedDistributionShelfCode.value = ''
}
function toggleModule(key) {
  activeModule.value = activeModule.value === key ? '' : key
}
function moduleForView(key) {
  if (key === 'inbound-detail') return modules.find(m => m.key === 'inbound')
  if (key === 'outbound-detail') return modules.find(m => m.key === 'outbound')
  return modules.find(m => m.subs.some(sub => sub.key === key))
}
function routeForView(key, entityKey = '') {
  const owner = moduleForView(key)
  if (!owner) return '/'
  const suffix = ['receiving', 'inbound-detail', 'outbound-detail'].includes(key) && entityKey ? `/${encodeURIComponent(entityKey)}` : ''
  return `/modules/${owner.key}/${key}${suffix}`
}
function viewFromRoute() {
  const parts = window.location.pathname.split('/').filter(Boolean)
  if (parts[0] !== 'modules' || parts.length < 3) return { key: 'home', orderNo: '' }
  const owner = modules.find(m => m.key === parts[1])
  const key = owner?.subs.some(sub => sub.key === parts[2]) || ['inbound-detail', 'outbound-detail'].includes(parts[2]) ? parts[2] : 'home'
  return { key, entityKey: ['receiving', 'inbound-detail', 'outbound-detail'].includes(key) && parts[3] ? decodeURIComponent(parts[3]) : '' }
}
async function loadView(key) {
  if (!user.value || !selectedWarehouseId.value) return
  if (key === 'order-search') await searchOrders()
  if (key === 'inbound-orders') await loadInboundOrders()
  if (key === 'outbound-orders') await loadOutboundOrders()
  if (key === 'picking') await loadOutboundOrders()
  if (key === 'shipping-jobs') await Promise.all([loadOutboundOrders(), loadShippingJobs()])
  if (key === 'location-query') await loadLocations()
  if (key === 'stock-query') await loadStocks()
  if (key === 'stock-flow') await loadMovements()
  if (key === 'stock-distribution') await loadStockDistribution()
  if (key === 'inbound-detail' && activeDetailOrderNo.value) await loadOrderDetail(activeDetailOrderNo.value)
  if (key === 'outbound-detail' && activeOutboundOrderId.value) await loadOutboundOrderDetail(activeOutboundOrderId.value)
  if (key === 'receiving') {
    if (activeReceiveOrderNo.value) await loadReceiveOrder(activeReceiveOrderNo.value)
    else nextTick(() => focusField('orderNo'))
  }
}
async function activateView(key, updateHistory = true, entityKey = '') {
  view.value = key
  activeModule.value = moduleForView(key)?.key || ''
  if (key === 'inbound-create' || key === 'outbound-create') resetOrderForm(key)
  if (key === 'receiving') {
    activeReceiveOrderNo.value = entityKey
    if (!entityKey) clearReceiveState()
  }
  else activeReceiveOrderNo.value = ''
  activeDetailOrderNo.value = key === 'inbound-detail' ? entityKey : ''
  activeOutboundOrderId.value = key === 'outbound-detail' ? Number(entityKey) : null
  if (updateHistory) window.history.pushState({}, '', routeForView(key, entityKey))
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
async function openOrderDetail(orderNo) {
  await activateView('inbound-detail', true, orderNo)
}
async function openOutboundDetail(id) {
  await activateView('outbound-detail', true, id)
}
async function openReceiveByTracking(orderNo, trackingNo) {
  await openReceiveOrder(orderNo)
  if (trackingNo) {
    receiveForm.productCode = trackingNo
    await scanProduct()
  }
}
function openHome(replace = false) {
  view.value = 'home'
  activeModule.value = ''
  activeReceiveOrderNo.value = ''
  activeDetailOrderNo.value = ''
  activeOutboundOrderId.value = null
  window.history[replace ? 'replaceState' : 'pushState']({}, '', '/')
}
async function syncRoute() {
  if (!selectedWarehouseId.value) return
  const route = viewFromRoute()
  await activateView(route.key, false, route.entityKey)
}
async function loadInboundOrders() { inboundOrders.value = await api.get(`/inbound?warehouseId=${selectedWarehouseId.value}`) }
async function loadOrderDetail(orderNo) { inboundOrderDetail.value = await api.get(`/inbound/${encodeURIComponent(orderNo)}`) }
async function loadOutboundOrders() { outboundOrders.value = await api.get(`/outbound-orders?warehouseId=${selectedWarehouseId.value}`) }
async function loadShippingJobs() {
  shippingJobs.value = await api.get(`/shipping-jobs?warehouseId=${selectedWarehouseId.value}`)
  if (selectedShippingJob.value) {
    selectedShippingJob.value = shippingJobs.value.find(job => job.id === selectedShippingJob.value.id) || null
  }
}
function shippingJobStatus(status) {
  if (locale.value === 'en') return { DRAFT: 'Draft', SCHEDULED: 'Scheduled', SHIPPED: 'Shipped', CANCELLED: 'Cancelled' }[status] || status
  return { DRAFT: '\u8349\u7a3f', SCHEDULED: '\u5df2\u6392\u8f66', SHIPPED: '\u5df2\u53d1\u8fd0', CANCELLED: '\u5df2\u53d6\u6d88' }[status] || status
}
async function createShippingJob() {
  if (loading.shippingJob) return
  loading.shippingJob = true
  try {
    const createdBy = user.value?.displayName || user.value?.username || ''
    const job = await api.post('/shipping-jobs', {
      warehouseId: selectedWarehouseId.value,
      plannedShipDate: shippingJobForm.plannedShipDate,
      truckNo: shippingJobForm.truckNo,
      driverName: shippingJobForm.driverName,
      driverPhone: shippingJobForm.driverPhone,
      remark: shippingJobForm.remark,
      createdBy,
      outboundOrderIds: shippingJobForm.outboundOrderIds.map(Number)
    })
    shippingJobForm.outboundOrderIds = []
    shippingJobForm.truckNo = ''
    shippingJobForm.driverName = ''
    shippingJobForm.driverPhone = ''
    shippingJobForm.remark = ''
    await loadShippingJobs()
    selectedShippingJob.value = shippingJobs.value.find(item => item.id === job.id) || job
    showToast(locale.value === 'en' ? 'Shipping job created' : 'Shipping Job \u5df2\u521b\u5efa')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shippingJob = false
  }
}
async function runShippingJobAction(action) {
  if (!selectedShippingJob.value || loading.shippingJob) return
  loading.shippingJob = true
  try {
    selectedShippingJob.value = await api.post(`/shipping-jobs/${selectedShippingJob.value.id}/${action}`)
    await loadShippingJobs()
    showToast(locale.value === 'en' ? 'Shipping job updated' : 'Shipping Job \u72b6\u6001\u5df2\u66f4\u65b0')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shippingJob = false
  }
}
async function removeShippingOrder(orderId) {
  if (!selectedShippingJob.value || loading.shippingJob) return
  loading.shippingJob = true
  try {
    selectedShippingJob.value = await api.del(`/shipping-jobs/${selectedShippingJob.value.id}/orders/${orderId}`)
    await loadShippingJobs()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shippingJob = false
  }
}
async function addShippingOrders() {
  if (!selectedShippingJob.value || !shippingOrdersToAdd.value.length || loading.shippingJob) return
  loading.shippingJob = true
  try {
    selectedShippingJob.value = await api.post(`/shipping-jobs/${selectedShippingJob.value.id}/orders`, {
      outboundOrderIds: shippingOrdersToAdd.value.map(Number)
    })
    shippingOrdersToAdd.value = []
    await loadShippingJobs()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.shippingJob = false
  }
}
async function loadOutboundOrderDetail(id) {
  outboundOrderDetail.value = await api.get(`/outbound-orders/${id}`)
  initializePickingQuantities()
}
function initializePickingQuantities() {
  Object.keys(pickingQuantities).forEach(key => delete pickingQuantities[key])
  outboundOrderDetail.value?.items.forEach(item => {
    pickingQuantities[item.itemId] = item.pickedQuantity || 0
  })
}
async function confirmInboundOrder() {
  if (!inboundOrderDetail.value || loading.inboundAction) return
  loading.inboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    inboundOrderDetail.value = await api.post(`/inbound/${encodeURIComponent(inboundOrderDetail.value.orderNo)}/confirm?operatorName=${encodeURIComponent(operator)}`)
    showToast(locale.value === 'en' ? 'Inbound order completed' : '\u5165\u5e93\u8ba2\u5355\u5df2\u5b8c\u6210')
    await loadInboundOrders()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.inboundAction = false
  }
}
async function cancelInboundOrder() {
  if (!inboundOrderDetail.value || loading.inboundAction) return
  loading.inboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    inboundOrderDetail.value = await api.post(`/inbound/${encodeURIComponent(inboundOrderDetail.value.orderNo)}/cancel?operatorName=${encodeURIComponent(operator)}`)
    showToast(locale.value === 'en' ? 'Inbound order cancelled' : '\u5165\u5e93\u8ba2\u5355\u5df2\u53d6\u6d88')
    await loadInboundOrders()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.inboundAction = false
  }
}
async function generatePickList() {
  await runOutboundAction('generate-pick-list', locale.value === 'en' ? 'Inventory allocated' : '\u5e93\u5b58\u5360\u7528\u6210\u529f')
  if (outboundOrderDetail.value?.status === 'NOT_ENOUGH_INV') {
    showToast(locale.value === 'en' ? 'Inventory shortage recorded in order history' : '\u5e93\u5b58\u4e0d\u8db3\uff0c\u7f3a\u8d27\u660e\u7ec6\u5df2\u8bb0\u5f55\u5230\u8ba2\u5355\u5386\u53f2', 'error')
  }
}
async function assignOutboundPicking() {
  let continueOnShortage = false
  if (outboundOrderDetail.value?.status === 'NOT_ENOUGH_INV') {
    continueOnShortage = window.confirm(locale.value === 'en'
      ? 'Inventory is insufficient. Continue with available inventory and create a Back Outbound Order for the shortage?'
      : '\u5f53\u524d\u5e93\u5b58\u4e0d\u8db3\u3002\u662f\u5426\u7ee7\u7eed\u5206\u914d\u62e3\u8d27\uff0c\u5f53\u524d\u8ba2\u5355\u5148\u51fa\u53ef\u7528\u5e93\u5b58\uff0c\u7f3a\u8d27\u90e8\u5206\u751f\u6210 Back Outbound Order\uff1f')
    if (!continueOnShortage) return
  }
  await runOutboundAction(
    `assign-picking?continueOnShortage=${continueOnShortage}`,
    locale.value === 'en' ? 'Picking assigned' : '\u62e3\u8d27\u4efb\u52a1\u5df2\u5206\u914d',
    true
  )
  if (continueOnShortage && outboundOrderDetail.value?.backOrderNo) {
    const prefix = outboundOrderDetail.value.status === 'CANCELLED'
      ? (locale.value === 'en' ? 'No inventory was available; the full order was moved to' : '\u65e0\u53ef\u7528\u5e93\u5b58\uff0c\u5168\u90e8\u7f3a\u53e3\u5df2\u8f6c\u5165')
      : (locale.value === 'en' ? 'Back outbound order created' : '\u5df2\u751f\u6210\u56de\u8865\u51fa\u5e93\u5355')
    showToast(`${prefix}: ${outboundOrderDetail.value.backOrderNo}`)
  }
}
async function runOutboundAction(action, successMessage, hasQuery = false) {
  if (!outboundOrderDetail.value || loading.outboundAction) return
  loading.outboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    const separator = hasQuery ? '&' : '?'
    outboundOrderDetail.value = await api.post(`/outbound-orders/${outboundOrderDetail.value.id}/${action}${separator}operatorName=${encodeURIComponent(operator)}`)
    initializePickingQuantities()
    showToast(successMessage)
    await Promise.all([loadOutboundOrders(), loadStocks()])
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.outboundAction = false
  }
}
function searchPickingOrder() {
  if (filteredPickingOrders.value.length === 1) enterPickingOrder(filteredPickingOrders.value[0])
}
async function enterPickingOrder(order) {
  await openOutboundDetail(order.id)
  if (outboundOrderDetail.value?.status === 'READY_TO_PICK') await startOutboundPicking()
}
function markItemPicked(item) {
  pickingQuantities[item.itemId] = item.quantity
}
function pickingItemClass(item) {
  const picked = Number(pickingQuantities[item.itemId] || 0)
  if (picked >= Number(item.quantity)) return 'picked'
  if (picked > 0) return 'partial'
  return 'pending'
}
function pickingStatusName(item) {
  const status = pickingItemClass(item)
  if (locale.value === 'en') return { pending: 'Pending', partial: 'Partial', picked: 'Picked' }[status]
  return { pending: '\u5f85\u62e3\u8d27', partial: '\u90e8\u5206\u62e3\u8d27', picked: '\u5df2\u62e3\u8d27' }[status]
}
async function startOutboundPicking() {
  if (!outboundOrderDetail.value || loading.outboundAction) return
  loading.outboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    outboundOrderDetail.value = await api.post(`/outbound-orders/${outboundOrderDetail.value.id}/picking/start?operatorName=${encodeURIComponent(operator)}`)
    initializePickingQuantities()
    showToast('\u5df2\u5f00\u59cb\u62e3\u8d27')
    await loadOutboundOrders()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.outboundAction = false
  }
}
async function completeOutboundPicking() {
  if (!outboundOrderDetail.value || loading.outboundAction) return
  const invalid = outboundOrderDetail.value.items.some(item => Number(pickingQuantities[item.itemId]) !== Number(item.quantity))
  if (invalid) return showToast('\u5b9e\u9645\u62e3\u8d27\u6570\u91cf\u5fc5\u987b\u4e0e\u5e94\u62e3\u6570\u91cf\u4e00\u81f4', 'error')
  loading.outboundAction = true
  try {
    const operatorName = user.value?.displayName || user.value?.username || ''
    outboundOrderDetail.value = await api.post(`/outbound-orders/${outboundOrderDetail.value.id}/picking/complete`, {
      operatorName,
      items: outboundOrderDetail.value.items.map(item => ({
        itemId: item.itemId,
        pickedQuantity: Number(pickingQuantities[item.itemId])
      }))
    })
    showToast('\u62e3\u8d27\u5df2\u5b8c\u6210\uff0c\u53ef\u786e\u8ba4\u51fa\u5e93')
    await loadOutboundOrders()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.outboundAction = false
  }
}
async function confirmOutboundOrder() {
  if (!outboundOrderDetail.value || loading.outboundAction) return
  loading.outboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    outboundOrderDetail.value = await api.post(`/outbound-orders/${outboundOrderDetail.value.id}/confirm?operatorName=${encodeURIComponent(operator)}`)
    showToast('\u51fa\u5e93\u786e\u8ba4\u6210\u529f')
    await Promise.all([loadOutboundOrders(), loadStocks(), loadMovements()])
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.outboundAction = false
  }
}
async function cancelOutboundOrder() {
  if (!outboundOrderDetail.value || loading.outboundAction) return
  loading.outboundAction = true
  try {
    const operator = user.value?.displayName || user.value?.username || ''
    outboundOrderDetail.value = await api.post(`/outbound-orders/${outboundOrderDetail.value.id}/cancel?operatorName=${encodeURIComponent(operator)}`)
    showToast('\u51fa\u5e93\u8ba2\u5355\u5df2\u53d6\u6d88')
    await loadOutboundOrders()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.outboundAction = false
  }
}
async function searchOrders() {
  if (orderSearchFilters.createdFrom && orderSearchFilters.createdTo && orderSearchFilters.createdFrom > orderSearchFilters.createdTo) {
    return showToast('\u521b\u5efa\u8d77\u59cb\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u7ed3\u675f\u65e5\u671f', 'error')
  }
  loading.orderSearch = true
  try {
    const query = new URLSearchParams()
    Object.entries(orderSearchFilters).forEach(([key, value]) => {
      if (value) query.set(key, value)
    })
    query.set('warehouseId', selectedWarehouseId.value)
    orderSearchResults.value = await api.get(`/orders/search?${query.toString()}`)
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.orderSearch = false
  }
}
async function resetOrderSearch() {
  Object.assign(orderSearchFilters, { orderNo: '', direction: '', status: '', operatorName: '', createdFrom: '', createdTo: '' })
  await searchOrders()
}
async function loadWarehouses() {
  warehouses.value = (await api.get('/warehouses')).filter(warehouse => /^[A-Z]{5}$/.test(warehouse.code || ''))
  if (selectedWarehouseId.value && !warehouses.value.some(warehouse => warehouse.id === selectedWarehouseId.value)) {
    selectedWarehouseId.value = null
    localStorage.removeItem('wms-warehouse-id')
  }
}
async function loadProducts() { products.value = await api.get('/products') }
async function loadAllLocations() { locations.value = await api.get(`/locations?warehouseId=${selectedWarehouseId.value}`) }
async function loadStocks() { stocks.value = await api.get(`/stocks?warehouseId=${selectedWarehouseId.value}`) }
async function loadMovements() { movements.value = await api.get(`/movements?warehouseId=${selectedWarehouseId.value}`) }
async function loadStockDistribution() {
  await Promise.all([loadAllLocations(), loadStocks()])
  if (!distributionWarehouseId.value || !warehouses.value.some(warehouse => warehouse.id === distributionWarehouseId.value)) {
    distributionWarehouseId.value = selectedWarehouseId.value
  }
  selectedDistributionLocation.value = null
  selectedDistributionShelfCode.value = ''
}
function parseLocationPosition(location, index = 0) {
  const match = String(location.code || '').match(/-(\d+)-(\d+)-(\d+)$/)
  return match
    ? { x: Number(match[1]), y: Number(match[2]), z: Number(match[3]) }
    : { x: (index % 4) + 1, y: Math.floor(index / 8) + 1, z: (Math.floor(index / 4) % 2) + 1 }
}
function compareLocationPosition(first, second) {
  const a = parseLocationPosition(first)
  const b = parseLocationPosition(second)
  return a.x - b.x || a.y - b.y || a.z - b.z
}
function locationStocks(code) { return stockByLocation.value[code] || [] }
function locationStockTotal(code) { return locationStocks(code).reduce((total, stock) => total + Number(stock.quantity || 0), 0) }
function locationUtilization(location) {
  const capacity = Number(location.capacity || 0)
  if (!capacity) return 0
  return Math.min(100, Math.round(locationStockTotal(location.code) / capacity * 100))
}
function distributionLocationClass(location) {
  if (location.status === 'DISABLED') return 'disabled'
  const total = locationStockTotal(location.code)
  if (!total) return 'empty'
  if (location.capacity && total >= location.capacity) return 'full'
  return 'occupied'
}
function distributionLocationTitle(location) {
  return `${location.code} · ${locationStockTotal(location.code)} / ${location.capacity || 0}`
}
function shortLocationCode(code) {
  const parts = String(code).split('-')
  return parts.length > 3 ? parts.slice(-3).join('-') : code
}
function openDistributionShelf(shelf) {
  selectedDistributionShelfCode.value = shelf.code
  selectedDistributionLocation.value = null
}
function backToDistributionOverview() {
  selectedDistributionShelfCode.value = ''
  selectedDistributionLocation.value = null
}
function newOrderLine() {
  return { key: `${Date.now()}-${Math.random()}`, productId: null, productKeyword: '', showProductOptions: false, warehouseId: selectedWarehouseId.value, locationId: null, quantity: 1 }
}
function resetOrderForm(targetView = view.value) {
  orderForm.type = targetView === 'inbound-create' ? 'PURCHASE' : 'SALE'
  orderForm.operatorName = user.value?.displayName || user.value?.username || ''
  orderForm.receiverName = ''
  orderForm.receiverPhone = ''
  orderForm.address = ''
  orderForm.reason = ''
  orderForm.trackingNo = ''
  orderForm.remark = ''
  orderForm.items.splice(0, orderForm.items.length, newOrderLine())
}
function addOrderLine() { orderForm.items.push(newOrderLine()) }
function removeOrderLine(index) { if (orderForm.items.length > 1) orderForm.items.splice(index, 1) }
function productMatches(line) {
  const keyword = line.productKeyword.trim().toLowerCase()
  if (!keyword) return []
  return products.value.filter(product =>
    [product.sku, product.barcode, product.name].some(value => String(value || '').toLowerCase().includes(keyword))
  ).slice(0, 8)
}
function handleProductKeyword(line) {
  line.showProductOptions = true
  const selected = products.value.find(product => product.id === line.productId)
  if (selected && line.productKeyword !== selected.sku) line.productId = null
}
function selectLineProduct(line, product) {
  line.productId = product.id
  line.productKeyword = product.sku
  line.showProductOptions = false
}
function selectFirstProduct(line) {
  const first = productMatches(line)[0]
  if (first) selectLineProduct(line, first)
}
function selectedProductText(line) {
  const product = products.value.find(item => item.id === line.productId)
  return product ? `${product.name} / ${product.modelSpec || '-'} / ${product.unitName || '-'}` : ''
}
function locationsForWarehouse(warehouseId) {
  return locations.value.filter(l => l.warehouseId === warehouseId && l.status !== 'DISABLED')
}
async function createOrder() {
  const invalid = orderForm.items.some(line => !line.productId || !line.warehouseId || !line.locationId || !line.quantity || line.quantity < 1)
  if (invalid) return showToast('\u8bf7\u5b8c\u6574\u586b\u5199\u6240\u6709\u8ba2\u5355\u660e\u7ec6', 'error')
  if (!isInboundCreate.value && (!orderForm.receiverName || !orderForm.address || !orderForm.reason)) {
    return showToast('\u8bf7\u586b\u5199\u6536\u8d27\u4eba\u3001\u6536\u8d27\u5730\u5740\u548c\u51fa\u5e93\u539f\u56e0', 'error')
  }
  loading.order = true
  try {
    const path = isInboundCreate.value ? '/inbound' : '/outbound-orders'
    const payload = {
      type: orderForm.type,
      operatorName: orderForm.operatorName,
      remark: orderForm.remark,
      items: orderForm.items.map(({ productId, warehouseId, locationId, quantity }) => ({ productId, warehouseId, locationId, quantity: Number(quantity) }))
    }
    if (!isInboundCreate.value) {
      Object.assign(payload, {
        receiverName: orderForm.receiverName,
        receiverPhone: orderForm.receiverPhone,
        address: orderForm.address,
        reason: orderForm.reason,
        trackingNo: orderForm.trackingNo
      })
    }
    const order = await api.post(path, payload)
    showToast(`\u8ba2\u5355 ${order.orderNo} \u521b\u5efa\u6210\u529f`)
    if (isInboundCreate.value) await openOrderDetail(order.orderNo)
    else {
      await loadOutboundOrders()
      await openOutboundDetail(order.id)
    }
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
  clearScanTimer(field)
  if (loading.receive) return
  const value = receiveForm[field]
  if (handleScanInput(value)) return
  if (field === 'orderNo') return loadInboundOrder()
  if (field === 'locationCode') return scanLocation()
  if (field === 'productCode') return scanProduct()
  if (field === 'quantity') return confirmReceive()
}
function scheduleScan(field) {
  clearScanTimer(field)
  if (!receiveForm[field]) return
  scanTimers[field] = window.setTimeout(() => handleEnter(field), scanDelay(field))
}
function clearScanTimer(field) {
  window.clearTimeout(scanTimers[field])
  scanTimers[field] = null
}
function scanDelay(field) {
  return field === 'orderNo' ? 450 : 320
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
  if (loading.receive) return
  if (!receiveForm.orderNo) {
    errorField.value = 'orderNo'
    return showToast('\u8bf7\u5148\u8f93\u5165\u5165\u5e93\u5355\u53f7', 'error')
  }
  if (inboundOrder.value?.orderNo === receiveForm.orderNo) return
  await runScan('orderNo', async () => {
    const order = await api.get(`/inbound/${encodeURIComponent(receiveForm.orderNo)}/receiving`)
    inboundOrder.value = order
    activeReceiveOrderNo.value = order.orderNo
    window.history.pushState({}, '', routeForView('receiving', order.orderNo))
    showToast('\u5165\u5e93\u5355\u8bc6\u522b\u6210\u529f')
    focusField('locationCode')
  })
}
async function loadReceiveOrder(orderNo) {
  try {
    receiveForm.orderNo = orderNo
    inboundOrder.value = await api.get(`/inbound/${encodeURIComponent(orderNo)}/receiving`)
    activeReceiveOrderNo.value = inboundOrder.value.orderNo
    nextTick(() => focusField('locationCode'))
  } catch (e) {
    showToast(e.message, 'error')
    clearReceiveState()
    window.history.replaceState({}, '', routeForView('receiving'))
    nextTick(() => focusField('orderNo'))
  }
}
async function scanLocation() {
  if (loading.receive || !receiveForm.locationCode || scanResult.location?.code === receiveForm.locationCode) return
  await runScan('locationCode', async () => {
    scanResult.location = await api.get(`/scan/location/${encodeURIComponent(receiveForm.locationCode)}`)
    showToast('\u8d27\u4f4d\u8bc6\u522b\u6210\u529f')
    focusNext('locationCode')
  })
}
async function scanProduct() {
  if (loading.receive || !receiveForm.productCode || scanResult.product?.sku === receiveForm.productCode || scanResult.product?.barcode === receiveForm.productCode) return
  await runScan('productCode', async () => {
    scanResult.product = await api.get(`/scan/inbound-product?orderNo=${encodeURIComponent(receiveForm.orderNo)}&code=${encodeURIComponent(receiveForm.productCode)}`)
    receiveForm.quantity = qtyMode.value === 'fixed' ? 1 : null
    showToast('\u5546\u54c1\u8bc6\u522b\u6210\u529f')
    if (qtyMode.value === 'fixed') await confirmReceive()
    else focusField('quantity')
  })
}
async function confirmReceive() {
  if (loading.receiveSubmit) return
  if (!receiveForm.quantity || Number(receiveForm.quantity) < 1) {
    errorField.value = 'quantity'
    showToast('\u8bf7\u8f93\u5165\u6709\u6548\u7684\u6536\u8d27\u6570\u91cf', 'error')
    return selectField('quantity')
  }
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
async function selectTracking(item) {
  receiveForm.productCode = item.trackingNo
  await scanProduct()
}
function clearReceiveState() {
  Object.assign(receiveForm, { orderNo: '', locationCode: '', productCode: '', quantity: 1 })
  Object.assign(scanResult, { location: null, product: null })
  inboundOrder.value = null
  activeReceiveOrderNo.value = ''
  qtyMode.value = 'fixed'
}
function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}
function historyName(type) {
  if (locale.value === 'en') return { CREATE: 'Order Created', ALLOCATED: 'Inventory Allocated', SHORTAGE: 'Inventory Shortage', BACK_ORDER: 'Back Order Created', ASSIGNED: 'Picking Assigned', PICKING: 'Picking Started', PICKED: 'Picking Completed', INBOUND: 'Received into Stock', OUTBOUND: 'Outbound Confirmed', COMPLETE: 'Order Completed', CANCEL: 'Order Cancelled' }[type] || type
  return { CREATE: '\u521b\u5efa\u8ba2\u5355', SHORTAGE: '\u5e93\u5b58\u4e0d\u8db3', BACK_ORDER: '\u751f\u6210\u56de\u8865\u51fa\u5e93\u5355', PICKING: '\u5f00\u59cb\u62e3\u8d27', PICKED: '\u62e3\u8d27\u5b8c\u6210', INBOUND: '\u6536\u8d27\u5165\u5e93', OUTBOUND: '\u786e\u8ba4\u51fa\u5e93', COMPLETE: '\u5b8c\u6210\u8ba2\u5355', CANCEL: '\u53d6\u6d88\u8ba2\u5355' }[type] || type
}
function focusField(field) { nextTick(() => refs[field]?.value?.focus()) }
function focusNext(field) {
  const order = ['orderNo', 'locationCode', 'productCode', 'quantity']
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
  if (locale.value === 'en') return { SYSTEM_ADMIN: 'System Administrator', WAREHOUSE_MANAGER: 'Warehouse Manager', PURCHASING_OPERATIONS: 'Purchasing / Operations' }[role] || role
  return { SYSTEM_ADMIN: '\u7cfb\u7edf\u7ba1\u7406\u5458', WAREHOUSE_MANAGER: '\u4ed3\u5e93\u7ba1\u7406\u5458', PURCHASING_OPERATIONS: '\u91c7\u8d2d/\u8fd0\u8425\u4eba\u5458' }[role] || role
}
function locationStatus(l) {
  if (l.status === 'DISABLED') return labels.disabled
  if (l.status === 'FULL') return labels.full
  return (l.occupied || 0) > 0 ? '\u4f7f\u7528\u4e2d' : '\u7a7a\u95f2'
}
function orderStatus(status) {
  const map = locale.value === 'en'
    ? { CREATED: 'In Queue', IN_QUEUE: 'In Queue', RECEIVING: 'Receiving', RECEIVED: 'Received', ALLOCATED: 'Allocated', NOT_ENOUGH_INV: 'Not Enough Inv', READY_TO_PICK: 'Ready to Pick', PICKING: 'Picking', PICKED: 'Picked', COMPLETED: 'Completed', CANCELLED: 'Cancelled' }
    : { CREATED: 'In Queue', IN_QUEUE: 'In Queue', RECEIVING: 'Receiving', RECEIVED: 'Received', ALLOCATED: 'Allocated', NOT_ENOUGH_INV: 'Not Enough Inv', READY_TO_PICK: 'Ready to Pick', PICKING: '\u62e3\u8d27\u4e2d', PICKED: '\u62e3\u8d27\u5b8c\u6210', COMPLETED: '\u5df2\u5b8c\u6210', CANCELLED: '\u5df2\u53d6\u6d88' }
  return map[status] || status
}
function createAiSessionId() {
  return globalThis.crypto?.randomUUID?.() || `wms-ai-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
function loadAiPosition() {
  try {
    const position = JSON.parse(localStorage.getItem('wms-ai-position') || 'null')
    return Number.isFinite(position?.x) && Number.isFinite(position?.y) ? position : null
  } catch {
    return null
  }
}
function openAiAssistant() {
  aiChat.open = true
  nextTick(clampAiPosition)
}
function startAiDrag(event) {
  if (window.innerWidth <= 800 || event.button !== 0) return
  const rect = aiAssistantRef.value?.getBoundingClientRect()
  if (!rect) return
  aiPosition.x = rect.left
  aiPosition.y = rect.top
  aiDrag.active = true
  aiDrag.offsetX = event.clientX - rect.left
  aiDrag.offsetY = event.clientY - rect.top
  event.preventDefault()
}
function moveAiDrag(event) {
  if (!aiDrag.active) return
  const element = aiAssistantRef.value
  if (!element) return
  const margin = 8
  aiPosition.x = Math.min(
    Math.max(margin, event.clientX - aiDrag.offsetX),
    Math.max(margin, window.innerWidth - element.offsetWidth - margin)
  )
  aiPosition.y = Math.min(
    Math.max(margin, event.clientY - aiDrag.offsetY),
    Math.max(margin, window.innerHeight - element.offsetHeight - margin)
  )
}
function stopAiDrag() {
  if (!aiDrag.active) return
  aiDrag.active = false
  localStorage.setItem('wms-ai-position', JSON.stringify({
    x: Math.round(aiPosition.x),
    y: Math.round(aiPosition.y)
  }))
}
function clampAiPosition() {
  if (!aiChat.open || aiPosition.x == null || aiPosition.y == null) return
  const element = aiAssistantRef.value
  if (!element) return
  const margin = 8
  aiPosition.x = Math.min(Math.max(margin, aiPosition.x), Math.max(margin, window.innerWidth - element.offsetWidth - margin))
  aiPosition.y = Math.min(Math.max(margin, aiPosition.y), Math.max(margin, window.innerHeight - element.offsetHeight - margin))
}
function agentName(agent) {
  if (locale.value === 'en') return { rules: 'Rules Agent', analytics: 'Data Agent', report: 'Report Agent' }[agent] || 'AI'
  return { rules: '规则助手', analytics: '数据分析助手', report: '报表助手' }[agent] || 'AI 助手'
}
function askAiSuggestion(text) {
  aiChat.input = text
  sendAiMessage()
}
async function sendAiMessage() {
  const message = aiChat.input.trim()
  if (!message || aiChat.loading) return
  aiChat.messages.push({ role: 'user', content: message })
  aiChat.input = ''
  aiChat.loading = true
  await nextTick()
  aiMessagesRef.value?.scrollTo({ top: aiMessagesRef.value.scrollHeight, behavior: 'smooth' })
  try {
    const response = await api.post('/ai/assistant/chat', {
      sessionId: aiChat.sessionId,
      message,
      warehouseId: selectedWarehouseId.value,
      locale: locale.value
    })
    aiChat.messages.push({
      role: 'assistant',
      agent: response.agent,
      content: response.answer,
      reportId: response.reportId
    })
  } catch (e) {
    aiChat.messages.push({
      role: 'assistant',
      agent: 'supervisor',
      content: `${locale.value === 'en' ? 'Request failed' : '请求失败'}：${e.message}`
    })
  } finally {
    aiChat.loading = false
    await nextTick()
    aiMessagesRef.value?.scrollTo({ top: aiMessagesRef.value.scrollHeight, behavior: 'smooth' })
  }
}
function outboundStatus(status) {
  return orderStatus(status)
}
function directionName(direction) {
  return { INBOUND: labels.inbound, OUTBOUND: labels.outbound }[direction] || direction
}
function receiveStatusName(status) {
  if (locale.value === 'en') return { NOT_RECEIVED: 'Not Received', PARTIAL: 'Partially Received', DONE: 'Done', OVER: 'Over Received' }[status] || status
  return { NOT_RECEIVED: '\u672a\u6536\u8d27', PARTIAL: '\u90e8\u5206\u6536\u8d27', DONE: '\u5df2\u5b8c\u6210', OVER: '\u8d85\u51fa' }[status] || status
}
function stockStatusName(status) {
  if (locale.value === 'en') return { NORMAL: 'Normal', LOW: 'Low Stock', OVERSTOCK: 'Overstock' }[status] || status
  return { NORMAL: '\u6b63\u5e38', LOW: '\u5e93\u5b58\u4e0d\u8db3', OVERSTOCK: '\u5e93\u5b58\u79ef\u538b' }[status] || status
}
function movementName(type) {
  if (locale.value === 'en') return { INBOUND: 'Inbound', OUTBOUND: 'Outbound', CHECK_GAIN: 'Check Gain', CHECK_LOSS: 'Check Loss' }[type] || type
  return { INBOUND: '\u5165\u5e93', OUTBOUND: '\u51fa\u5e93', CHECK_GAIN: '\u76d8\u76c8', CHECK_LOSS: '\u76d8\u4e8f' }[type] || type
}
function typeName(type) {
  if (locale.value === 'en') {
    return {
      PURCHASE: 'Purchase Inbound', RETURN: 'Return Inbound', TRANSFER: 'Transfer',
      INVENTORY_GAIN: 'Inventory Gain', SALE: 'Sales Outbound', REQUISITION: 'Requisition Outbound',
      DAMAGE: 'Damage Outbound', INVENTORY_LOSS: 'Inventory Loss'
    }[type] || type
  }
  return {
    PURCHASE: '\u91c7\u8d2d\u5165\u5e93', RETURN: '\u9000\u8d27\u5165\u5e93', TRANSFER: '\u8c03\u62e8',
    INVENTORY_GAIN: '\u76d8\u76c8\u5165\u5e93', SALE: '\u9500\u552e\u51fa\u5e93', REQUISITION: '\u9886\u7528\u51fa\u5e93',
    DAMAGE: '\u62a5\u635f\u51fa\u5e93', INVENTORY_LOSS: '\u76d8\u4e8f\u51fa\u5e93'
  }[type] || type
}

onMounted(async () => {
  switchLanguage()
  window.addEventListener('pointermove', moveAiDrag)
  window.addEventListener('pointerup', stopAiDrag)
  window.addEventListener('pointercancel', stopAiDrag)
  window.addEventListener('resize', clampAiPosition)
  window.addEventListener('popstate', syncRoute)
  if (user.value) {
    await Promise.all([loadWarehouses(), loadProducts()])
    if (selectedWarehouseId.value) {
      applyWarehouseDefaults()
      await bootstrap()
      await syncRoute()
    } else openHome(true)
  } else nextTick(() => loginUserRef.value?.focus())
})
onUnmounted(() => {
  window.removeEventListener('pointermove', moveAiDrag)
  window.removeEventListener('pointerup', stopAiDrag)
  window.removeEventListener('pointercancel', stopAiDrag)
  window.removeEventListener('resize', clampAiPosition)
  window.removeEventListener('popstate', syncRoute)
})
</script>
