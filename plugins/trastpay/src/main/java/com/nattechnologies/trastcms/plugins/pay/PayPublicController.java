package com.nattechnologies.trastcms.plugins.pay;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/public/commerce") public class PayPublicController {
 private final PayService service; PayPublicController(PayService service){this.service=service;}
 @PostMapping("/carts") @ResponseStatus(HttpStatus.CREATED) PayDtos.CartResponse create(@RequestParam(required=false)String currency){return service.createCart(currency);}
 @GetMapping("/carts/{token}") PayDtos.CartResponse cart(@PathVariable String token,@RequestParam(required=false)String coupon){return service.getCart(token,coupon);}
 @PostMapping("/carts/{token}/items") PayDtos.CartResponse add(@PathVariable String token,@Valid@RequestBody PayDtos.AddItemRequest request,@RequestParam(required=false)String coupon){return service.add(token,request,coupon);}
 @PutMapping("/carts/{token}/items/{itemId}") PayDtos.CartResponse update(@PathVariable String token,@PathVariable String itemId,@Valid@RequestBody PayDtos.UpdateItemRequest request,@RequestParam(required=false)String coupon){return service.update(token,itemId,request,coupon);}
 @DeleteMapping("/carts/{token}/items") PayDtos.CartResponse clear(@PathVariable String token){return service.clear(token);}
 @PostMapping("/carts/{token}/checkout") PayDtos.CheckoutResponse checkout(@PathVariable String token,@Valid@RequestBody PayDtos.CheckoutRequest request){return service.checkout(token,request);}
 @GetMapping("/gateways") List<Map<String,Object>> gateways(){return service.gateways();}
}
