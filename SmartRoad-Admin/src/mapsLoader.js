let loadPromise = null

export function loadGoogleMaps() {
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    if (window.google?.maps) {
      resolve(window.google.maps)
      return
    }

    const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
    const callbackName = '__initGoogleMaps'

    window[callbackName] = () => {
      resolve(window.google.maps)
      delete window[callbackName]
    }

    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${key}&callback=${callbackName}&loading=async`
    script.async = true
    script.onerror = () => reject(new Error('Failed to load Google Maps JS API'))
    document.head.appendChild(script)
  })

  return loadPromise
}
