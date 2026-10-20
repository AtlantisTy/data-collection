<script setup>
import { onMounted, onBeforeUnmount, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

// ---------- 状态 ----------
const points = ref([])                     // 点位表 [{device,name,unit,...}]
const latest = ref({})                     // metric -> 最新值对象
const wsStatus = ref('未连接')
const nowText = ref('')

let ws = null
const buffers = {}                        // metric -> { times:[], vals:[] }
const MAX = 120

// ---------- 实时曲线 ----------
let realtimeChart = null
function initRealtime() {
  const el = document.getElementById('realtime')
  realtimeChart = echarts.init(el)
  realtimeChart.setOption({
    title: { text: '实时采集曲线', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { top: 28 },
    grid: { left: 60, right: 20, top: 60, bottom: 30 },
    xAxis: { type: 'time', name: '时间' },
    yAxis: { type: 'value', name: '值' },
    series: []
  })
}
function ensureSeries(metric, name) {
  if (!buffers[metric]) buffers[metric] = { times: [], vals: [] }
  if (!realtimeChart) return
  const opt = realtimeChart.getOption()
  const exists = opt.series.some(s => s.id === metric)
  if (!exists) {
    realtimeChart.setOption({
      series: [{ id: metric, name, type: 'line', showSymbol: false, smooth: true, data: [] }]
    })
  }
}
function appendRealtime(rec) {
  const metric = rec.device + '_' + rec.name
  const name = rec.name + (rec.unit ? ` (${rec.unit})` : '')
  ensureSeries(metric, name)
  const b = buffers[metric]
  b.times.push(new Date(rec.timestamp).getTime())
  b.vals.push(rec.value)
  if (b.times.length > MAX) { b.times.shift(); b.vals.shift() }
  realtimeChart && realtimeChart.setOption({
    series: [{ id: metric, data: b.vals.map((v, i) => [b.times[i], v]) }]
  })
}

// ---------- WebSocket ----------
function connect() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.hostname}:8080/ws/live`
  ws = new WebSocket(url)
  ws.onopen = () => { wsStatus.value = '已连接' }
  ws.onclose = () => { wsStatus.value = '未连接'; setTimeout(connect, 3000) }
  ws.onerror = () => { wsStatus.value = '连接异常' }
  ws.onmessage = (e) => {
    try {
      const rec = JSON.parse(e.data)
      latest.value[rec.device + '_' + rec.name] = rec
      appendRealtime(rec)
    } catch (err) { /* ignore */ }
  }
}

// ---------- 历史查询 ----------
const histPoint = ref('')
const histFrom = ref('')
const histTo = ref('')
const histTitle = ref('')
let histChart = null
async function queryHistory() {
  const el = document.getElementById('history')
  histChart = histChart || echarts.init(el)
  const p = points.value.find(p => p.device + '_' + p.name === histPoint.value)
  if (!p) return
  // 默认最近 1 小时
  const from = histFrom.value ? new Date(histFrom.value).toISOString() : new Date(Date.now() - 3600e3).toISOString()
  const to = histTo.value ? new Date(histTo.value).toISOString() : new Date().toISOString()
  const { data } = await axios.get('/api/records', {
    params: { device: p.device, point: p.name, from, to }
  })
  histTitle.value = `${histPoint.value}  历史 (${data.length} 点)`
  const times = data.map(d => new Date(d.time).toLocaleString())
  const vals = data.map(d => d.value)
  histChart.setOption({
    title: { text: histTitle.value, left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value' },
    dataZoom: [{ type: 'inside' }],
    series: [{ name: histPoint.value, type: 'line', showSymbol: false, data: vals }]
  })
}

// ---------- 周期刷新时钟 ----------
let timer = null
onMounted(async () => {
  const { data } = await axios.get('/api/points')
  points.value = data
  data.forEach(p => { latest.value[p.device + '_' + p.name] = null })
  if (data.length) histPoint.value = data[0].device + '_' + data[0].name
  await nextTick(() => {
    initRealtime()
    realtimeChart && realtimeChart.resize()
  })
  connect()
  timer = setInterval(() => { nowText.value = new Date().toLocaleTimeString() }, 1000)
})
onBeforeUnmount(() => {
  ws && ws.close()
  clearInterval(timer)
  realtimeChart && realtimeChart.dispose()
  histChart && histChart.dispose()
})

const metricKeys = () => Object.keys(latest.value)
</script>

<template>
  <div class="wrap">
    <header>
      <h2>工业数据采集看板</h2>
      <span class="status" :class="wsStatus === '已连接' ? 'ok' : 'warn'">实时通道：{{ wsStatus }}</span>
      <span>时钟：{{ nowText }}</span>
    </header>

    <section class="card">
      <h3>实时点位值</h3>
      <table>
        <thead><tr><th>设备</th><th>点位</th><th>值</th><th>单位</th><th>采样时间</th></tr></thead>
        <tbody>
          <tr v-for="k in metricKeys()" :key="k">
            <td>{{ latest[k]?.device }}</td>
            <td>{{ latest[k]?.name }}</td>
            <td class="val">{{ latest[k] ? latest[k].value.toFixed(3) : '—' }}</td>
            <td>{{ latest[k]?.unit }}</td>
            <td>{{ latest[k] ? new Date(latest[k].timestamp).toLocaleTimeString() : '—' }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <div id="realtime" style="height: 340px;"></div>
    </section>

    <section class="card">
      <h3>历史查询</h3>
      <div class="row">
        <label>点位
          <select v-model="histPoint">
            <option v-for="p in points" :key="p.device + '_' + p.name"
                    :value="p.device + '_' + p.name">{{ p.device }}/{{ p.name }}</option>
          </select>
        </label>
        <label>开始 <input type="datetime-local" v-model="histFrom"></label>
        <label>结束 <input type="datetime-local" v-model="histTo"></label>
        <button @click="queryHistory">查询</button>
      </div>
      <div id="history" style="height: 320px;"></div>
    </section>
  </div>
</template>

<style>
* { box-sizing: border-box; }
body { margin: 0; background: #0f172a; color: #e2e8f0; font-family: system-ui, sans-serif; }
.wrap { max-width: 1100px; margin: 0 auto; padding: 20px; }
header { display: flex; align-items: center; gap: 20px; }
header h2 { margin: 0 20px 0 0; }
.status.ok { color: #4ade80; }
.status.warn { color: #fbbf24; }
.card { background: #1e293b; border-radius: 10px; padding: 16px 20px; margin: 16px 0; }
.card h3 { margin: 0 0 12px; }
table { width: 100%; border-collapse: collapse; }
th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #334155; }
.val { font-weight: 600; color: #38bdf8; }
.row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }
.row select, .row input { background: #0f172a; color: #e2e8f0; border: 1px solid #475569; border-radius: 6px; padding: 6px 8px; }
button { background: #2563eb; color: #fff; border: 0; border-radius: 6px; padding: 8px 18px; cursor: pointer; }
</style>