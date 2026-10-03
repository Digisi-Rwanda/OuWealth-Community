/** Hands a mailto: URL to the visitor's email app. Kept separate so tests can observe it. */
export function openMailClient(url: string): void {
  window.location.href = url
}
