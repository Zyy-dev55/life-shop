package com.lifeshop.controller;

import com.lifeshop.common.Result;
import com.lifeshop.common.UserContext;
import com.lifeshop.dto.CartAddDTO;
import com.lifeshop.dto.CartUpdateDTO;
import com.lifeshop.service.CartService;
import com.lifeshop.vo.CartVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/list")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list(UserContext.getUserId()));
    }

    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(cartService.count(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Valid CartAddDTO dto) {
        cartService.add(UserContext.getUserId(), dto);
        return Result.success("已加入购物车", null);
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestBody @Valid CartUpdateDTO dto) {
        cartService.updateQuantity(UserContext.getUserId(), dto);
        return Result.success();
    }

    @PutMapping("/checked")
    public Result<Void> updateChecked(@RequestParam Long id, @RequestParam Integer checked) {
        cartService.updateChecked(UserContext.getUserId(), id, checked);
        return Result.success();
    }

    @PutMapping("/check-all")
    public Result<Void> checkAll(@RequestParam Integer checked) {
        cartService.checkAll(UserContext.getUserId(), checked);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(UserContext.getUserId(), id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return Result.success("已清空", null);
    }
}
