import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Response interceptor — redirect to login on 401
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      delete api.defaults.headers.common['Authorization']
      window.location.href = '/'
    }
    return Promise.reject(err)
  }
)

export const getJobs = () => api.get('/jobs')
export const getJob = (id) => api.get(`/jobs/${id}`)
export const registerJob = (data) => api.post('/jobs', data)
export const deleteJob = (id) => api.delete(`/jobs/${id}`)
export const getStats = (id) => api.get(`/jobs/${id}/stats`)
export const getExecutions = (id, page = 0, size = 20) =>
  api.get(`/jobs/${id}/executions`, { params: { page, size } })
export const getAlertHistory = (page = 0, size = 20) =>
  api.get('/alerts/history', { params: { page, size } })
export const resolveAlert = (id) => api.post(`/alerts/${id}/resolve`)
export const pingStart = (slug) => api.post(`/ping/${slug}/start`)
export const pingFinish = (slug) => api.post(`/ping/${slug}/finish`)
export const pingFail = (slug) => api.post(`/ping/${slug}/fail`)
export const addChannel = (jobId, data) => api.post(`/jobs/${jobId}/channels`, data)
export const getChannels = (jobId) => api.get(`/jobs/${jobId}/channels`)

export default api
