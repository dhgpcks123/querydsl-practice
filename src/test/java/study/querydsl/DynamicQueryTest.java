package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import javafx.beans.binding.BooleanExpression;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

/**
 * fileName    : DynamicQueryTest
 * author      : hyechan
 * date        : 2022/03/23
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/23 3:55 오후  hyechan        최초 생성
 */
@SpringBootTest
@Transactional
public class DynamicQueryTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 3:56 오후
     * @description
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond){

        //필수값 받기, 물론 FE에서 값 넣도록 유도
        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
//        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 4:07 오후
     * @description 영한쌤이 실무에서 좀 더 잘 쓰는 방법
     * BooleanBuilder는 좀 더 꼼꼼하게 살펴봐야하는데, 아래 방법은 직관적으로 보임.
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond){
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private Predicate ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

//    private List<Member> searchMember3(String usernameCond, Integer ageCond){
//        return queryFactory
//                .select(new QMemberDto(member.username, member.age)
//                .from(member)
//                .where(allEq(usernameCond, ageCond)
//                .fetch();
//    }

    // 장점 1 : 광고 상태 isValid, 날짜 in : isServicable ? 등등 조합해서 사용할 수 있다.
    // 장점 2 : 다른 쿼리에서 재활용도 된다.
//    private BooleanExpression allEq(String usernameCond, Integer ageCond){
//        usernameEq(usernameCond).and(ageEq(ageCond));
//    }
}
