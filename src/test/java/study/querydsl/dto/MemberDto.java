package study.querydsl.dto;

import lombok.*;

/**
 * fileName    : MemberDto
 * author      : hyechan
 * date        : 2022/03/18
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/18 9:37 오후  hyechan        최초 생성
 */

public class MemberDto {

    private String username;
    private int age;

    public MemberDto(){
    }

    public MemberDto(String username, int age){
        this.username = username;
        this.age = age;
    }

    @Override
    public String toString() {
        return "MemberDto{" +
                "username='" + username + '\'' +
                ", age=" + age +
                '}';
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
