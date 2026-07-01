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
      <h1>SmartRoad Admin</h1>
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
  border: 1px solid var(--color-primary-dark);
  border-radius: var(--radius);
  padding: 2.5rem 2rem;
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.login-card h1 {
  font-size: 1.15rem;
  margin: 0 0 0.5rem;
  text-align: center;
  color: var(--color-primary-dark);
}

.login-card label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: var(--color-text-secondary);
}

.login-card input {
  padding: 0.55rem 0.65rem;
  border: 1px solid var(--color-divider);
  border-radius: var(--radius);
  background: var(--color-on-primary);
  color: var(--color-text-primary);
}

.login-card input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.login-card .btn {
  width: 100%;
  padding: 0.65rem;
}

.error {
  color: var(--color-danger);
  font-size: 0.85rem;
  margin: 0;
}
</style>
