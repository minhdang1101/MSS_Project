import { useMemo } from 'react'
import bookApi from '../../api/bookApi.js'
import useFetch from '../../hooks/useFetch.js'

/** The catalogue plus a lookup by id, so a loan or a fine that only knows a book id can show its title and cover. */
export default function useBooks() {
  const books = useFetch(bookApi.getAll)
  const byId = useMemo(() => new Map((books.data ?? []).map((b) => [b.id, b])), [books.data])
  return { ...books, byId }
}

/** Stands in for a book the catalogue no longer lists. */
export const unknownBook = (id) => ({ id, title: `Sách #${id}`, author: '' })

/** Lower case with the Vietnamese accents removed, so "dế mèn" is found by "de men". */
export const fold = (text) =>
  (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/gi, 'd').toLowerCase()
