import { createContext, useContext, useState, useCallback } from 'react'
import axios from 'axios'
import api from './client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)

  const login = useCallback(async (username, password) => {
    const res = await axios.post('/auth/login', { username, password })
    const data = res.data
    setUser({ username: data.username, email: data.email, token: data.token })
    api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`
    return data
  }, [])

  const register = useCallback(async (username, email, password) => {
    const res = await axios.post('/auth/register', { username, email, password })
    const data = res.data
    setUser({ username: data.username, email: data.email, token: data.token })
    api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`
    return data
  }, [])

  const logout = useCallback(() => {
    setUser(null)
    delete api.defaults.headers.common['Authorization']
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)