const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  if (options.raw) return res
  const json = await res.json()
  if (!json.success) throw new Error(json.message || 'Request failed')
  return json.data
}

export const api = {
  get: path => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: path => request(path, { method: 'DELETE' }),
  pdf: id => `${API_BASE}/ai/reports/${id}/pdf`
}
