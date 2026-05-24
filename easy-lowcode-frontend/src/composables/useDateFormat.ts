import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

export type DateFormat = 'YYYY-MM-DD' | 'YYYY-MM-DD HH:mm:ss' | 'YYYY/MM/DD' | 'YYYY/MM/DD HH:mm:ss' | 'MM-DD HH:mm' | 'HH:mm:ss'

/**
 * Composable for date/time formatting using dayjs
 *
 * Provides a formatTime function with sensible defaults and
 * support for relative time formatting.
 *
 * @param defaultFormat - Default date format string (defaults to 'YYYY-MM-DD HH:mm:ss')
 * @returns Object containing formatTime and formatRelative functions
 *
 * @example
 * const { formatTime, formatRelative } = useDateFormat()
 * formatTime('2024-01-15T10:30:00')
 * formatTime('2024-01-15T10:30:00', 'YYYY/MM/DD')
 * formatRelative('2024-01-15T10:30:00')
 */
export function useDateFormat(defaultFormat: DateFormat = 'YYYY-MM-DD HH:mm:ss') {
  /**
   * Format a date/time value to the specified format
   *
   * @param date - Date value (string, Date, Dayjs, or timestamp)
   * @param format - Output format string (uses defaultFormat if not provided)
   * @param fallback - Fallback string when date is invalid/empty (defaults to '-')
   * @returns Formatted date string or fallback
   */
  function formatTime(
    date: string | Date | Dayjs | number | null | undefined,
    format?: DateFormat,
    fallback = '-'
  ): string {
    if (!date) return fallback

    const parsed = dayjs(date)
    if (!parsed.isValid()) return fallback

    return parsed.format(format ?? defaultFormat)
  }

  /**
   * Format a date as relative time (e.g., "2 hours ago")
   *
   * @param date - Date value to format
   * @param suffix - Suffix to append (defaults to ' ago')
   * @returns Relative time string or formatted date if older than 7 days
   */
  function formatRelative(
    date: string | Date | Dayjs | number | null | undefined,
    suffix = ' ago'
  ): string {
    if (!date) return '-'

    const parsed = dayjs(date)
    if (!parsed.isValid()) return '-'

    const now = dayjs()
    const diffDays = now.diff(parsed, 'day')

    if (diffDays === 0) {
      const diffHours = now.diff(parsed, 'hour')
      if (diffHours === 0) {
        const diffMinutes = now.diff(parsed, 'minute')
        if (diffMinutes === 0) return 'Just now'
        return `${diffMinutes} minutes${suffix}`
      }
      return `${diffHours} hours${suffix}`
    }

    if (diffDays === 1) return `Yesterday${suffix}`
    if (diffDays < 7) return `${diffDays} days${suffix}`

    return parsed.format(defaultFormat)
  }

  /**
   * Parse a date string into a Dayjs object
   */
  function parseDate(date: string | Date | number | null | undefined): Dayjs | null {
    if (!date) return null
    const parsed = dayjs(date)
    return parsed.isValid() ? parsed : null
  }

  return {
    formatTime,
    formatRelative,
    parseDate,
  }
}
