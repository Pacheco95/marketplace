import { joinURL } from 'ufo'

export default defineEventHandler(async (event) => {
  if (!event.path.startsWith('/api/')) return

  const config = useRuntimeConfig()
  const target = joinURL(config.apiBaseUrl as string, event.path)
  return proxyRequest(event, target)
})
