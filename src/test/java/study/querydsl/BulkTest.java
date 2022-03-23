package study.querydsl;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;
/**
 * fileName    : BulkTest
 * author      : hyechan
 * date        : 2022/03/23
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/23 4:27 오후  hyechan        최초 생성
 */
@SpringBootTest
@Transactional
public class BulkTest {
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

    // 여러 건 한번에 수정, 삭제 (벌크)

    /**
     * @author: hyechan
     * @since: 2022/03/23 4:28 오후
     * @description
     */
    @Test
//    @Commit
    public void bulkUpdate() throws Exception{
        //member1 => 10 //비회원
        //member2 => 20 //비회원
        //member3, 4는 유지
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        //bulk연산 주의해야할 것,
        //영속성컨텍스트에 1,2,3,4 다 올라가 있음.
        //bulk연산하면 db에 데이터는 바로 바뀐다.
        //근데 영속성컨텍스트에는 그냥 그대로 남아있음.
        //즉 영속성컨텍스트(1차 캐시) username과 db username 정보가 다른 현상이 나타남.
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member : result) {
            System.out.println("member = " + member);
        }
        // 망
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 4:42 오후
     * @description
     */
    @Test
    public void bulkAdd() throws Exception{
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
//                .set(member.age, member.age.add(-1))
//                .set(member.age, member.age.multiply(2))
                .execute();
    }
    /**
     * @author: hyechan
     * @since: 2022/03/23 4:44 오후
     * @description
     */
    @Test
    public void bulkDelete() throws Exception{
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 4:55 오후
     * @description
     */
    @Test
    public void sqlFunction() throws Exception{
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M")
                )
                .from(member)
                .fetch();
        //H2Dialect에 등록 되어 있어야함.
//        H2Dialect
        //db에서 직접만든 function쓰고 싶다면? dialect상속한 거 등록해서 사용하여야 함.
        for (String r : result) {
            System.out.println("r = " + r);
        }
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 5:00 오후
     * @description
     */
    @Test
    public void sqlFunction2() throws Exception{
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(Expressions.stringTemplate(
                        "function('lower', {0})", member.username
                        ))
                )
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }

        List<String> results = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String result : results) {
            System.out.println("result = " + result);
        }
        //기본적인 거 그냥 제공해준다. 예제는 진짜 별로인데 이렇게 쓴다는 것만 알아두셈.
    }
}
