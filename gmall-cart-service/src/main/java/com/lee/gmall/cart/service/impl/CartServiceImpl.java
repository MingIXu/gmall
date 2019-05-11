package com.lee.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lee.gmall.bean.CartInfo;
import com.lee.gmall.cart.mapper.CartInfoMapper;
import com.lee.gmall.service.CartService;
import com.lee.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public CartInfo ifCartExist(CartInfo cartInfo) {

        boolean ifCartExist = false;

        CartInfo info = new CartInfo();
        info.setUserId(cartInfo.getUserId());
        info.setSkuId(cartInfo.getSkuId());
        return cartInfoMapper.selectOne(info);
    }

    @Override
    public void updateCart(CartInfo cartInfoDb) {
        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDb);
    }

    @Override
    public void insertCart(CartInfo cartInfo) {
        cartInfoMapper.insertSelective(cartInfo);
    }

    @Override
    public void syncCache(String userId) {
        Jedis jedis = redisUtil.getJedis();

        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);

        Map<String, String> cartInfoMap = new HashMap<>();
        for (CartInfo info : cartInfos) {
            cartInfoMap.put(info.getId(), JSON.toJSONString(info));
        }
        jedis.hmset("carts:" + userId + ":info", cartInfoMap);
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartCache(String userId) {
        Jedis jedis = redisUtil.getJedis();
        List<CartInfo> cartInfos = new ArrayList<>();

        List<String> hvals = jedis.hvals("carts:" + userId + ":info");
        if (hvals != null && hvals.size() > 0) {
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval,CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }

        return cartInfos;
    }


    @Override
    public void updateCartChecked(CartInfo cartInfo) {
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId", cartInfo.getSkuId()).andEqualTo("userId", cartInfo.getUserId());
        cartInfoMapper.updateByExampleSelective(cartInfo, example);

        syncCache(cartInfo.getUserId());

    }

}