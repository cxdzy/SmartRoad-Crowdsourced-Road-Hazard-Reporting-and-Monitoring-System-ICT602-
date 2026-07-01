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
    <div class="card map-card">
      <HazardMap :reports="reportList" height="320px" />
    </div>

    <h2>Recent Reports</h2>
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
          <td>{{ r.username }}</td>
          <td>
            <span class="status-badge" :class="statusClass(r.status)">{{ r.status || 'New' }}</span>
          </td>
          <td>{{ r.timestamp || '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin: 1.5rem 0;
}

.stat-card {
  text-align: center;
}

.map-card {
  margin-bottom: 1.5rem;
}

.card-value {
  font-size: 2rem;
  font-weight: 600;
  color: var(--color-primary-dark);
}

.card-label {
  color: var(--color-text-secondary);
  margin-top: 0.25rem;
  font-size: 0.9rem;
}

.reports-table {
  width: 100%;
  border-collapse: collapse;
}

.reports-table th,
.reports-table td {
  text-align: left;
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid var(--color-divider);
}

.reports-table th {
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 0.85rem;
}

.reports-table a {
  color: var(--color-primary);
}

.empty {
  color: var(--color-text-secondary);
  text-align: center;
}
</style>
