/**
 * True if every character of `query` appears in `text`, in order, case-insensitively - not
 * necessarily contiguous (e.g. "qcd" matches "Quality Control Department"). The same lightweight
 * "fuzzy search" behavior as VS Code's command palette or Sublime's Ctrl+P, not literal substring
 * matching.
 */
export function fuzzyMatch(text: string, query: string): boolean {
  if (query.length === 0) return true
  const t = text.toLowerCase()
  const q = query.toLowerCase()
  let ti = 0
  for (const ch of q) {
    ti = t.indexOf(ch, ti)
    if (ti === -1) return false
    ti++
  }
  return true
}
