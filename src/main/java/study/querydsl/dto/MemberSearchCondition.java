package study.querydsl.dto;

import lombok.Data;

/**
 * fileName    : MemberSearchCondition
 * author      : hyechan
 * date        : 2022/03/23
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/23 5:56 오후  hyechan        최초 생성
 */
@Data
public class MemberSearchCondition {
    // 회원명, 팀명, 나이(ageGoe, ageLoe)


    private String username;
    private String teamName;
    private Integer ageGoe; //Integer쓰는 이유? null일 수 있어서
    private Integer ageLoe;
}

