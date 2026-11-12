<template>
  <footer class="site-footer">
    <div class="container">
      <div class="features">
        <div class="feature" v-for="item in features" :key="item.title">
          <span class="icon">{{ item.icon }}</span>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>

      <div class="footer-main">
        <div class="brand-col">
          <div class="brand">
            <span class="brand-mark">悦</span>
            <strong>悦家生活</strong>
          </div>
          <p class="desc">
            专注厨房、清洁、收纳、卫浴与家居布艺的日常生活好物商城，<br />
            把「用得住、看得顺眼」的东西，挑给你。
          </p>
        </div>
        <div class="link-col">
          <h4>购物指南</h4>
          <router-link to="/list">全部商品</router-link>
          <router-link to="/cart">购物车</router-link>
          <router-link to="/orders">订单查询</router-link>
        </div>
        <div class="link-col">
          <h4>我的账户</h4>
          <router-link to="/profile">个人中心</router-link>
          <router-link to="/address">收货地址</router-link>
          <router-link to="/login">登录 / 注册</router-link>
        </div>
        <div class="link-col">
          <h4>联系我们</h4>
          <span>客服电话 400-800-1234</span>
          <span>服务时间 9:00 - 21:00</span>
          <span>邮箱 hi@yuejia-life.com</span>
        </div>
      </div>

      <div class="copyright">
        <span>© 2026 悦家生活 · 生活用品电商系统</span>
        <span>Java 8 · Spring Boot · MySQL 8.0 · Vue 3</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
const features = [
  { icon: '✅', title: '正品保障', desc: '品牌直供，假一赔十' },
  { icon: '🚚', title: '满 99 包邮', desc: '当日 16 点前下单当日发货' },
  { icon: '↩️', title: '30 天退换', desc: '不影响二次销售可退换' },
  { icon: '💬', title: '客服在线', desc: '每天 9:00 - 21:00 在线' }
]
</script>

<style scoped>
.site-footer {
  margin-top: 72px;
  background: #fff;
  border-top: 1px solid var(--line);
  padding: 8px 0 24px;
}

.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 32px 0;
  border-bottom: 1px dashed var(--line);
}

.feature {
  display: flex;
  gap: 12px;
  align-items: center;
}

.feature .icon {
  font-size: 22px;
}

.feature strong {
  font-size: 14px;
  display: block;
}

.feature p {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.footer-main {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr 1.2fr;
  gap: 32px;
  padding: 36px 0 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #ff8a5b, var(--primary));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.desc {
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--muted);
  line-height: 1.9;
}

.link-col {
  display: flex;
  flex-direction: column;
  gap: 9px;
  font-size: 13px;
  color: var(--ink-2);
}

.link-col h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 4px;
}

.link-col a:hover {
  color: var(--primary);
}

.copyright {
  display: flex;
  justify-content: space-between;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .features,
  .footer-main {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
