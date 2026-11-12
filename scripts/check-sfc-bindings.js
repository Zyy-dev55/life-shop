/**
 * 静态检查：找出模板里引用了、但 <script setup> 里没有定义的变量。
 * 这类问题编译能过，运行时渲染报 TypeError，表现为整页白屏、点不动。
 *
 * 用法：node scripts/check-sfc-bindings.js
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.join(__dirname, '..', 'frontend', 'node_modules')
const { parse, compileTemplate, compileScript } = require(path.join(ROOT, 'vue', 'compiler-sfc'))

// 模板里可以直接用、无需在 setup 中声明的名字
const GLOBALS = new Set([
  'Math', 'Date', 'JSON', 'Number', 'String', 'Boolean', 'Array', 'Object', 'parseInt', 'parseFloat',
  'isNaN', 'isFinite', 'encodeURIComponent', 'decodeURIComponent', 'undefined', 'null', 'true', 'false',
  'console', 'window', 'document', 'RegExp', 'Set', 'Map', 'Intl', 'NaN', 'Infinity'
])

function walk(dir, out = []) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p, out)
    else if (name.endsWith('.vue')) out.push(p)
  }
  return out
}

let problems = 0
const files = walk(path.join(__dirname, '..', 'frontend', 'src'))

for (const file of files) {
  const source = fs.readFileSync(file, 'utf8')
  const { descriptor, errors } = parse(source, { filename: file })
  if (errors.length) {
    console.log(`✗ 解析失败 ${file}`)
    problems++
    continue
  }
  if (!descriptor.scriptSetup || !descriptor.template) continue

  let bindings = {}
  try {
    const script = compileScript(descriptor, { id: 'check' })
    bindings = script.bindings || {}
  } catch (e) {
    console.log(`✗ 脚本编译失败 ${file}: ${e.message}`)
    problems++
    continue
  }

  const tpl = compileTemplate({
    source: descriptor.template.content,
    filename: file,
    id: 'check',
    compilerOptions: { bindingMetadata: bindings }
  })
  if (tpl.errors && tpl.errors.length) {
    console.log(`✗ 模板编译失败 ${file}: ${tpl.errors[0].message || tpl.errors[0]}`)
    problems++
    continue
  }

  // 未被 setup 绑定的引用会编译成 _ctx.xxx
  const missing = new Set()
  const re = /_ctx\.([A-Za-z_$][\w$]*)/g
  let m
  while ((m = re.exec(tpl.code))) {
    const name = m[1]
    // $router / $route 等来自 app.config.globalProperties，不算未定义
    if (name.startsWith('$')) continue
    if (!bindings[name] && !GLOBALS.has(name)) missing.add(name)
  }

  if (missing.size) {
    problems++
    console.log(`✗ ${path.relative(path.join(__dirname, '..'), file)}`)
    console.log(`    模板引用了未定义变量：${[...missing].join(', ')}`)
  }
}

console.log(problems === 0 ? '\n全部页面模板与脚本变量一致 ✓' : `\n共 ${problems} 个文件存在问题`)
process.exit(problems === 0 ? 0 : 1)
