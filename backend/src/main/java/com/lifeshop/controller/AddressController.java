package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.entity.Address;
import com.lifeshop.service.AddressService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址接口
 */
@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.list(UserContext.getUserId()));
    }

    @GetMapping("/default")
    public Result<Address> getDefault() {
        return Result.success(addressService.getDefault(UserContext.getUserId()));
    }

    @PostMapping
    public Result<Address> save(@RequestBody Address address) {
        return Result.success("新增成功", addressService.save(UserContext.getUserId(), address));
    }

    @PutMapping
    public Result<Void> update(@RequestBody Address address) {
        addressService.update(UserContext.getUserId(), address);
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.getUserId(), id);
        return Result.success("设置成功", null);
    }
}
