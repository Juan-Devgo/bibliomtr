import { QueryClient } from '@tanstack/react-query'

/** Cliente de TanStack Query compartido por toda la aplicación. */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
})
