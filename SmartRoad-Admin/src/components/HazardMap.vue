<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { loadGoogleMaps } from '../mapsLoader'

const props = defineProps({
  reports: { type: Array, default: () => [] },
  height: { type: String, default: '400px' },
})

// Same red/blue/orange/green/yellow mapping as the Android app's HazardMapActivity.
const LEGEND = [
  { type: 'Pothole', color: '#e53935' },
  { type: 'Flood', color: '#1e88e5' },
  { type: 'Accident', color: '#fb8c00' },
  { type: 'Fallen Tree', color: '#43a047' },
  { type: 'Traffic Light', color: '#fdd835' },
]
const COLOR_BY_TYPE = Object.fromEntries(LEGEND.map((item) => [item.type, item.color]))
const DEFAULT_COLOR = '#9e9e9e'

function pinIcon(color) {
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="38" viewBox="0 0 28 38">` +
    `<path d="M14 0C6.268 0 0 6.268 0 14c0 9.5 14 24 14 24s14-14.5 14-24C28 6.268 21.732 0 14 0z" ` +
    `fill="${color}" stroke="#00000040" stroke-width="1"/>` +
    `<circle cx="14" cy="14" r="5.5" fill="#ffffff"/>` +
    `</svg>`
  return {
    url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    scaledSize: new google.maps.Size(28, 38),
    anchor: new google.maps.Point(14, 38),
  }
}

const mapEl = ref(null)
const loadError = ref('')
const hiddenTypes = ref(new Set())
const router = useRouter()

let map = null
let infoWindow = null
let markerEntries = []

function clearMarkers() {
  markerEntries.forEach(({ marker }) => marker.setMap(null))
  markerEntries = []
}

function applyVisibility() {
  for (const { marker, type } of markerEntries) {
    marker.setMap(hiddenTypes.value.has(type) ? null : map)
  }
}

function toggleType(type) {
  if (hiddenTypes.value.has(type)) {
    hiddenTypes.value.delete(type)
  } else {
    hiddenTypes.value.add(type)
  }
  applyVisibility()
}

function renderMarkers() {
  if (!map) return
  clearMarkers()

  const bounds = new google.maps.LatLngBounds()
  let hasValid = false

  for (const r of props.reports) {
    if (typeof r.latitude !== 'number' || typeof r.longitude !== 'number') continue
    const position = { lat: r.latitude, lng: r.longitude }
    const color = COLOR_BY_TYPE[r.type] || DEFAULT_COLOR

    const marker = new google.maps.Marker({
      position,
      map,
      icon: pinIcon(color),
      title: r.type || 'Unknown',
    })

    marker.addListener('click', () => {
      const content = document.createElement('div')
      const title = document.createElement('strong')
      title.textContent = r.type || 'Unknown'
      const statusLine = document.createElement('div')
      statusLine.textContent = `Status: ${r.status || 'New'}`
      const btn = document.createElement('button')
      btn.textContent = 'View details'
      btn.style.cssText =
        'margin-top:6px;background:#3D5A3D;color:#fff;border:none;border-radius:4px;padding:4px 10px;cursor:pointer;font:inherit;'
      btn.addEventListener('click', () => router.push(`/reports/${r.id}`))
      content.append(title, statusLine, btn)

      infoWindow.setContent(content)
      infoWindow.open({ anchor: marker, map })
    })

    markerEntries.push({ marker, type: r.type })
    bounds.extend(position)
    hasValid = true
  }

  applyVisibility()

  if (hasValid) {
    map.fitBounds(bounds)
    google.maps.event.addListenerOnce(map, 'bounds_changed', () => {
      if (map.getZoom() > 16) map.setZoom(16)
    })
  } else {
    map.setCenter({ lat: 4.2105, lng: 101.9758 })
    map.setZoom(6)
  }
}

onMounted(async () => {
  try {
    await loadGoogleMaps()
    map = new google.maps.Map(mapEl.value, {
      center: { lat: 4.2105, lng: 101.9758 },
      zoom: 6,
      streetViewControl: false,
      mapTypeControl: false,
    })
    infoWindow = new google.maps.InfoWindow()
    renderMarkers()
  } catch {
    loadError.value = 'Failed to load Google Maps. Check the API key.'
  }
})

onUnmounted(() => {
  clearMarkers()
})

watch(
  () => props.reports,
  () => renderMarkers(),
  { deep: true },
)
</script>

<template>
  <div class="hazard-map">
    <div v-if="loadError" class="map-error">{{ loadError }}</div>
    <div v-else ref="mapEl" class="map-canvas" :style="{ height }"></div>

    <div class="legend">
      <button
        v-for="item in LEGEND"
        :key="item.type"
        type="button"
        class="legend-item"
        :class="{ inactive: hiddenTypes.has(item.type) }"
        @click="toggleType(item.type)"
      >
        <span class="legend-dot" :style="{ background: item.color }"></span>
        {{ item.type }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.hazard-map {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.map-canvas {
  width: 100%;
  border-radius: var(--radius);
  border: 1px solid var(--color-divider);
}

.map-error {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-divider);
  border-radius: var(--radius);
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.25rem;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.25rem 0.6rem;
  border: none;
  background: none;
  border-radius: var(--radius);
  font: inherit;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: opacity 0.15s;
}

.legend-item:hover {
  background: var(--color-surface);
}

.legend-item.inactive {
  opacity: 0.45;
  text-decoration: line-through;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
</style>
