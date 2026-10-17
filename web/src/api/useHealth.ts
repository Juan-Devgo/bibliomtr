import { useQuery } from '@tanstack/react-query'
import { apiGet } from './client'

interface Health {
  status: string
}

/** Estado del backend (Spring Boot Actuator). */
export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: () => apiGet<Health>('/api/actuator/health'),
  })
}
