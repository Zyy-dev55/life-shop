# 网吧自行发布到 GitHub · 操作流程

> 适用场景：本机网络不好 / 还没有 GitHub 账号，把发布包拷到 U 盘，
> 到网吧（或任意能上网的电脑）解压后，照本文把项目推到自己的仓库。
>
> 发布包里的 `repo/` **已经是一个完整 git 仓库**（含 `.git` 与首次提交），
> 不需要重新 `git init`，也不需要重新整理文件。

---

## 0. 出发前先确认（在本机做一次）

- [ ] 已拿到发布包 `生活用品商城-GitHub发布包.zip`（约 30 MB）
- [ ] 包内 `repo/` 目录含 `.gitignore`、`README.md`、`backend/`、`frontend/`、`tests/`、`docs/`、`.github/`
- [ ] **包里没有论文 docx/pdf、没有 75MB 演示包、没有 118MB 图片备份**
      —— 论文正文放进公开仓库有查重风险，已按约定排除
- [ ] 准备好一个能收邮件的邮箱（注册 GitHub 用）

---

## 1. 注册 / 登录 GitHub

1. 浏览器打开 <https://github.com/>，点 **Sign up** 注册（或 Sign in 登录）。
2. 验证邮箱。
3. 记下自己的**用户名**（后面仓库地址要用）。

---

## 2. 生成一个 Personal Access Token（关键）

GitHub 现在**不允许用账号密码推送代码**，必须用工牌（PAT）代替密码。

1. 右上角头像 → **Settings**
2. 左下角 **Developer settings**
3. **Personal access tokens** → **Tokens (classic)** → **Generate new token (classic)**
4. Note 填 `life-shop-upload`，Expiration 选 **7 days**（够用且安全）
5. 勾选 **`repo`** 这一项即可
6. 点 **Generate token**，**立刻复制**保存到一个临时文本文件
   （页面关掉后就再也看不到了）

> ⚠️ 网吧是公用电脑：不要勾选"记住我"，用完在 GitHub 的 token 页面点 **Delete** 撤销，
> 退出账号并删除解压目录。

---

## 3. 新建空仓库

1. 打开 <https://github.com/new>
2. Repository name 填 `life-shop`（或自己喜欢的名字）
3. 选 **Public**（毕业设计展示通常用公开仓库；Private 也可以）
4. **不要勾选** `Add a README file`、`.gitignore`、`Choose a license`
   —— 仓库必须是空的，否则第 5 步推送会被拒绝
5. 点 **Create repository**
6. 复制页面上显示的仓库地址，形如
   `https://github.com/你的用户名/life-shop.git`

---

## 4. 解压并进入仓库目录

1. 把 U 盘里的 zip 拷到电脑上，**解压到一个不含中文和空格的路径**，
   例如 `D:\life-shop-github\`（中文路径可能导致 git 报 `Filename too long` 或乱码）
2. 进入解压后的 `repo\` 目录，地址栏输入 `cmd` 回车可直接在当前目录打开命令行；
   或右键 → **Git Bash Here**（如果装了 Git）

先检查环境：

```bat
git --version
```

- 有输出（如 `git version 2.x`）→ 走 **方案 A（命令行，推荐）**
- 提示"不是内部或外部命令" → 走 **方案 B（网页上传）** 或 **方案 C（GitHub Desktop）**

---

## 5A. 方案 A：命令行推送（推荐，保留提交历史）

在 `repo\` 目录里依次执行：

```bat
git remote add origin https://github.com/你的用户名/life-shop.git
git branch -M main
git push -u origin main
```

弹出登录时：

- 用户名：你的 **GitHub 用户名**
- 密码：**粘贴第 2 步的 PAT**（不是账号密码！）

看到 `Writing objects: 100% ...` 和 `Branch 'main' set up to track ...` 就是成功。

如果提示 `remote origin already exists`（说明这条命令重复执行过，或本机已有同名 remote），改用：

```bat
git remote set-url origin https://github.com/你的用户名/life-shop.git
git push -u origin main
```

---

## 5B. 方案 B：网页上传（网吧没装 git 时）

会**丢失提交历史**，但文件完整，适合应急。

1. 在刚建好的空仓库页面，点 **uploading an existing file**
2. 打开解压目录的 `repo\` 文件夹，按 `Ctrl+A` 全选所有文件和文件夹，
   拖到浏览器上传区
3. 注意把隐藏文件也带上：`.gitignore`、`.github\` 目录
   （Windows 资源管理器需勾选"显示隐藏的项目"）
4. 下方 Commit message 填 `Initial commit: 生活用品商城源码与回归测试`
5. 点 **Commit changes**

> 网页上传有单次文件数量限制，若文件太多上传失败，请改用方案 A 或方案 C。

---

## 5C. 方案 C：GitHub Desktop（图形界面）

1. 下载安装 <https://desktop.github.com/>
2. **File → Add Local Repository**，选择解压后的 `repo\` 目录
3. 右上角 **Publish repository**，填仓库名、取消勾选 `Keep this code private`（按需）
4. 登录时用浏览器授权，不需要手工粘 PAT

---

## 6. 发布后检查

| 检查项 | 期望结果 |
| --- | --- |
| 仓库文件列表 | 有 `backend/`、`frontend/`、`tests/`、`docs/`、`.github/workflows/ci.yml`、`.gitignore`、`README.md`、`LICENSE` |
| 没有 | 论文 docx/pdf、演示包 zip、`node_modules/`、`target/`、`dist/`、`build/`、jar |
| README | 首页能正常渲染，含运行方式与测试说明 |
| Actions | 点顶部 **Actions**，CI 首次运行（后端 41 用例 + 前端 20 用例 + 端到端 31 断言），约 3~6 分钟 |
| 仓库大小 | 31 MB 左右（主要是商品图片），不应超过 50 MB |

把 [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) 第一节的「仓库 About 一句话版」
粘到仓库右侧 **About → Description**，并在 Topics 里加上文末建议的标签。

---

## 7. 常见报错对照表

| 报错 | 原因 | 处理 |
| --- | --- | --- |
| `Authentication failed` | 用了账号密码 | 密码栏必须填 PAT |
| `remote origin already exists` | 重复执行过 `remote add` | 用 `git remote set-url origin <新地址>` |
| `! [rejected] main -> main (fetch first)` | 建仓时勾了 README / license | 新建空仓库；或 `git pull --rebase origin main` 后再 push |
| `Failed to connect / timed out` | 网吧网络限制或代理 | 换网络、关代理、或改用方案 B；也可稍后重试 |
| `Filename too long` | 路径过深或含中文 | 解压到 `D:\life-shop-github\`；执行 `git config core.longpaths true` |
| `warning: LF will be replaced by CRLF` | Windows 换行符 | 忽略即可，不影响推送 |
| 中文文件名乱码 | 解压工具编码问题 | 用 7-Zip 或 Windows 自带解压；包内文件名均为英文 |
| 仓库超过 100 MB | 误把演示包拖进去了 | 只上传 `repo\` 内的文件，排除 U 盘上的其它 zip |

---

## 8. 收尾（网吧公用电脑必做）

- [ ] GitHub → Settings → Developer settings → 删除本次生成的 PAT
- [ ] 退出 GitHub 账号，清理浏览器缓存 / 退出浏览器
- [ ] 删除解压出来的 `life-shop-github\` 目录（尤其含 PAT 的临时文本）
- [ ] 拔走 U 盘

---

## 附：本地验证命令（可选，网吧电脑若装了 JDK/Node）

```bat
cd backend && mvn test
cd frontend && npm ci && npm test
node tests/regression.mjs      :: 需先启动 demo 模式后端
node tests/diagnose.mjs        :: 分层排障体检
```

详细说明见 [TESTING.md](./TESTING.md) 与 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)。
