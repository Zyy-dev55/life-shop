/**
 * 生成「双击即可运行」的发布包。
 *
 * 前置：
 *   1. frontend 构建产物已在 backend/src/main/resources/static
 *      （cd frontend && npm run build:server）
 *   2. 后端已打包：backend/target/life-shop-server.jar
 *   3. 精简 JRE 已生成：build/jre（jlink 产物）
 *
 * 产物：
 *   dist/life-shop-demo/       目录版，可直接拷给别人
 *   dist/生活用品商城-演示版.zip
 *
 * 用法：node scripts/make-package.js
 */
const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

const ROOT = path.join(__dirname, '..')
const JAR = path.join(ROOT, 'backend', 'target', 'life-shop-server.jar')
const STATIC_INDEX = path.join(ROOT, 'backend', 'src', 'main', 'resources', 'static', 'index.html')
const JRE = path.join(ROOT, 'build', 'jre')
const DIST = path.join(ROOT, 'dist', 'life-shop-demo')
const ZIP = path.join(ROOT, 'dist', '生活用品商城-演示版.zip')

function must(p, hint) {
  if (!fs.existsSync(p)) {
    console.error(`✗ 缺少 ${path.relative(ROOT, p)}\n  ${hint}`)
    process.exit(1)
  }
}

must(JAR, '先执行：cd backend && mvn clean package')
must(STATIC_INDEX, '先执行：cd frontend && npm run build:server')
must(JRE, '先执行：jlink --module-path <jdk>/jmods --add-modules ... --output build/jre')

const LAUNCHER_BAT = `@echo off
chcp 65001 >nul
cd /d "%~dp0"
title 生活用品商城 - 演示版

echo.
echo  ============================================
echo     生活用品商城 · 演示版
echo  ============================================
echo.

set "PORT=8080"

:findport
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul 2>nul
if errorlevel 1 goto :foundport
set /a PORT=PORT+1
if %PORT% gtr 8090 goto :noport
goto :findport

:foundport
set "JAVA_EXE="
if exist "runtime\\bin\\java.exe" set "JAVA_EXE=%cd%\\runtime\\bin\\java.exe"
if not defined JAVA_EXE (
  where java >nul 2>nul
  if errorlevel 1 goto :nojava
  set "JAVA_EXE=java"
)

echo  正在启动，首次大约需要 10 秒，请不要关窗口...
echo  访问地址：http://localhost:%PORT%
echo.
echo  关闭这个黑色窗口 = 停止服务。
echo.

start "" /b cmd /c "timeout /t 10 /nobreak >nul & start http://localhost:%PORT%/"

"%JAVA_EXE%" -jar life-shop.jar --spring.profiles.active=demo --server.port=%PORT%

echo.
echo  服务已停止，按任意键关闭窗口。
pause >nul
exit /b 0

:nojava
echo  [错误] 没有找到 Java 运行环境。
echo         包内应自带 runtime 文件夹；若被删除，请安装 Java 17 后重试。
echo.
pause
exit /b 1

:noport
echo  [错误] 8080-8090 端口都被占用，请先关闭占用端口的程序再试。
echo.
pause
exit /b 1
`

const LAUNCHER_SH = `#!/bin/bash
# 生活用品商城 · 演示版（macOS / Linux）
cd "$(dirname "$0")" || exit 1

echo
echo " ============================================"
echo "    生活用品商城 · 演示版"
echo " ============================================"
echo

PORT=8080
while [ $PORT -le 8090 ]; do
  if command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:$PORT -sTCP:LISTEN >/dev/null 2>&1 || break
  else
    break
  fi
  PORT=$((PORT+1))
done

JAVA_EXE="java"
if [ -x "./runtime/bin/java" ]; then
  # 包内 runtime 是 Windows 版，macOS/Linux 下不可用，改用系统 Java
  if "./runtime/bin/java" -version >/dev/null 2>&1; then
    JAVA_EXE="./runtime/bin/java"
  fi
fi

if ! "$JAVA_EXE" -version >/dev/null 2>&1; then
  echo " [错误] 没有找到可用的 Java 17。"
  echo "        macOS: brew install openjdk@17"
  echo "        Ubuntu: sudo apt install openjdk-17-jdk"
  echo
  read -n 1 -s -r -p "按任意键退出..."
  exit 1
fi

echo " 访问地址：http://localhost:$PORT"
echo " 关闭此窗口即可停止服务。"
echo

( sleep 10
  if command -v open >/dev/null 2>&1; then open "http://localhost:$PORT"
  elif command -v xdg-open >/dev/null 2>&1; then xdg-open "http://localhost:$PORT"
  fi ) >/dev/null 2>&1 &

exec "$JAVA_EXE" -jar life-shop.jar --spring.profiles.active=demo --server.port=$PORT
`

const README = `生活用品商城 · 演示版
========================================

一、怎么打开
----------------------------------------
Windows：双击「启动商城.bat」
         （如果弹出安全提示，点「更多信息」→「仍要运行」）

macOS ：双击「start-mac-linux.command」
         第一次可能提示没有权限，在该文件夹打开终端执行：
         chmod +x start-mac-linux.command

Linux ：打开终端，执行 ./start-mac-linux.command

启动后会自动打开浏览器。如果没有自动打开，手动访问窗口里显示的地址，
一般是 http://localhost:8080

关闭那个黑色窗口 = 停止服务。


二、电脑需要装什么
----------------------------------------
什么都不用装。包内自带精简版 Java 运行环境（runtime 文件夹）
和内置数据库（H2），不用装 JDK，也不用装 MySQL。

注意：runtime 是 Windows 版，macOS / Linux 运行时会自动改用
系统已安装的 Java 17。


三、演示账号
----------------------------------------
平台管理员   admin        / admin123
买家         zhangsan     / 123456
商家         shop_kitchen / 123456   悦家厨房旗舰店
其他商家     shop_home    / 123456   悦家家居生活馆
             shop_clean   / 123456   净界清洁专营店
             shop_pet     / 123456   毛孩子宠物用品
             shop_personal/ 123456   雅致个护旗舰店
             shop_travel  / 123456   途行者户外优选


四、三个入口
----------------------------------------
买家商城    http://localhost:8080
平台后台    http://localhost:8080/admin
            └ 网络安全运维监控：/admin/sec-ops（左侧菜单最后一项）
商家后台    http://localhost:8080/merchant

用 merchants 账号登录后会自动进入商家后台，admin 登录进平台后台，
买家登录后停在商城前台。


五、里面有什么
----------------------------------------
6 家店铺、6 个分类、48 件商品、144 张商品图、65 条评价、70 笔订单、
8 组客服会话。

能完整走通：浏览 → 咨询客服 → 加购物车 → 下单（跨店自动拆单）→
模拟支付 → 商家发货 → 确认收货 → 评价 → 商家回复，以及售后退款、
优惠券抵扣、商家钱包提现。

网络安全运维演示（管理员登录后进入「网络安全运维监控」）：
可查看访问日志、QPS 趋势、HTTP 状态码分布、Top IP 与慢接口；
点「流量回放测试」会注入一段突发异常流量并立即执行动态基线检测，
生成流量突增、错误率异常、响应变慢、单 IP 集中访问四类告警；
页面里还能手工封禁 / 解封 IP，验证限流与黑名单是否生效。


六、数据说明
----------------------------------------
演示数据放在内存里，关掉再开就恢复成初始状态 —— 试坏了重启即可，
不用担心把数据弄乱。

自己上传的商品图片保存在 uploads 文件夹，重启后记录会清空。


七、遇到问题
----------------------------------------
1) 浏览器打开是空白页
   等 10 秒再刷新一次，服务还没起完。窗口出现
   「Started LifeShopApplication」就表示起来了。

2) 端口被占用
   脚本会自动从 8080 往后找，最多到 8090，
   窗口里「访问地址」显示的就是实际端口。

3) 窗口一闪就消失
   右键「以管理员身份运行」；还是不行就把文件夹放到
   纯英文路径（比如 D:\\demo）再试。

4) macOS 提示「无法验证开发者」
   右键文件 → 打开 → 仍要打开；
   或终端执行：xattr -d com.apple.quarantine start-mac-linux.command

5) 换台电脑
   把整个文件夹（或解压后的 zip）复制过去，不要只复制 jar。


八、这是什么
----------------------------------------
一套演示用的生活用品电商系统，技术栈：
Spring Boot 2.7 + MyBatis-Plus + H2 + Vue 3 + Element Plus。
仅用于参观和演示，支付为模拟，不接真实支付渠道。
`

function writeCrlf(file, text, bom = false) {
  const body = text.replace(/\r\n/g, '\n').split('\n').join('\r\n')
  fs.writeFileSync(file, (bom ? '\ufeff' : '') + body, 'utf8')
}

// shell 脚本必须是 LF，否则 macOS 会报 bad interpreter: /bin/bash^M
function writeLf(file, text) {
  fs.writeFileSync(file, text.replace(/\r\n/g, '\n'), 'utf8')
}

function copyDir(src, dest) {
  fs.mkdirSync(dest, { recursive: true })
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, entry.name)
    const d = path.join(dest, entry.name)
    if (entry.isDirectory()) copyDir(s, d)
    else if (entry.isSymbolicLink()) fs.symlinkSync(fs.readlinkSync(s), d)
    else fs.copyFileSync(s, d)
  }
}

// 逐文件清理：某些环境把 fs.rmSync 接到了回收站，整目录删除会失败
function rmTree(dir) {
  if (!fs.existsSync(dir)) return
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name)
    if (entry.isDirectory()) rmTree(p)
    else fs.unlinkSync(p)
  }
  fs.rmdirSync(dir)
}

// ---- 组装 ----
console.log('清理旧产物...')
try {
  rmTree(DIST)
} catch (e) {
  // 有些环境把删除操作接到回收站，大目录会被中止；这里退化为「直接覆盖」
  console.log('  （旧目录未能删除，改为覆盖写入）')
}
fs.mkdirSync(DIST, { recursive: true })

console.log('复制 jar...')
fs.copyFileSync(JAR, path.join(DIST, 'life-shop.jar'))

console.log('复制运行时（约 44MB）...')
copyDir(JRE, path.join(DIST, 'runtime'))

console.log('写入启动脚本与说明...')
writeCrlf(path.join(DIST, '启动商城.bat'), LAUNCHER_BAT)
writeLf(path.join(DIST, 'start-mac-linux.command'), LAUNCHER_SH)
writeCrlf(path.join(DIST, '使用说明.txt'), README, true)
fs.mkdirSync(path.join(DIST, 'uploads'), { recursive: true })

// 可执行权限（Git Bash 下有效；zip 里也会保留）
try {
  fs.chmodSync(path.join(DIST, 'start-mac-linux.command'), 0o755)
} catch (e) {
  /* Windows 下忽略 */
}

// ---- 压缩 ----
// 用 Python 的 zipfile 而不是 PowerShell Compress-Archive：
// 后者会把条目写成反斜杠路径、非 ASCII 文件名不带 UTF-8 标记，别人在 Mac 上解压会乱码。
console.log('压缩 zip...')
function findPython() {
  const candidates = [
    process.env.PYTHON,
    'python',
    'python3',
    path.join(process.env.USERPROFILE || '', '.workbuddy', 'binaries', 'python', 'envs', 'default', 'Scripts', 'python.exe'),
    path.join(process.env.HOME || '', '.workbuddy', 'binaries', 'python', 'envs', 'default', 'bin', 'python')
  ].filter(Boolean)

  for (const cmd of candidates) {
    try {
      execSync(`"${cmd}" -c "import zipfile"`, { stdio: 'ignore' })
      return cmd
    } catch (e) {
      /* 试下一个 */
    }
  }
  return null
}

const python = findPython()
if (!python) {
  console.error('✗ 没找到可用的 Python，无法生成 zip。可直接分发 dist/life-shop-demo 文件夹')
  process.exit(1)
}
execSync(`"${python}" "${path.join(__dirname, 'make_zip.py')}" "${DIST}" "${ZIP}"`, { stdio: 'inherit' })

function dirSize(p) {
  const st = fs.statSync(p)
  if (!st.isDirectory()) return st.size
  return fs.readdirSync(p, { withFileTypes: true })
    .reduce((sum, e) => sum + dirSize(path.join(p, e.name)), 0)
}
const mb = (bytes) => (bytes / 1024 / 1024).toFixed(1) + ' MB'

console.log('\n完成：')
console.log('  目录：' + path.relative(ROOT, DIST))
console.log('  压缩包：' + path.relative(ROOT, ZIP) + '  ' + mb(fs.statSync(ZIP).size))
console.log('  jar：' + mb(fs.statSync(path.join(DIST, 'life-shop.jar')).size))
console.log('  runtime：' + mb(dirSize(path.join(DIST, 'runtime'))))
