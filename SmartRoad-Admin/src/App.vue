<script setup>
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { signOut } from 'firebase/auth'
import { auth } from './firebase'
import { currentUser } from './auth'

const route = useRoute()
const router = useRouter()

async function handleLogout() {
  await signOut(auth)
  router.push('/login')
}
</script>

<template>
  <header v-if="route.name !== 'login'" class="topbar">
    <h1 class="brand">SmartRoad Admin</h1>
    <nav>
      <RouterLink to="/">Dashboard</RouterLink>
      <RouterLink to="/reports">Manage Reports</RouterLink>
    </nav>
    <div class="user-info">
      <span v-if="currentUser">{{ currentUser.email }}</span>
      <button class="btn btn-outline-light" @click="handleLogout">Logout</button>
    </div>
  </header>

  <main :class="route.name === 'login' ? '' : 'content'">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  background: var(--color-primary);
  color: var(--color-on-primary);
  gap: 1.5rem;
  border-bottom: 1px solid var(--color-primary-dark);
}

.brand {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0;
  color: var(--color-on-primary);
  white-space: nowrap;
}

nav {
  display: flex;
  gap: 2rem;
  flex: 1;
}

nav a {
  color: rgba(255, 255, 255, 0.75);
  text-decoration: none;
  font-weight: 500;
  padding: 0.25rem 0;
  border-bottom: 2px solid transparent;
}

nav a:hover {
  color: var(--color-on-primary);
  text-decoration: none;
}

nav a.router-link-active {
  color: var(--color-on-primary);
  border-bottom-color: var(--color-accent);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.8);
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}
</style>
