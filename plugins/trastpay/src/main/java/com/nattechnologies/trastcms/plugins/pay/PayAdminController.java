package com.nattechnologies.trastcms.plugins.pay;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/admin/commerce") public class PayAdminController {
 private final PayService service; PayAdminController(PayService service){this.service=service;}
 @GetMapping("/dashboard") PayDtos.Dashboard dashboard(){return service.dashboard();}
 @GetMapping("/orders") PayDtos.PageResponse<PayDtos.OrderResponse> orders(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return service.orders(page,size);}
 @GetMapping("/orders/{id}") PayDtos.OrderResponse order(@PathVariable String id){return service.order(id);}
 @PutMapping("/orders/{id}/status") PayDtos.OrderResponse status(@PathVariable String id,@Valid@RequestBody PayDtos.StatusRequest request){return service.updateOrderStatus(id,request.status());}
 @PutMapping("/payments/{id}/status") PayDtos.OrderResponse payment(@PathVariable String id,@Valid@RequestBody PayDtos.StatusRequest request){return service.markPayment(id,request.status());}
 @GetMapping("/coupons") List<PayDtos.CouponResponse> coupons(){return service.coupons();}
 @PostMapping("/coupons") @ResponseStatus(HttpStatus.CREATED) PayDtos.CouponResponse createCoupon(@Valid@RequestBody PayDtos.CouponRequest request){return service.saveCoupon(null,request);}
 @PutMapping("/coupons/{id}") PayDtos.CouponResponse updateCoupon(@PathVariable String id,@Valid@RequestBody PayDtos.CouponRequest request){return service.saveCoupon(id,request);}
 @DeleteMapping("/coupons/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCoupon(@PathVariable String id){service.deleteCoupon(id);}
 @GetMapping("/settings") PayDtos.SettingsResponse settings(){return service.settings();}
 @PutMapping("/settings") PayDtos.SettingsResponse settings(@Valid@RequestBody PayDtos.SettingsRequest request){return service.saveSettings(request);}
}
