import http from 'node:http'
import crypto from 'node:crypto'

const port = Number(process.env.PORT || 9090)
const secret = process.env.TRASTCMS_PLUGIN_SECRET || 'replace-with-the-same-16-character-secret'

http.createServer((request, response) => {
  if (request.method !== 'POST' || request.url !== '/trastcms/events') {
    response.writeHead(404).end()
    return
  }
  let body = ''
  request.on('data', chunk => body += chunk)
  request.on('end', () => {
    const expected = 'sha256=' + crypto.createHmac('sha256', secret).update(body).digest('hex')
    const received = request.headers['x-trastcms-signature'] || ''
    const valid = expected.length === received.length && crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(received))
    if (!valid) { response.writeHead(401).end('invalid signature'); return }
    console.log(`[${request.headers['x-trastcms-event']}]`, JSON.parse(body))
    response.writeHead(204).end()
  })
}).listen(port, () => console.log(`Logger plugin escuchando en http://localhost:${port}`))
