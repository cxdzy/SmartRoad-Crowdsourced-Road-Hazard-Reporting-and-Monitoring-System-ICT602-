<script setup>
import { ref } from 'vue'
import { signInWithEmailAndPassword, signOut } from 'firebase/auth'
import { ref as dbRef, get } from 'firebase/database'
import { useRouter } from 'vue-router'
import { auth, db } from '../firebase'

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const router = useRouter()

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const cred = await signInWithEmailAndPassword(auth, email.value, password.value)
    const snap = await get(dbRef(db, `admins/${cred.user.uid}`))
    if (snap.val() !== true) {
      await signOut(auth)
      error.value = 'This account is not authorized as an admin.'
      return
    }
    router.push('/')
  } catch {
    error.value = 'Login failed. Check your email and password.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="handleLogin">
      <svg class="login-icon" viewBox="0 0 24 24" width="40" height="40" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M11 2L4 21h3.5l1.2-4h6.6l1.2 4H20L13 2h-2zm-1.4 12L12 6.5 14.4 14H9.6z" fill="#636B2F" />
      </svg>
      <h1>SmartRoad Admin</h1>
      <p class="subtitle">Administrator Portal</p>
      <label>
        Email
        <input v-model="email" type="email" required autocomplete="username" />
      </label>
      <label>
        Password
        <input v-model="password" type="password" required autocomplete="current-password" />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" class="btn btn-primary" :disabled="loading">
        {{ loading ? 'Signing in…' : 'Sign in' }}
      </button>
      <p class="footer-note">SmartRoad © 2026</p>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
}

.login-card {
  background: var(--color-surface);
  border-top: 4px solid #636B2F;
  border-radius: var(--radius);
  box-shadow: 0 4px 24px rgba(61, 65, 39, 0.13);
  padding: 2.5rem;
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.login-icon {
  align-self: center;
  margin-bottom: -0.5rem;
}

.login-card h1 {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin: 0;
  text-align: center;
  color: #3D4127;
}

.subtitle {
  margin: 0 0 1.5rem;
  text-align: center;
  color: #636B2F;
  font-size: 0.85rem;
}

.login-card label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: #636B2F;
  font-weight: 600;
  font-size: 0.85rem;
}

.login-card input {
  padding: 0.6rem 0.75rem;
  border: 1.5px solid #BAC095;
  border-radius: 6px;
  background: var(--color-on-primary);
  color: var(--color-text-primary);
  font-weight: 400;
}

.login-card input:focus {
  outline: none;
  border-color: #636B2F;
}

.login-card .btn {
  width: 100%;
  padding: 0.75rem;
  background: #E87000;
  color: #fff;
  font-weight: 700;
  border-radius: 8px;
  transition: background 0.2s ease;
}

.login-card .btn:hover:not(:disabled) {
  background: #c96200;
}

.error {
  color: var(--color-danger);
  font-size: 0.85rem;
  margin: 0;
}

.footer-note {
  margin: 1rem 0 0;
  text-align: center;
  color: #BAC095;
  font-size: 0.75rem;
}
</style>
