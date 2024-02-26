package com.example.facecheck.Enity;

public class Userinfo {
    public String name;
    public int id;
    public String gender;
    public int age;
    public boolean pass;
    public String identity_id;
    public Userinfo(){

    }

    public Userinfo(String name, int id , String gender, int age,String identity_id,boolean pass){
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.identity_id = identity_id;
        this.pass = pass;

    }
}

