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
        <dd>{{ reporterName }}</dd>

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
  color: var(--color-primary);
}

.report-card {
  max-width: 640px;
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
  max-height: 360px;
  object-fit: cover;
  border-radius: var(--radius);
  border: 1px solid var(--color-divider);
  margin: 1rem 0;
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
  row-gap: 0.75rem;
  margin: 1.5rem 0;
}

.fields dt {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.fields dd {
  margin: 0;
}

.status-form {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--color-divider);
}

.status-form select {
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--color-divider);
  border-radius: var(--radius);
  background: var(--color-on-primary);
  color: var(--color-text-primary);
}

.error {
  color: var(--color-danger);
  margin-top: 0.75rem;
}

.empty {
  color: var(--color-text-secondary);
}
</style>
