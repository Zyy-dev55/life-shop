package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.dto.ConsultSendDTO;
import com.lifeshop.entity.Consult;
import com.lifeshop.entity.ConsultMessage;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.Product;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.ConsultMapper;
import com.lifeshop.mapper.ConsultMessageMapper;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.ProductMapper;
import com.lifeshop.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 咨询（客服）服务
 *
 * 数据边界：买家只能看自己的会话，商家只能看到自己店铺的会话。
 */
@Service
public class ConsultService {

    public static final String SENDER_USER = "USER";
    public static final String SENDER_MERCHANT = "MERCHANT";

    /** 单条消息最大长度 */
    private static final int MAX_LENGTH = 500;

    private final ConsultMapper consultMapper;
    private final ConsultMessageMapper messageMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public ConsultService(ConsultMapper consultMapper, ConsultMessageMapper messageMapper,
                          MerchantMapper merchantMapper, ProductMapper productMapper, UserMapper userMapper) {
        this.consultMapper = consultMapper;
        this.messageMapper = messageMapper;
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    // ------------------------------------------------------------ 买家端

    /** 我的会话列表（最近有消息的排前面） */
    public List<Consult> myList(Long userId) {
        return consultMapper.selectList(new LambdaQueryWrapper<Consult>()
                .eq(Consult::getUserId, userId)
                .orderByDesc(Consult::getLastTime)
                .orderByDesc(Consult::getId));
    }

    /** 我的未读消息总数 */
    public long myUnread(Long userId) {
        List<Consult> list = myList(userId);
        long total = 0;
        for (Consult c : list) {
            total += c.getUserUnread() == null ? 0 : c.getUserUnread();
        }
        return total;
    }

    /**
     * 打开与某店铺的会话（不发消息）：有会话就返回历史消息并清买家未读，没有就返回空会话
     * 前端「咨询商家」弹窗进入时调用
     */
    public Map<String, Object> withShop(Long userId, Long merchantId, Long productId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("店铺不存在");
        }
        Consult consult = findConversation(userId, merchantId);
        if (consult == null) {
            // 还没有聊过：返回一个未落库的会话骨架，前端可直接展示店铺信息
            Consult draft = new Consult();
            draft.setUserId(userId);
            draft.setMerchantId(merchantId);
            draft.setShopName(merchant.getName());
            draft.setShopLogo(merchant.getLogo());
            draft.setUserUnread(0);
            draft.setMerchantUnread(0);
            if (productId != null) {
                Product product = productMapper.selectById(productId);
                if (product != null) {
                    draft.setProductId(product.getId());
                    draft.setProductName(product.getName());
                    draft.setProductImage(product.getMainImage());
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("consult", draft);
            data.put("messages", new ArrayList<ConsultMessage>());
            return data;
        }
        // 从别的商品进来，更新「最近咨询的商品」
        if (productId != null && !productId.equals(consult.getProductId())) {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                consult.setProductId(product.getId());
                consult.setProductName(product.getName());
                consult.setProductImage(product.getMainImage());
                consultMapper.updateById(consult);
            }
        }
        return detail(userId, consult.getId());
    }

    /** 买家会话详情：消息列表（按时间正序），并把买家未读清零 */
    public Map<String, Object> detail(Long userId, Long consultId) {
        Consult consult = consultMapper.selectById(consultId);
        if (consult == null || !consult.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        if (consult.getUserUnread() != null && consult.getUserUnread() > 0) {
            consultMapper.update(null, new LambdaUpdateWrapper<Consult>()
                    .eq(Consult::getId, consultId)
                    .set(Consult::getUserUnread, 0));
            consult.setUserUnread(0);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("consult", consult);
        data.put("messages", messages(consultId));
        return data;
    }

    /** 买家发消息 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> send(Long userId, ConsultSendDTO dto) {
        if (dto.getMerchantId() == null) {
            throw new BusinessException("请选择要咨询的店铺");
        }
        String content = check(dto.getContent());
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException("店铺不存在");
        }
        if (merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException("店铺已停止营业，暂时无法咨询");
        }

        Consult consult = findConversation(userId, dto.getMerchantId());
        if (consult == null) {
            consult = new Consult();
            consult.setUserId(userId);
            consult.setMerchantId(merchant.getId());
            consult.setShopName(merchant.getName());
            consult.setShopLogo(merchant.getLogo());
            User user = userMapper.selectById(userId);
            consult.setUserName(user == null ? "买家" : (StringUtils.hasText(user.getNickname())
                    ? user.getNickname() : user.getUsername()));
            consult.setUserAvatar(user == null ? "" : user.getAvatar());
            consult.setUserUnread(0);
            consult.setMerchantUnread(0);
            consult.setCreateTime(LocalDateTime.now());
            consult.setUpdateTime(LocalDateTime.now());
            consultMapper.insert(consult);
        }

        // 记录本次咨询的商品（会话里只保留最近一件）
        if (dto.getProductId() != null) {
            Product product = productMapper.selectById(dto.getProductId());
            if (product != null) {
                consult.setProductId(product.getId());
                consult.setProductName(product.getName());
                consult.setProductImage(product.getMainImage());
            }
        }

        appendMessage(consult, SENDER_USER, userId, consult.getUserName(), content);
        return detail(userId, consult.getId());
    }

    // ------------------------------------------------------------ 商家端

    /** 商家会话分页（keyword 匹配买家昵称或最后消息） */
    public PageResult<Consult> merchantPage(Long merchantId, String keyword, Integer unreadOnly,
                                            long pageNum, long pageSize) {
        LambdaQueryWrapper<Consult> wrapper = new LambdaQueryWrapper<Consult>()
                .eq(Consult::getMerchantId, merchantId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Consult::getUserName, keyword)
                    .or().like(Consult::getLastContent, keyword)
                    .or().like(Consult::getProductName, keyword));
        }
        if (unreadOnly != null && unreadOnly == 1) {
            wrapper.gt(Consult::getMerchantUnread, 0);
        }
        wrapper.orderByDesc(Consult::getLastTime).orderByDesc(Consult::getId);
        Page<Consult> page = consultMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 商家未读总数（侧边栏红点） */
    public long merchantUnread(Long merchantId) {
        List<Consult> list = consultMapper.selectList(new LambdaQueryWrapper<Consult>()
                .eq(Consult::getMerchantId, merchantId).gt(Consult::getMerchantUnread, 0));
        long total = 0;
        for (Consult c : list) {
            total += c.getMerchantUnread() == null ? 0 : c.getMerchantUnread();
        }
        return total;
    }

    /** 商家查看会话（清商家未读） */
    public Map<String, Object> merchantDetail(Long merchantId, Long consultId) {
        Consult consult = requireShopConsult(merchantId, consultId);
        if (consult.getMerchantUnread() != null && consult.getMerchantUnread() > 0) {
            consultMapper.update(null, new LambdaUpdateWrapper<Consult>()
                    .eq(Consult::getId, consultId)
                    .set(Consult::getMerchantUnread, 0));
            consult.setMerchantUnread(0);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("consult", consult);
        data.put("messages", messages(consultId));
        return data;
    }

    /** 商家回复 */
    @Transactional(rollbackFor = Exception.class)
    public Consult merchantReply(Long merchantId, Long consultId, String content) {
        String text = check(content);
        Consult consult = requireShopConsult(merchantId, consultId);
        Merchant merchant = merchantMapper.selectById(merchantId);
        String name = merchant == null ? consult.getShopName() : merchant.getName();
        appendMessage(consult, SENDER_MERCHANT, merchantId, name, text);
        return consultMapper.selectById(consultId);
    }

    /** 商家主动结束/清理单个会话未读（无实际删除，仅归零） */
    public void merchantReadAll(Long merchantId) {
        consultMapper.update(null, new LambdaUpdateWrapper<Consult>()
                .eq(Consult::getMerchantId, merchantId)
                .set(Consult::getMerchantUnread, 0));
    }

    // ------------------------------------------------------------ 平台端（监管）

    /** 平台管理员：全量会话分页（keyword 匹配买家昵称 / 店铺名 / 最后消息） */
    public PageResult<Consult> adminPage(String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<Consult> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(Consult::getUserName, keyword)
                    .or().like(Consult::getShopName, keyword)
                    .or().like(Consult::getLastContent, keyword)
                    .or().like(Consult::getProductName, keyword));
        }
        wrapper.orderByDesc(Consult::getLastTime).orderByDesc(Consult::getId);
        Page<Consult> page = consultMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 平台管理员：查看任意会话详情（只读，不改变任何未读数） */
    public Map<String, Object> adminDetail(Long consultId) {
        Consult consult = consultMapper.selectById(consultId);
        if (consult == null) {
            throw new BusinessException(404, "会话不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("consult", consult);
        data.put("messages", messages(consultId));
        return data;
    }

    // ------------------------------------------------------------ 内部

    private Consult findConversation(Long userId, Long merchantId) {
        return consultMapper.selectOne(new LambdaQueryWrapper<Consult>()
                .eq(Consult::getUserId, userId)
                .eq(Consult::getMerchantId, merchantId)
                .last("limit 1"));
    }

    private Consult requireShopConsult(Long merchantId, Long consultId) {
        Consult consult = consultMapper.selectById(consultId);
        if (consult == null || consult.getMerchantId() == null || !consult.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "会话不存在或不属于当前店铺");
        }
        return consult;
    }

    private List<ConsultMessage> messages(Long consultId) {
        return messageMapper.selectList(new LambdaQueryWrapper<ConsultMessage>()
                .eq(ConsultMessage::getConsultId, consultId)
                .orderByAsc(ConsultMessage::getId));
    }

    /** 落一条消息，同时刷新会话摘要与对端未读数 */
    private void appendMessage(Consult consult, String senderType, Long senderId, String senderName, String content) {
        LocalDateTime now = LocalDateTime.now();
        ConsultMessage message = new ConsultMessage();
        message.setConsultId(consult.getId());
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setCreateTime(now);
        messageMapper.insert(message);

        int userUnread = consult.getUserUnread() == null ? 0 : consult.getUserUnread();
        int merchantUnread = consult.getMerchantUnread() == null ? 0 : consult.getMerchantUnread();
        if (SENDER_USER.equals(senderType)) {
            merchantUnread += 1;
            // 买家自己发的消息，买家侧未读直接忽略
            userUnread = 0;
        } else {
            userUnread += 1;
            merchantUnread = 0;
        }

        consult.setLastContent(content.length() > 60 ? content.substring(0, 60) + "…" : content);
        consult.setLastSender(senderType);
        consult.setLastTime(now);
        consult.setUserUnread(userUnread);
        consult.setMerchantUnread(merchantUnread);
        consult.setUpdateTime(now);
        consultMapper.updateById(consult);
    }

    private String check(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("请输入咨询内容");
        }
        String text = content.trim();
        if (text.length() > MAX_LENGTH) {
            throw new BusinessException("一次最多输入 " + MAX_LENGTH + " 字");
        }
        return text;
    }
}
