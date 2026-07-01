<script setup>
defineProps({ src: String })
defineEmits(['close'])
</script>

<template>
  <div class="overlay" @click.self="$emit('close')">
    <button class="close-btn" @click="$emit('close')">&times;</button>
    <img :src="src" class="full-photo" alt="Hazard photo full size" />
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.full-photo {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 4px;
}

.close-btn {
  position: absolute;
  top: 1.5rem;
  right: 2rem;
  background: none;
  border: none;
  color: white;
  font-size: 2rem;
  line-height: 1;
  cursor: pointer;
  transition: color 0.15s;
}

.close-btn:hover {
  color: var(--color-accent);
}
</style>
