// E2E interactive test: drives real Chrome (on Xvfb virtual display) against the
// running stack (vite 5173 → backend 8080). Asserts the full invoice lifecycle.
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// Reset DB to a clean slate before the test: delete every invoice via the API so
// the sample PDF (fixed invoice number) can be uploaded without a 409 duplicate.
async function resetInvoices() {
  const api = 'http://localhost:5173/api/invoices'
  for (;;) {
    const res = await fetch(`${api}?page=0&size=20`)
    const { content } = await res.json()
    if (!content.length) break
    for (const r of content) {
      const d = await fetch(`${api}/${r.id}`, { method: 'DELETE' })
      if (!d.ok) throw new Error(`reset: delete ${r.id} -> ${d.status}`)
    }
  }
  console.log('  DB reset (0 invoices)')
}

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const SHOT_DIR = path.join(__dirname, 'screenshots')
fs.mkdirSync(SHOT_DIR, { recursive: true })

const BASE = 'http://localhost:5173'
const CHROME = '/usr/bin/google-chrome-stable'
const SAMPLE = '/home/gaobingyi/code_repos/my-invoice-manager/invoice_examples/_餐饮服务_餐饮服务_-2026年05月26日-201.00.pdf'
const SAMPLE2 = '/home/gaobingyi/code_repos/my-invoice-manager/invoice_examples/260331_178.00_深圳市美之高实业发展有限公司.pdf'
const INVOICE_NUMBER = '26322000004144614676'

let pass = 0, fail = 0
function check(name, cond) {
  if (cond) { pass++; console.log(`  ✅ ${name}`) }
  else { fail++; console.log(`  ❌ ${name}`) }
}

await resetInvoices()
console.log('0) 清理数据库')

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: false,            // headed on Xvfb: real browser behaviour
  args: ['--no-sandbox', '--disable-gpu', '--window-size=1400,900']
})

try {
  const page = await browser.newPage()
  await page.setViewport({ width: 1400, height: 900 })
  const errors = []
  page.on('pageerror', e => errors.push(e.message))
  page.on('requestfailed', r => errors.push(`requestfailed: ${r.url()}`))
  page.on('console', m => {
    if (m.type() !== 'error') return
    const txt = m.text()
    // favicon + aborted navigation noise; keep everything else
    if (txt.includes('favicon') || txt.includes('Failed to load resource')) {
      const req = m.location()
      errors.push(`console-error: ${txt} @${req.url}`)
    } else {
      errors.push(txt)
    }
  })

  // --- 1. load ---
  console.log('\n1) 加载页面')
  await page.goto(BASE, { waitUntil: 'networkidle0', timeout: 20000 })
  await page.waitForSelector('table', { timeout: 10000 })
  check('表格渲染', true)
  await page.screenshot({ path: path.join(SHOT_DIR, '1-list.png') })

  // --- 2. upload (two invoices consecutively) ---
  console.log('\n2) 连续上传发票')
  const rowsBefore = await page.$$eval('tbody tr', trs => trs.length)
  const samples = [SAMPLE, SAMPLE2]
  // select BOTH files at once, then upload in one click (batch upload)
  const input = await page.$('input[type=file]')
  if (!input) throw new Error('file input not found')
  await input.uploadFile(...samples)
  await new Promise(r => setTimeout(r, 400))   // let on-change fire for both
  const btnText = await page.$eval('.upload-btn', b => b.textContent).catch(() => '')
  check(`批量选择后按钮显示数量 (${btnText.trim()})`, btnText.includes('2'))
  await page.click('.upload-btn:not(.is-disabled)')
  // wait for both rows to appear
  await page.waitForFunction(
    (n) => document.querySelectorAll('tbody tr').length >= n + 2,
    { timeout: 15000 }, rowsBefore
  )
  const rowsAfter = await page.$$eval('tbody tr', trs => trs.length)
  check(`批量上传后行数 ${rowsBefore} → ${rowsAfter}`, rowsAfter >= rowsBefore + 2)
  // verify both sellers rendered
  const sellerNames = await page.$$eval('tbody tr', trs => trs.map(tr => tr.innerText))
  check('含餐饮发票', sellerNames.some(t => t.includes('扬州滋奇奥邦餐饮管理有限公司')))
  check('含美之高发票', sellerNames.some(t => t.includes('深圳市美之高实业发展有限公司')))
  await page.screenshot({ path: path.join(SHOT_DIR, '2-after-upload.png') })

  // --- 3. preview ---
  console.log('\n3) 预览')
  // wait for the "上传成功" toast to clear so it can't swallow the row's clicks
  await page.waitForFunction(
    () => !document.querySelector('.el-message'),
    { timeout: 5000 }
  ).catch(() => {})
  // first row's 预览 button
  const row = (await page.$$('tbody tr'))[0]
  const cellBtns = await row.$$('button')
  const texts = await Promise.all(cellBtns.map(b => b.evaluate(el => el.textContent)))
  // buttons: 预览, 下载, 删除
  const previewBtn = cellBtns[texts.findIndex(t => t.includes('预览'))]
  if (!previewBtn) throw new Error('preview button not found; texts=' + JSON.stringify(texts))
  await previewBtn.click()
  await page.waitForSelector('.el-dialog', { timeout: 5000 })
  await new Promise(r => setTimeout(r, 1500))
  const hasIframe = await page.$eval('.el-dialog iframe', f => !!f).catch(() => false)
  check('预览对话框含 iframe', hasIframe)
  await page.screenshot({ path: path.join(SHOT_DIR, '3-preview.png') })
  // close dialog
  await page.click('.el-dialog__headerbtn')
  await new Promise(r => setTimeout(r, 500))

  // --- 4. download ---
  console.log('\n4) 下载')
  // download flows fire Page.downloadWillBegin — assert an attachment download starts
  const cdp = await page.createCDPSession()
  let downloaded = null
  cdp.on('Page.downloadWillBegin', e => { downloaded = { url: e.url, filename: e.suggestedFilename } })
  await cdp.send('Page.enable')
  await page.waitForFunction(
    () => !document.querySelector('.el-message'),
    { timeout: 5000 }
  ).catch(() => {})
  const row2 = (await page.$$('tbody tr'))[0]
  const btns2 = await row2.$$('button')
  const texts2 = await Promise.all(btns2.map(b => b.evaluate(el => el.textContent)))
  const downloadBtn = btns2[texts2.findIndex(t => t.includes('下载'))]
  if (!downloadBtn) throw new Error('download button not found')
  await downloadBtn.click()
  await new Promise(r => setTimeout(r, 1500))
  check(
    `下载触发 attachment (${downloaded ? downloaded.filename : 'none'})`,
    !!downloaded
  )
  await page.screenshot({ path: path.join(SHOT_DIR, '4-download.png') })

  // --- 5. delete ---
  console.log('\n5) 删除')
  const countBefore = await page.$$eval('tbody tr', trs => trs.length)
  const row3 = (await page.$$('tbody tr'))[0]
  const btns3 = await row3.$$('button')
  const texts3 = await Promise.all(btns3.map(b => b.evaluate(el => el.textContent)))
  const delBtn = btns3[texts3.findIndex(t => t.includes('删除'))]
  if (!delBtn) throw new Error('delete button not found')
  await delBtn.click()
  await page.waitForSelector('.el-message-box', { timeout: 5000 })
  await page.screenshot({ path: path.join(SHOT_DIR, '5-confirm.png') })
  // click confirm (删除 in messagebox footer)
  const confirm = await page.$('.el-message-box__btns .el-button--danger, .el-message-box__btns button:last-child')
  await confirm.click()
  await page.waitForFunction(
    (n) => document.querySelectorAll('tbody tr').length < n,
    { timeout: 10000 }, countBefore
  )
  const countAfter = await page.$$eval('tbody tr', trs => trs.length)
  check(`删除后行数 ${countBefore} → ${countAfter}`, countAfter < countBefore)
  await page.screenshot({ path: path.join(SHOT_DIR, '6-after-delete.png') })

  // --- 6. no page errors ---
  console.log('\n6) 控制台错误')
  check(`无 JS 错误 (${errors.length})`, errors.length === 0)
  if (errors.length) console.log('  errors:', errors.slice(0, 5).join(' | '))

  // --- 7. cleanup: leave the DB empty ---
  console.log('\n7) 清理数据')
  await resetInvoices()
  const remaining = (await (await fetch('http://localhost:5173/api/invoices?page=0&size=20')).json()).totalElements
  check(`DB 归零 (剩余 ${remaining})`, remaining === 0)

} finally {
  await browser.close()
}

console.log(`\n=== PASS ${pass} / FAIL ${fail} ===`)
process.exit(fail ? 1 : 0)
