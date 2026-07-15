<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { onValue, ref as dbRef } from 'firebase/database'
import { db } from '../firebase'
import { RouterLink } from 'vue-router'
import HazardMap from '../components/HazardMap.vue'

const users = ref({})
const reports = ref({})
let unsubUsers = null
let unsubReports = null

onMounted(() => {
  unsubUsers = onValue(dbRef(db, 'users'), (snapshot) => {
    users.value = snapshot.val() || {}
  })
  unsubReports = onValue(dbRef(db, 'hazard_reports'), (snapshot) => {
    reports.value = snapshot.val() || {}
  })
})

onUnmounted(() => {
  if (unsubUsers) unsubUsers()
  if (unsubReports) unsubReports()
})

const reportList = computed(() =>
  Object.entries(reports.value)
    .map(([id, r]) => ({ id, ...r, username: users.value[r.uid]?.name || 'Unknown' }))
    .sort((a, b) => (b.timestamp || '').localeCompare(a.timestamp || '')),
)

const totalUsers = computed(() => Object.keys(users.value).length)
const totalReports = computed(() => reportList.value.length)
const openReports = computed(
  () => reportList.value.filter((r) => (r.status || 'New') !== 'Resolved').length,
)
const resolvedReports = computed(
  () => reportList.value.filter((r) => r.status === 'Resolved').length,
)

const recentReports = computed(() => reportList.value.slice(0, 10))

function statusClass(status) {
  if (status === 'Resolved') return 'status-resolved'
  if (status === 'Under Investigation') return 'status-investigating'
  return 'status-new'
}
</script>

<template>
  <div class="dashboard">
    <h1>Dashboard</h1>

    <div class="cards">
      <div class="card stat-card">
        <div class="card-value">{{ totalUsers }}</div>
        <div class="card-label">Total Users</div>
      </div>
      <div class="card stat-card">
        <div class="card-value">{{ totalReports }}</div>
        <div class="card-label">Total Reports</div>
      </div>
      <div class="card stat-card">
        <div class="card-value">{{ openReports }}</div>
        <div class="card-label">Open</div>
      </div>
      <div class="card stat-card">
        <div class="card-value">{{ resolvedReports }}</div>
        <div class="card-label">Resolved</div>
      </div>
    </div>

    <h2>Hazard Map</h2>
    <div class="map-card">
      <HazardMap :reports="reportList" height="380px" />
    </div>

    <h2>Recent Reports</h2>
    <div class="table-wrap">
      <table class="reports-table">
        <thead>
          <tr>
            <th>Type</th>
            <th>Reporter</th>
            <th>Status</th>
            <th>Date/Time</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="recentReports.length === 0">
            <td colspan="4" class="empty">No reports yet.</td>
          </tr>
          <tr v-for="r in recentReports" :key="r.id">
            <td>
              <RouterLink :to="`/reports/${r.id}`">{{ r.type || '—' }}</RouterLink>
            </td>
            <td class="reporter">{{ r.username }}</td>
            <td>
              <span class="status-badge" :class="statusClass(r.status)">{{ r.status || 'New' }}</span>
            </td>
            <td>{{ r.timestamp || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard > * + * {
  margin-top: 2rem;
}

h1,
h2 {
  color: #3D4127;
  font-weight: 700;
  font-size: 1.5rem;
  border-left: 4px solid #636B2F;
  padding-left: 12px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.stat-card {
  text-align: center;
  min-height: 120px;
  padding: 1.5rem;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(61, 65, 39, 0.1);
  border: none;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-card:nth-child(1) {
  border-top: 4px solid #636B2F;
}

.stat-card:nth-child(2) {
  border-top: 4px solid #BAC095;
}

.stat-card:nth-child(3) {
  border-top: 4px solid #E87000;
}

.stat-card:nth-child(4) {
  border-top: 4px solid #3D4127;
}

.card-value {
  font-size: 2.5rem;
  font-weight: 700;
  color: #3D4127;
}

.card-label {
  color: #636B2F;
  margin-top: 0.25rem;
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.map-card {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(61, 65, 39, 0.1);
  border: 1.5px solid #BAC095;
}

.table-wrap {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(61, 65, 39, 0.1);
  border: 1.5px solid #BAC095;
}

.reports-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.reports-table th,
.reports-table td {
  text-align: left;
  padding: 12px 8px;
  border-bottom: 1px solid var(--color-divider);
}

.reports-table th {
  background: #636B2F;
  color: #fff;
  font-weight: 500;
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.reports-table tbody tr:nth-child(odd) {
  background: #fff;
}

.reports-table tbody tr:nth-child(even) {
  background: #f7f9f2;
}

.reports-table tbody tr:hover {
  background: #D4DE95;
  transition: background 0.15s;
}

.reports-table tbody tr:last-child td {
  border-bottom: none;
}

.reports-table a {
  color: var(--color-primary);
}

.reporter {
  color: #E87000;
}

.empty {
  color: var(--color-text-secondary);
  text-align: center;
}
</style>
