package com.nattechnologies.trastcms.plugins.store;
import com.nattechnologies.trastcms.plugin.api.SellableProvider; import org.springframework.stereotype.Component; import java.util.*;
@Component public class StoreSellableProvider implements SellableProvider{
 private final StoreProductRepository products;private final StoreService service;StoreSellableProvider(StoreProductRepository p,StoreService s){products=p;service=s;}public String providerKey(){return "store";}public String pluginId(){return "traststore";}
 public Optional<SellableItem> resolve(String ref){return products.findById(ref).filter(p->p.status==ProductStatus.ACTIVE).map(p->new SellableItem(providerKey(),p.id,"STORE_PRODUCT",p.name,p.shortDescription,p.price,p.currency,false,null,p.type==ProductType.PHYSICAL,p.featuredImageUrl,!p.trackInventory||p.allowBackorder||p.stockQuantity>0,Map.of("slug",p.slug,"sku",p.sku,"productType",p.type.name())));}
 public List<SellableItem> featured(int limit){return products.findTop12ByStatusAndFeaturedTrueOrderByUpdatedAtDesc(ProductStatus.ACTIVE).stream().limit(limit).map(p->resolve(p.id).orElseThrow()).toList();}
 public void fulfill(FulfillmentRequest request){service.reserve(request.itemReference(),request.quantity(),request.orderId());}
}
