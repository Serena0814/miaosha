package com_imooc.miaosha.vo;



public class Main {
    public static void main(String[] args) {
        new Derive();
    }
}

class Base {
    private int i = 2;
    public Base() {
        this.display();
        System.out.println(this.getClass());
    }

    public void display() {
        System.out.println(this.i + "" + this.getClass());
    }

}

class Derive extends Base {
    private int i = 22;
    public Derive() {
        i = 222;
    }
}





