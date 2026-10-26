import { useCallback, useEffect, useState } from 'react'

/** Runs an async API call on mount and exposes { data, loading, error, reload }. */
export default function useFetch(fetcher) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await fetcher())
    } catch (err) {
      setError(err.message ?? 'Unexpected error')
    } finally {
      setLoading(false)
    }
  }, [fetcher])

  useEffect(() => {
    load()
  }, [load])

  return { data, loading, error, reload: load }
}
