<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { onValue, ref as dbRef, set, increment } from 'firebase/database'
import { db } from '../firebase'
import { RouterLink } from 'vue-router'
import PhotoModal from '../components/PhotoModal.vue'

const STATUS_OPTIONS = ['New', 'Under Investigation', 'Resolved']

const props = defineProps({ id: String })
const lightboxOpen = ref(false)

const report = ref(null)
const notFound = ref(false)
const reporterName = ref('Unknown')
const selectedStatus = ref('New')
const saving = ref(false)
const saveError = ref('')

let unsubReport = null
let unsubUser = null

onMounted(() => {
  unsubReport = onValue(dbRef(db, `hazard_reports/${props.id}`), (snapshot) => {
    const val = snapshot.val()
    if (val === null) {
      notFound.value = true
      report.value = null
      return
    }
    report.value = { id: props.id, ...val }
    selectedStatus.value = val.status || 'New'

    if (unsubUser) unsubUser()
    unsubUser = onValue(dbRef(db, `users/${val.uid}/name`), (userSnap) => {
      reporterName.value = userSnap.val() || 'Unknown'
    })
  })
})

onUnmounted(() => {
  if (unsubReport) unsubReport()
  if (unsubUser) unsubUser()
})

const statusChanged = computed(
  () => report.value && selectedStatus.value !== (report.value.status || 'New'),
)

function statusClass(status) {
  if (status === 'Resolved') return 'status-resolved'
  if (status === 'Under Investigation') return 'status-investigating'
  return 'status-new'
}

async function saveStatus() {
  if (!report.value || !statusChanged.value) return
  saving.value = true
  saveError.value = ''
  try {
    const oldStatus = report.value.status || 'New'
    const newStatus = selectedStatus.value
    await set(dbRef(db, `hazard_reports/${report.value.id}/status`), newStatus)

    const wasResolved = oldStatus === 'Resolved'
    const isResolved = newStatus === 'Resolved'
    if (wasResolved !== isResolved && report.value.uid) {
      await set(
        dbRef(db, `users/${report.value.uid}/resolvedReports`),
        increment(isResolved ? 1 : -1),
      )
    }
  } catch (e) {
    saveError.value = `Failed to save: ${e.message}`
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="details">
    <RouterLink to="/reports" class="back-link">&larr; Back to Manage Reports</RouterLink>

    <div v-if="notFound" class="empty">Report not found.</div>

    <div v-else-if="report" class="card report-card">
      <h1>{{ report.type || '—' }}</h1>

      <button v-if="report.photoUrl" class="photo-btn" @click="lightboxOpen = true">
        <img :src="report.photoUrl" class="photo" alt="Hazard photo" />
      </button>
      <div v-else class="photo photo-placeholder">No photo</div>

      <dl class="fields">
        <dt>Reporter</dt>
        <dd class="reporter">{{ reporterName }}</dd>

        <dt>Description</dt>
        <dd>{{ report.description || '—' }}</dd>

        <dt>GPS Coordinates</dt>
        <dd>{{ report.latitude?.toFixed(6) }}, {{ report.longitude?.toFixed(6) }}</dd>

        <dt>Date/Time</dt>
        <dd>{{ report.timestamp || '—' }}</dd>

        <dt>User-Agent</dt>
        <dd>{{ report.userAgent || '—' }}</dd>

        <dt>Current Status</dt>
        <dd>
          <span class="status-badge" :class="statusClass(report.status)">
            {{ report.status || 'New' }}
          </span>
        </dd>
      </dl>

      <div class="status-form">
        <label for="status-select">Change Status</label>
        <select id="status-select" v-model="selectedStatus">
          <option v-for="s in STATUS_OPTIONS" :key="s" :value="s">{{ s }}</option>
        </select>
        <button class="btn btn-primary" :disabled="!statusChanged || saving" @click="saveStatus">
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </div>
      <p v-if="saveError" class="error">{{ saveError }}</p>
    </div>

    <div v-else class="empty">Loading…</div>

    <PhotoModal
      v-if="lightboxOpen && report?.photoUrl"
      :src="report.photoUrl"
      @close="lightboxOpen = false"
    />
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 1rem;
  color: #636B2F;
  font-weight: 600;
  font-size: 0.9rem;
  text-decoration: none;
  transition: 0.15s;
}

.back-link:hover {
  text-decoration: underline;
}

.report-card {
  max-width: 700px;
  margin: 0 auto;
  border-radius: 12px;
  box-shadow: 0 2px 16px rgba(61, 65, 39, 0.12);
  border: 1.5px solid #BAC095;
  background: #fff;
  padding: 2rem;
}

.report-card h1 {
  color: #3D4127;
  font-weight: 700;
  font-size: 1.4rem;
  border-left: 4px solid #636B2F;
  padding-left: 12px;
  margin-bottom: 1.25rem;
}

.photo-btn {
  display: block;
  width: 100%;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
}

.photo {
  width: 100%;
  max-height: 280px;
  object-fit: cover;
  border-radius: 10px;
  border: 1.5px solid #BAC095;
  margin-bottom: 1.5rem;
}

.photo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-divider);
}

.fields {
  display: grid;
  grid-template-columns: 160px 1fr;
  margin: 1.5rem 0;
}

.fields dt {
  color: #636B2F;
  font-weight: 600;
  font-size: 0.875rem;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0e8;
}

.fields dd {
  margin: 0;
  color: #3D4127;
  font-size: 0.9rem;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0e8;
}

.fields dd.reporter {
  color: #E87000;
}

.status-form {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-top: 1.25rem;
  border-top: 1.5px solid #BAC095;
  margin-top: 1.25rem;
}

.status-form label {
  color: #636B2F;
  font-weight: 600;
  font-size: 0.875rem;
}

.status-form select {
  padding: 8px 12px;
  border: 1.5px solid #BAC095;
  border-radius: 6px;
  background: var(--color-on-primary);
  color: #3D4127;
}

.status-form select:focus {
  outline: none;
  border-color: #636B2F;
}

.status-form .btn {
  background: #636B2F;
  color: #fff;
  font-weight: 700;
  border-radius: 8px;
  padding: 8px 20px;
  border: none;
  transition: background 0.2s;
}

.status-form .btn:hover:not(:disabled) {
  background: #4a5022;
}

.error {
  color: var(--color-danger);
  margin-top: 0.75rem;
}

.empty {
  color: var(--color-text-secondary);
}
</style>
