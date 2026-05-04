const BASE_URL = 'http://localhost:8080/api/notices';

async function handle(res) {
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed: ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export function fetchNotices() {
  return fetch(BASE_URL).then(handle);
}

export function fetchNotice(id) {
  return fetch(`${BASE_URL}/${id}`).then(handle);
}

export function createNotice(data) {
  return fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }).then(handle);
}

export function updateNotice(id, data) {
  return fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }).then(handle);
}

export function deleteNotice(id) {
  return fetch(`${BASE_URL}/${id}`, { method: 'DELETE' }).then(handle);
}