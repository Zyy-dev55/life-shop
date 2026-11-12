/**
 * 商品数据源（种子数据与图片抓取共用这一份，避免两边不一致）
 * keyword 仅用于 scripts/fetch-images.js 去搜商品图，不写入数据库
 */
const CATEGORIES = [
  { id: 1, name: '厨房烹饪', icon: '🍳', sort: 1 },
  { id: 2, name: '清洁除菌', icon: '🧽', sort: 2 },
  { id: 3, name: '收纳整理', icon: '📦', sort: 3 },
  { id: 4, name: '卫浴洗漱', icon: '🛁', sort: 4 },
  { id: 5, name: '家纺床品', icon: '🛏️', sort: 5 },
  { id: 6, name: '家居日用', icon: '🏠', sort: 6 },
]

/**
 * 入驻商家：一个商家经营一个分类，商品按 categoryId 归属到店铺。
 * userId 对应 t_user 里的商家账号（role = MERCHANT）。
 */
const MERCHANTS = [
  {
    id: 1, userId: 6, username: 'shop_kitchen', password: '123456', categoryId: 1,
    name: '悦家厨房旗舰店', contact: '陈明', phone: '13700000001',
    address: '浙江省杭州市余杭区仓前街道文一西路 1218 号 3 号楼',
    description: '专注中式厨房十年，锅具、刀具、保鲜容器一站买齐。自有仓储，48 小时内发货。',
  },
  {
    id: 2, userId: 7, username: 'shop_clean', password: '123456', categoryId: 2,
    name: '洁净家清专营店', contact: '刘芳', phone: '13700000002',
    address: '广东省广州市白云区云城东路 88 号 A 栋',
    description: '做真正好用的清洁用品。油污、除菌、除螨，按场景给你配好方案。',
  },
  {
    id: 3, userId: 8, username: 'shop_storage', password: '123456', categoryId: 3,
    name: '收纳研究所', contact: '周航', phone: '13700000003',
    address: '江苏省苏州市工业园区星湖街 328 号 6 号厂房',
    description: '小户型收纳解决方案提供商。每一件产品都先在自己家里用满三个月再上架。',
  },
  {
    id: 4, userId: 9, username: 'shop_bath', password: '123456', categoryId: 4,
    name: '暖居卫浴馆', contact: '吴静', phone: '13700000004',
    address: '福建省泉州市南安市仑苍镇卫浴产业园 12 号',
    description: '卫浴五金与家纺洗护，主打恒温、增压、速干，让洗澡变成享受。',
  },
  {
    id: 5, userId: 10, username: 'shop_bedding', password: '123456', categoryId: 5,
    name: '眠光家纺', contact: '赵一鸣', phone: '13700000005',
    address: '江苏省南通市通州区川姜镇家纺城 B 区 15 号',
    description: '来自家纺之都南通，纯棉四件套、枕头、凉席，只做 A 类母婴级面料。',
  },
  {
    id: 6, userId: 11, username: 'shop_daily', password: '123456', categoryId: 6,
    name: '日用百货铺', contact: '孙倩', phone: '13700000006',
    address: '浙江省义乌市国际商贸城二区 3 楼 18 街',
    description: '过日子要用的零碎小物，纸巾、雨伞、衣架、香薰，一件也能发。',
  },
]

/** 商品按分类归属店铺 */
function merchantIdOfCategory(categoryId) {
  const m = MERCHANTS.find((x) => x.categoryId === Number(categoryId))
  return m ? m.id : 1
}

const PRODUCTS = [
  // ---------------- 厨房烹饪 ----------------
  {
    id: 1, categoryId: 1, name: '麦饭石不粘炒锅 30cm',
    subtitle: '少油不粘 · 燃气电磁炉通用 · 附赠木铲',
    price: 129, originalPrice: 199, stock: 126, sales: 862, unit: '口',
    keyword: '麦饭石不粘炒锅 商品图',
    detail: '锅体采用麦饭石复合涂层，煎蛋不放油也能整片揭起；加深 9cm 锅身，炒菜不易外溅。加厚锅底导热均匀，燃气灶、电磁炉通用。手柄为隔热防烫木纹设计，单手握持不打滑。',
  },
  {
    id: 2, categoryId: 6, name: '316 不锈钢保温杯 500ml',
    subtitle: '24 小时锁温 · 内胆无缝 · 一键弹盖',
    price: 79, originalPrice: 129, stock: 340, sales: 1245, unit: '只',
    keyword: '不锈钢保温杯 商品图',
    detail: '316 不锈钢内胆，无焊接缝隙不易藏污。真空双层结构，95℃ 热水 12 小时后仍有 62℃。杯盖一键弹开，单手可开，通勤、带娃、开车都方便。',
  },
  {
    id: 3, categoryId: 1, name: '竹纤维洗碗布 5 片装',
    subtitle: '不沾油 · 速干不臭 · 可机洗',
    price: 19.9, originalPrice: 29.9, stock: 800, sales: 2310, unit: '包',
    keyword: '竹纤维洗碗布 厨房抹布 商品图',
    detail: '竹纤维材质，油污一冲即掉，不用洗洁精也能擦净轻度油渍。加厚双层织法，吸水后柔软有弹性，擦灶台、擦碗都不留水痕。速干不易滋生异味。',
  },
  {
    id: 4, categoryId: 1, name: '北欧陶瓷餐具 16 件套',
    subtitle: '釉下彩 · 可进微波炉洗碗机 · 4 人份',
    price: 238, originalPrice: 358, stock: 88, sales: 431, unit: '套',
    keyword: '陶瓷餐具套装 碗盘 商品图',
    detail: '16 件包含 4 碗 4 盘 4 汤勺 4 筷架，刚好满足四口之家日常使用。釉下彩工艺，图案在釉层之下，不易磨损掉色。加厚瓷体，微波炉、烤箱、洗碗机均可使用。',
  },
  {
    id: 5, categoryId: 2, name: '厨房重油污清洁剂 500ml',
    subtitle: '泡沫型 · 免刷洗 · 附赠喷头',
    price: 29.9, originalPrice: 49.9, stock: 560, sales: 1876, unit: '瓶',
    keyword: '厨房重油污清洁剂 商品图',
    detail: '喷出即膨胀成泡沫，附着在油烟机、灶台表面 3 分钟，油垢软化后用抹布一擦即净。配方温和不刺鼻，500ml 大容量，一瓶约可用 2 个月。',
  },
  {
    id: 6, categoryId: 1, name: '食品级硅胶铲 6 件套',
    subtitle: '不伤锅涂层 · 耐 230℃ · 带收纳桶',
    price: 59, originalPrice: 89, stock: 210, sales: 654, unit: '套',
    keyword: '硅胶铲 厨具套装 商品图',
    detail: '食品级硅胶包覆，边缘柔软，用在麦饭石、不粘锅上不划伤涂层。耐温 -40℃ 至 230℃，炒菜、烘焙都能用。一体成型无缝隙，附赠沥水收纳桶。',
  },
  {
    id: 19, categoryId: 1, name: '多功能厨房剪刀',
    subtitle: '带磁吸刀套 · 剪骨不费力 · 可拆洗',
    price: 39, originalPrice: 59, stock: 260, sales: 742, unit: '把',
    keyword: '厨房剪刀 多功能 商品图',
    detail: '加厚不锈钢刀刃，剪鸡骨、剪葱花都利落，中间带凹槽可夹核桃。手柄防滑贴合，右手左手都好握；刀身可拆开单独清洗，避免缝隙藏污。',
  },
  {
    id: 20, categoryId: 1, name: '天然竹砧板 大号',
    subtitle: '双面可用 · 带沥水槽 · 不易发霉',
    price: 69, originalPrice: 99, stock: 180, sales: 1305, unit: '块',
    keyword: '竹砧板 切菜板 商品图',
    detail: '整竹拼接，密度高不易吸水，南方潮湿环境也相对不容易发霉。一面切菜一面切水果，边缘有导流槽，切肉汁水不会流到台面。',
  },
  {
    id: 21, categoryId: 1, name: '玻璃保鲜盒 4 件套',
    subtitle: '可微波可冷冻 · 密封扣盖 · 可叠放',
    price: 55, originalPrice: 89, stock: 320, sales: 892, unit: '套',
    keyword: '玻璃保鲜盒 密封 商品图',
    detail: '高硼硅玻璃，耐 -20℃ 至 400℃，冰箱拿出来可以直接进微波炉。四扣密封盖，汤汁不侧漏，装便当带通勤也不怕洒。四种尺寸可叠放，冰箱更整齐。',
  },
  {
    id: 22, categoryId: 1, name: '双层不锈钢蒸锅 28cm',
    subtitle: '一锅多用 · 燃气电磁炉通用 · 可视锅盖',
    price: 149, originalPrice: 229, stock: 90, sales: 468, unit: '口',
    keyword: '不锈钢蒸锅 双层 商品图',
    detail: '304 不锈钢锅身，双层设计上蒸下煮，早餐一锅搞定包子加鸡蛋。加厚复合锅底，受热均匀不糊底，燃气灶、电磁炉都能用。',
  },

  // ---------------- 清洁除菌 ----------------
  {
    id: 7, categoryId: 2, name: '免手洗平板拖把套装',
    subtitle: '单手刮水 · 干湿两用 · 含 2 块拖布',
    price: 89, originalPrice: 139, stock: 175, sales: 942, unit: '套',
    keyword: '免手洗平板拖把 商品图',
    detail: '推拉式刮水结构，拖完按一下就能把脏水刮进桶里，全程不沾手。平板拖头 360° 旋转，沙发底、床底都能探进去。附赠 2 块超细纤维拖布。',
  },
  {
    id: 8, categoryId: 2, name: '洗衣机槽清洁泡腾片 12 片',
    subtitle: '除垢去味 · 滚筒波轮通用 · 一片一次',
    price: 25, originalPrice: 39, stock: 620, sales: 1503, unit: '盒',
    keyword: '洗衣机槽清洁泡腾片 商品图',
    detail: '一片一次，丢进空桶选标准洗即可。泡腾配方深入内筒夹层，分解洗衣液残留与水垢，减少异味来源。滚筒、波轮洗衣机通用。',
  },
  {
    id: 23, categoryId: 2, name: '洗衣凝珠 50 颗',
    subtitle: '深层去渍 · 持久留香 · 三合一',
    price: 45, originalPrice: 79, stock: 500, sales: 2103, unit: '盒',
    keyword: '洗衣凝珠 商品图',
    detail: '一颗搞定洗净、柔顺、留香，不用再单独倒柔顺剂。水溶膜遇水即化，冷水也能完全溶解不残留。50 颗装约可用两个月。',
  },
  {
    id: 24, categoryId: 2, name: '加厚手提垃圾袋 100 只',
    subtitle: '自动收口 · 加厚不漏 · 家用大号',
    price: 19.9, originalPrice: 32, stock: 900, sales: 3420, unit: '卷',
    keyword: '垃圾袋 手提 商品图',
    detail: '加厚 HDPE 材质，装满厨余也不易破底。手提抽绳设计，一拉即收口，拎下楼不脏手。100 只分 5 卷，断点清晰好撕。',
  },
  {
    id: 25, categoryId: 2, name: '75% 酒精消毒湿巾 80 抽',
    subtitle: '家用办公 · 不伤手 · 抽取式',
    price: 22, originalPrice: 35, stock: 600, sales: 1877, unit: '包',
    keyword: '消毒湿巾 商品图',
    detail: '75% 酒精配比，擦门把、擦手机、擦办公桌都合适。加厚无纺布不易破，含水量足，抽出来不干。含保湿成分，频繁使用手也不紧绷。',
  },
  {
    id: 26, categoryId: 2, name: '磁吸双面玻璃刮水器',
    subtitle: '高层适用 · 双面同步擦 · 可拆洗',
    price: 49, originalPrice: 79, stock: 260, sales: 634, unit: '个',
    keyword: '擦玻璃神器 刮水器 商品图',
    detail: '内外磁吸同步移动，站在屋里就能擦到窗外那面。刮条贴合玻璃，一遍刮过不留水痕。配安全绳，高层使用更放心。',
  },
  {
    id: 27, categoryId: 2, name: '除螨喷雾 500ml',
    subtitle: '免洗 · 床品沙发可用 · 淡淡清香',
    price: 35, originalPrice: 59, stock: 420, sales: 1052, unit: '瓶',
    keyword: '除螨喷雾 商品图',
    detail: '喷在床垫、沙发、地毯表面，静置晾干即可，不用再洗一遍。适合换季、梅雨季，家里有宠物也可以定期用。气味清淡不刺激。',
  },

  // ---------------- 收纳整理 ----------------
  {
    id: 9, categoryId: 3, name: '无痕强力粘钩 20 枚',
    subtitle: '承重 5kg · 免打孔 · 撕下不留胶',
    price: 15.9, originalPrice: 25.9, stock: 900, sales: 2765, unit: '盒',
    keyword: '无痕粘钩 强力挂钩 商品图',
    detail: '加厚背胶，贴墙 24 小时后承重可达 5kg，挂锅铲、浴球、钥匙都稳。撕下时用吹风机热风吹 30 秒，可整片揭下不留胶痕，租房友好。',
  },
  {
    id: 12, categoryId: 3, name: '真空压缩收纳袋 8 件套',
    subtitle: '含手泵 · 节省 70% 空间 · 防潮防虫',
    price: 40, originalPrice: 69, stock: 380, sales: 1102, unit: '套',
    keyword: '真空压缩收纳袋 商品图',
    detail: '8 件套含 3 大 3 中 2 小，换季被褥、羽绒服都能压。双轨密封条 + 防漏气阀，压好后可维持数月不进气。附赠手泵，出差、露营也能用。',
  },
  {
    id: 28, categoryId: 3, name: '抽屉分隔收纳盒 6 件套',
    subtitle: '自由组合 · 桌面衣柜通用 · 透明可视',
    price: 32, originalPrice: 49, stock: 480, sales: 1560, unit: '套',
    keyword: '抽屉收纳盒 分隔 商品图',
    detail: '六种尺寸可自由拼进抽屉，袜子、内衣、文具各归各位。透明盒身一眼看到内容物，不用翻箱倒柜。边缘圆润，抽拉不刮手。',
  },
  {
    id: 29, categoryId: 3, name: '免钉多层鞋架 4 层',
    subtitle: '承重 20kg · 门口窄位可用 · 可叠高',
    price: 79, originalPrice: 129, stock: 210, sales: 720, unit: '个',
    keyword: '多层鞋架 简易 商品图',
    detail: '免工具组装，卡扣一按就成型。4 层可放 12 双常穿鞋，层高适配运动鞋与短靴。窄身设计，门后、玄关角落都塞得下。',
  },
  {
    id: 30, categoryId: 3, name: '冰箱收纳保鲜盒 5 件套',
    subtitle: '透明可视 · 可叠放 · 冰箱更整齐',
    price: 45, originalPrice: 69, stock: 340, sales: 981, unit: '套',
    keyword: '冰箱收纳盒 保鲜 商品图',
    detail: '五种尺寸对应水果、鸡蛋、剩菜、调料，冰箱分层不再乱。透明盒身加手柄，拿出来一眼看清。可叠放，纵向空间也利用起来。',
  },
  {
    id: 31, categoryId: 3, name: '可折叠洗衣篮',
    subtitle: '大容量 · 手提防滑 · 不用时折起来',
    price: 39, originalPrice: 59, stock: 300, sales: 655, unit: '个',
    keyword: '脏衣篮 可折叠 商品图',
    detail: '70L 大容量，一家三口一周的换洗衣服装得下。两侧提手加厚，装满拎着不勒手。不用时压扁收进柜缝，卫生间不占地。',
  },
  {
    id: 32, categoryId: 3, name: '三层置物推车',
    subtitle: '带刹车轮 · 厨房浴室通用 · 承重 15kg',
    price: 119, originalPrice: 189, stock: 150, sales: 508, unit: '台',
    keyword: '置物推车 三层 商品图',
    detail: '三层大容量，厨房放调料、浴室放洗护、书房放文具都合适。万向轮带刹车，推到哪停到哪。镂空篮身透气，湿区用也不积水。',
  },
  {
    id: 33, categoryId: 3, name: '挂式真空压缩袋 5 件套',
    subtitle: '直接挂衣柜 · 免抽气 · 防尘防潮',
    price: 59, originalPrice: 89, stock: 260, sales: 433, unit: '套',
    keyword: '挂式压缩袋 收纳 商品图',
    detail: '压好之后可以直接挂在衣柜杆上，羽绒服、大衣一件一袋。免抽气设计，卷压排气就行，出差酒店也能用。透明视窗，找衣服不用全拆。',
  },

  // ---------------- 卫浴洗漱 ----------------
  {
    id: 10, categoryId: 4, name: '一次性洗脸巾 80 抽',
    subtitle: '纯棉加厚 · 干湿两用 · 抽取式',
    price: 24.9, originalPrice: 39.9, stock: 700, sales: 1988, unit: '包',
    keyword: '一次性洗脸巾 棉柔巾 商品图',
    detail: '100% 纯棉水刺无纺布，厚度比普通洗脸巾高 30%，湿水后不易破。珍珠纹面用于清洁，平纹面用于擦干，一片可替代毛巾使用。',
  },
  {
    id: 13, categoryId: 4, name: '纯棉毛巾 3 条装',
    subtitle: '32 支长绒棉 · 吸水速干 · 亲肤不掉毛',
    price: 45, originalPrice: 69, stock: 520, sales: 1674, unit: '组',
    keyword: '纯棉毛巾 洗脸毛巾 商品图',
    detail: '32 支长绒棉，克重 420g/㎡，摸起来厚实柔软。毛圈密度高，吸水快也干得快，浴室挂一晚基本干透。3 条装可区分洗脸、洗澡、擦手。',
  },
  {
    id: 14, categoryId: 4, name: '纯棉加厚浴巾 70x140',
    subtitle: '吸水快干 · 可裹全身 · 亲肤柔软',
    price: 59.9, originalPrice: 89, stock: 300, sales: 786, unit: '条',
    keyword: '纯棉浴巾 加厚 商品图',
    detail: '70×140cm 加大尺寸，成年人可裹住全身。加厚毛圈吸水量约为自重的 4 倍，洗完澡一裹即干。多次水洗后仍保持柔软，不易变硬。',
  },
  {
    id: 15, categoryId: 4, name: '浴室防滑拖鞋',
    subtitle: 'EVA 一体成型 · 防滑底纹 · 速干不臭',
    price: 35, originalPrice: 59, stock: 450, sales: 1398, unit: '双',
    keyword: '浴室防滑拖鞋 商品图',
    detail: 'EVA 一体成型，无缝隙不藏水，洗完澡踩一会儿就干。鞋底深纹路抓地，湿瓷砖上也不打滑。轻至 180g，久穿不累脚。',
  },
  {
    id: 16, categoryId: 4, name: '陶瓷漱口杯 2 只装',
    subtitle: '简约哑光 · 可放牙刷 · 易清洗',
    price: 26, originalPrice: 39, stock: 260, sales: 523, unit: '组',
    keyword: '漱口杯 牙刷杯 陶瓷 商品图',
    detail: '哑光釉面手感细腻，杯口圆润不割嘴。杯身高度可放常规牙刷，一家两口各一只不混用。瓷质厚实，日常使用很稳。',
  },
  {
    id: 34, categoryId: 4, name: '增压淋浴花洒',
    subtitle: '三档调节 · 一键止水 · 易安装',
    price: 79, originalPrice: 129, stock: 280, sales: 1129, unit: '个',
    keyword: '淋浴花洒 增压 商品图',
    detail: '细密出水孔增压，高层水压小也能洗得舒服。三档模式切换，按摩档冲肩颈很解乏。手柄一键止水，抹沐浴露时不用反复开关水阀。',
  },
  {
    id: 35, categoryId: 4, name: '浴室置物架 免打孔',
    subtitle: '双层太空铝 · 防锈 · 承重 10kg',
    price: 69, originalPrice: 109, stock: 240, sales: 830, unit: '个',
    keyword: '浴室置物架 免打孔 商品图',
    detail: '免打孔背胶安装，租房也能放心用。太空铝材质不生锈，长期潮气环境也不掉漆。双层加护栏，洗发水沐浴露摆满不滑落。',
  },
  {
    id: 36, categoryId: 4, name: '硅藻泥吸水地垫',
    subtitle: '速干 · 浴室门口 · 不打滑',
    price: 45, originalPrice: 75, stock: 360, sales: 1244, unit: '块',
    keyword: '硅藻泥地垫 浴室 商品图',
    detail: '踩上去几秒吸干脚底水，浴室门口不再一片湿。表面磨砂防滑，湿脚踩也不打滑。脏了用清水冲一冲，晾干继续用。',
  },

  // ---------------- 家纺床品 ----------------
  {
    id: 17, categoryId: 5, name: '天然乳胶枕 枕芯',
    subtitle: '93% 乳胶含量 · 透气孔设计 · 护颈曲线',
    price: 159, originalPrice: 259, stock: 140, sales: 467, unit: '只',
    keyword: '天然乳胶枕 枕芯 商品图',
    detail: '93% 天然乳胶含量，回弹快、支撑足，睡醒不易塌。表面密集透气孔，夏天不闷汗。波浪曲线设计贴合颈部，仰睡侧睡都有支撑。',
  },
  {
    id: 18, categoryId: 5, name: '全棉四件套 1.8m 床',
    subtitle: '60 支长绒棉 · 亲肤透气 · 四季可用',
    price: 269, originalPrice: 399, stock: 96, sales: 312, unit: '套',
    keyword: '四件套 全棉床上用品 商品图',
    detail: '60 支长绒棉，密度 133×72，摸起来细腻不扎。活性印染，色牢度高，多次水洗不易褪色。含被套 1、床单 1、枕套 2，适配 1.8m 床。',
  },
  {
    id: 37, categoryId: 5, name: '水洗棉夏凉被',
    subtitle: '可机洗 · 空调房适用 · 亲肤不闷',
    price: 129, originalPrice: 199, stock: 180, sales: 690, unit: '床',
    keyword: '夏凉被 水洗棉 商品图',
    detail: '水洗棉面料，柔软带一点自然的褶皱感，直接盖不扎人。填充轻薄，空调房 26℃ 盖着刚好不闷汗。整条可机洗，晾干就能用。',
  },
  {
    id: 38, categoryId: 5, name: '床垫保护垫',
    subtitle: '防水防螨 · 可拆洗 · 四周松紧带',
    price: 99, originalPrice: 159, stock: 220, sales: 470, unit: '条',
    keyword: '床垫保护垫 防水 商品图',
    detail: '表层纯棉亲肤，中间防水膜挡住汗渍、尿渍，床垫用几年还干净。四周松紧带包边，翻身不跑位。可整条机洗，家里有小孩宠物很实用。',
  },
  {
    id: 39, categoryId: 5, name: '抗菌枕巾 2 条装',
    subtitle: '纯棉 · 吸汗透气 · 换洗方便',
    price: 35, originalPrice: 55, stock: 400, sales: 866, unit: '组',
    keyword: '枕巾 纯棉 商品图',
    detail: '枕巾比换枕套省事，出汗多的人建议一周一换。纯棉材质吸汗透气，抗菌处理减少异味。2 条装方便替换。',
  },
  {
    id: 40, categoryId: 5, name: '针织沙发盖毯',
    subtitle: '130x180 · 四季可用 · 可机洗',
    price: 89, originalPrice: 139, stock: 200, sales: 545, unit: '条',
    keyword: '沙发盖毯 针织 商品图',
    detail: '针织纹理有质感，搭在沙发上就是装饰，追剧时随手一盖。130×180 够两个人分。可机洗，不起球不缩水。',
  },
  {
    id: 41, categoryId: 5, name: '遮光窗帘 2 片装',
    subtitle: '隔热降噪 · 遮光率 90% · 免打孔可选',
    price: 159, originalPrice: 259, stock: 160, sales: 388, unit: '套',
    keyword: '遮光窗帘 卧室 商品图',
    detail: '三层复合织造，遮光率约 90%，上夜班白天补觉也不刺眼。布身有垂感，挂起来褶皱好看。可选免打孔伸缩杆安装，租房友好。',
  },
  {
    id: 42, categoryId: 5, name: '冰丝凉席三件套',
    subtitle: '可折叠 · 亲肤凉爽 · 含枕套 2 只',
    price: 139, originalPrice: 219, stock: 140, sales: 402, unit: '套',
    keyword: '冰丝凉席 三件套 商品图',
    detail: '冰丝纤维贴身凉爽但不冰人，比竹席更适合空调房。可折叠收纳，换季塞进柜子不占地。含凉席 1 张、枕套 2 只。',
  },

  // ---------------- 家居日用 ----------------
  {
    id: 11, categoryId: 6, name: '维达抽纸 24 包整箱',
    subtitle: '3 层加厚 · 超韧不易破 · 家庭装',
    price: 49.9, originalPrice: 79.9, stock: 460, sales: 3120, unit: '箱',
    keyword: '抽纸 家用 纸巾 商品图',
    detail: '3 层加厚压花，湿水后仍不易破，擦手擦桌都够用。24 包整箱家庭装，客厅、卧室、车里各放一包。原生木浆，无荧光增白剂。',
  },
  {
    id: 43, categoryId: 6, name: '全自动晴雨伞',
    subtitle: '十骨加固 · 防紫外线 · 一按即开',
    price: 59, originalPrice: 99, stock: 350, sales: 1420, unit: '把',
    keyword: '全自动雨伞 折叠 商品图',
    detail: '十骨加固伞架，大风天不容易翻伞。伞布带黑胶涂层，晴天遮阳防紫外线。一键自动开收，上车、抱娃时单手就能操作。',
  },
  {
    id: 44, categoryId: 6, name: '无火香薰 200ml',
    subtitle: '客厅卧室 · 持久扩香 · 含藤条',
    price: 69, originalPrice: 109, stock: 300, sales: 977, unit: '瓶',
    keyword: '无火香薰 香氛 商品图',
    detail: '藤条自然挥发，不用点火也不用插电，家里有小孩宠物更安心。200ml 约可散香 2-3 个月，玄关、卫生间放一瓶进门就有好味道。',
  },
  {
    id: 45, categoryId: 6, name: '桌面加湿器',
    subtitle: '静音 · USB 供电 · 带小夜灯',
    price: 59, originalPrice: 99, stock: 280, sales: 1130, unit: '台',
    keyword: '桌面加湿器 小型 商品图',
    detail: '超声波雾化，出雾细腻不积水珠，办公桌、床头柜都合适。运行声音很轻，睡觉开着不吵。带暖光小夜灯，起夜不用摸黑开大灯。',
  },
  {
    id: 46, categoryId: 6, name: '防滑衣架 20 个装',
    subtitle: '无痕 · 干湿两用 · 承重 5kg',
    price: 25, originalPrice: 39, stock: 700, sales: 2210, unit: '组',
    keyword: '防滑衣架 无痕 商品图',
    detail: '肩部带防滑齿，针织衫、真丝裙挂上去不往下滑。加厚材质，冬天挂大衣也不变形。干湿两用，阳台晾完直接挂回衣柜。',
  },
  {
    id: 47, categoryId: 6, name: '除湿盒 6 盒装',
    subtitle: '衣柜鞋柜 · 可重复使用 · 无香味',
    price: 29, originalPrice: 45, stock: 520, sales: 1655, unit: '组',
    keyword: '除湿盒 衣柜 商品图',
    detail: '氯化钙吸湿，梅雨季衣柜、鞋柜放一盒，衣服不再潮乎乎。吸满水后倒掉，晒干或微波加热可重复使用。无香型，不串味。',
  },
  {
    id: 48, categoryId: 6, name: '静音挂钟 12 英寸',
    subtitle: '简约北欧 · 扫描机芯 · 夜间不吵',
    price: 69, originalPrice: 109, stock: 190, sales: 512, unit: '个',
    keyword: '挂钟 静音 简约 商品图',
    detail: '扫描机芯无滴答声，卧室、书房挂着不扰人。12 英寸表盘数字清晰，客厅沙发上看时间也够大。哑光边框，配原木、奶油风都好看。',
  },
]

module.exports = { CATEGORIES, PRODUCTS, MERCHANTS, merchantIdOfCategory }
