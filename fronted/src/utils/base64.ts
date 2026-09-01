/** UTF-8 安全的 Base64 编解码。用于将错误详情编码为错误码(便于复制/转述,非加密)。 */
const encoder = new TextEncoder()
const decoder = new TextDecoder()

export function encodeBase64(text: string): string {
  let binary = ''
  for (const byte of encoder.encode(text)) binary += String.fromCharCode(byte)
  return btoa(binary)
}

export function decodeBase64(b64: string): string {
  const binary = atob(b64.trim())
  const bytes = Uint8Array.from(binary, (ch) => ch.charCodeAt(0))
  return decoder.decode(bytes)
}
