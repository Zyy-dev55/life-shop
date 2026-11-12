package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lifeshop.common.BusinessException;
import com.lifeshop.entity.Address;
import com.lifeshop.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务
 */
@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<Address> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    public Address getDefault(Long userId) {
        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .orderByDesc(Address::getId)
                .last("LIMIT 1"));
        if (address != null) {
            return address;
        }
        List<Address> list = list(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Address save(Long userId, Address address) {
        validate(address);
        address.setId(null);
        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        if (address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return address;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Address address) {
        if (address.getId() == null) {
            throw new BusinessException("地址ID不能为空");
        }
        Address exists = getOwn(userId, address.getId());
        validate(address);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        address.setUserId(exists.getUserId());
        addressMapper.updateById(address);
    }

    public void delete(Long userId, Long id) {
        getOwn(userId, id);
        addressMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        getOwn(userId, id);
        clearDefault(userId);
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .set(Address::getIsDefault, 1)
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId));
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .set(Address::getIsDefault, 0)
                .eq(Address::getUserId, userId));
    }

    private Address getOwn(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        return address;
    }

    private void validate(Address address) {
        if (address.getReceiver() == null || address.getReceiver().trim().isEmpty()) {
            throw new BusinessException("请填写收货人");
        }
        if (address.getPhone() == null || !address.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请填写正确的手机号");
        }
        if (address.getDetail() == null || address.getDetail().trim().isEmpty()) {
            throw new BusinessException("请填写详细地址");
        }
    }
}
