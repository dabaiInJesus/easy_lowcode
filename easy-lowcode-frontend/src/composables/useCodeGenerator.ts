/**
 * Composable for generating slugs/codes from display names
 *
 * Provides utilities to convert human-readable names into
 * URL-safe, lowercase, hyphen-separated codes suitable for
 * use as unique identifiers, API paths, or resource codes.
 *
 * @param prefix - Optional prefix to prepend to generated codes
 * @returns Object containing generateCode, generateSlug, and validateCode functions
 *
 * @example
 * const { generateCode, generateSlug } = useCodeGenerator('usr')
 * generateCode('User Management')
 * generateSlug('User Management')
 */
export function useCodeGenerator(prefix?: string) {
  /**
   * Generate a unique code/slug from a display name
   *
   * Converts the input to lowercase, replaces spaces and special
   * characters with hyphens, and removes consecutive hyphens.
   *
   * @param displayName - Human-readable name to convert
   * @param separator - Character to use as separator (defaults to '_')
   * @returns Generated code string
   */
  function generateCode(displayName: string, separator = '_'): string {
    if (!displayName) return ''

    const code = displayName
      .trim()
      .toLowerCase()
      .replace(/[\s\-]+/g, separator)
      .replace(/[^a-z0-9_\u4e00-\u9fa5]/g, '')
      .replace(new RegExp(`${separator}+`, 'g'), separator)
      .replace(new RegExp(`^${separator}|${separator}$`, 'g'), '')

    return prefix ? `${prefix}${separator}${code}` : code
  }

  /**
   * Generate a URL-safe slug from a display name
   *
   * Similar to generateCode but uses hyphens as separators
   * and removes non-ASCII characters for URL compatibility.
   *
   * @param displayName - Human-readable name to convert
   * @returns URL-safe slug string
   */
  function generateSlug(displayName: string): string {
    if (!displayName) return ''

    const slug = displayName
      .trim()
      .toLowerCase()
      .replace(/[\s_]+/g, '-')
      .replace(/[^a-z0-9\-]/g, '')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '')

    return prefix ? `${prefix}-${slug}` : slug
  }

  /**
   * Generate a unique code with auto-increment suffix if needed
   *
   * @param displayName - Human-readable name to convert
   * @param existingCodes - Set of existing codes to avoid duplicates
   * @returns Unique code string
   */
  function generateUniqueCode(displayName: string, existingCodes: Set<string>): string {
    let code = generateCode(displayName)
    let counter = 1

    while (existingCodes.has(code)) {
      code = `${generateCode(displayName)}_${counter}`
      counter++
    }

    return code
  }

  /**
   * Validate if a code string is properly formatted
   *
   * @param code - Code string to validate
   * @returns True if code is valid
   */
  function validateCode(code: string): boolean {
    if (!code) return false
    return /^[a-z0-9_]+$/i.test(code)
  }

  /**
   * Validate if a slug is properly formatted for URLs
   *
   * @param slug - Slug string to validate
   * @returns True if slug is valid
   */
  function validateSlug(slug: string): boolean {
    if (!slug) return false
    return /^[a-z0-9\-]+$/i.test(slug)
  }

  return {
    generateCode,
    generateSlug,
    generateUniqueCode,
    validateCode,
    validateSlug,
  }
}
