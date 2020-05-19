package com_imooc.miaosha.dao;

import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MiaoshaUserDao {
    @Select("select * from user where id = #{id}")
    public MiaoshaUser getById(@Param("id") long id);

    @Update("update user set password = #{password}, last_login_date = #{lastLoginDate}, " +
            "login_count = #{loginCount} where id = #{id}")
    public void update(MiaoshaUser toBeUpdate);

    @Insert("insert into user(id,nickname,password,salt,head,register_date,last_login_date,login_count) " +
            "values(#{id},#{nickname},#{password},#{salt},#{head},#{registerDate},#{lastLoginDate},#{loginCount})")
    public int insert(MiaoshaUser miaoshaUser);
}
