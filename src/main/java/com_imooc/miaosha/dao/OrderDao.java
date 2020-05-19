package com_imooc.miaosha.dao;

import com_imooc.miaosha.domain.MiaoshaOrder;
import com_imooc.miaosha.domain.OrderInfo;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;


@Repository
@Mapper
public interface OrderDao {

    @Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId") long userId, @Param("goodsId") long goodsId);

    /**
     * 关于@SelectKey的用法:
     * 1.statement是要运行的SQL语句，它的返回值通过resultType来指定；
     * 2.before=true表示先执行statement再插入，false反之；
     * 3.keyProperty表示statement的执行结果赋值给代码中的哪个对象，keyColumn表示将执行结果赋值给数据库表中哪一列，相当于更改数据库；
     * 4.selectKey的两大作用：1、生成主键；2、获取刚刚插入数据的主键。
     * 5.使用selectKey，并且使用MySQL的last_insert_id()函数时，before必为false，也就是说必须先插入然后执行last_insert_id()才能获得刚刚插入数据的ID。
     */
    @Insert("insert into order_info(user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)values("
            + "#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    @SelectKey(keyColumn = "id", keyProperty = "id", resultType = long.class, before = false, statement = "select last_insert_id()")//把上次插入的id返回
    public long insert(OrderInfo orderInfo); // 这个返回值是主键id

    @Insert("insert into miaosha_order (user_id, goods_id, order_id)values(#{userId}, #{goodsId}, #{orderId})")
    public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder); // 这个返回值是插入的条数

    @Select("select * from order_info where id = #{orderId}")
    public OrderInfo getOrderById(@Param("orderId") long orderId);

    @Delete("truncate order_info")
    public void clearOrders();

    @Delete("truncate miaosha_order")
    public void clearMiaoshaOrders();
}
