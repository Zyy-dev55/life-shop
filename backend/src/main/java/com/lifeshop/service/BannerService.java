package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeshop.entity.Banner;
import com.lifeshop.mapper.BannerMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图服务
 */
@Service
public class BannerService {

    private final BannerMapper bannerMapper;

    public BannerService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<Banner> listEnabled() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort));
    }
}
