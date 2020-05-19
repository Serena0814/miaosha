package com_imooc.miaosha.service;

import com_imooc.miaosha.dao.MiaoshaUserDao;
import com_imooc.miaosha.dao.UserDao;
import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    MiaoshaUserDao userDao;

    public MiaoshaUser getById(int id){
        return userDao.getById(id);
    }

    @Transactional
    public boolean tx(){
        MiaoshaUser user1 = new MiaoshaUser();
        user1.setId(222L);
        user1.setNickname("sandy");
        userDao.insert(user1);

        MiaoshaUser user2 = new MiaoshaUser();
        user2.setId(1L);
        user2.setNickname("lili");
        userDao.insert(user2);

        return true;
    }
}
