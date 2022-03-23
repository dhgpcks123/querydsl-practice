package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * fileName    : QuerydslAdvancedTest
 * author      : hyechan
 * date        : 2022/03/18
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/18 9:27 오후  hyechan        최초 생성
 */
@SpringBootTest
@Transactional
public class QuerydslAdvancedTest {

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
     * @since: 2022/03/18 9:28 오후
     * @description
     */
    @Test
    public void 단일프로젝션() throws Exception{
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        List<Member> results = queryFactory
                .select(member) //이것도 proejction 대상 하나인 것
                .from(member)
                .fetch();

        for (Member s : results) {
            System.out.println("s = " + s);
        }
    }


    /**
     * @author: hyechan
     * @since: 2022/03/18 9:30 오후
     * @description
     */
    @Test
    public void 튜플_프로젝션() throws Exception{
        List<Tuple> results = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : results) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
            System.out.println("username = " + username);
        }
        //tuple 계층안에서만 쓰셈. 밖으로 던지는 건 dto로 바꿔서 던져.
        //querydsl 추가한다고 컨트롤러 다 바꿀 수 없잖어.
        //튜플 쿼리디에스엘 쓴다고 씀.
        //dto로 바로 꺼내오는 거 배울거야. 대부분 이렇게 꺼내고 쓰게되겠지
    }

// ---------------------------  PROJECTIONS --------------------

    /**
     * @author: hyechan
     * @since: 2022/03/18 9:36 오후
     * @description: 순수 JPA에서 dto반환은?
     */
    @Test
    public void 프로젝션_dto반환_JPQA() throws Exception{
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }
    
    /**
     * @author: hyechan
     * @since: 2022/03/18 10:23 오후
     * @description //setter 이것은 딱 답이 없습니다.
     * 실용적인 관점에서는 @QueryProjection이 가장 편리하고 좋습니다.
     * Projections.bean조회가 안된다? setter하는건데 @Data롬복 줬는데 안들어가네..
     * fields(getter,setter) 없어도 됨.
     */
    @Test
    public void findDto_쿼리DSL () throws Exception{
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age
                ))
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    //constructor, 다만 타입을 맞춰서(순서) 넣어야 함.
    @Test
    public void 생성자방식 () throws Exception{
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age
//                        member.id
                ))
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * @author: hyechan
     * @since: 2022/03/18 10:42 오후
     * @description
     */
    @Test
    public void findUserDto() throws Exception{
        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch = queryFactory
// 이건 순서만 맞으면 되는데?.select(Projections.constructor(UserDto.class,
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                            .select(memberSub.age.max())
                                .from(memberSub), "age")
                        //subquery 이름 없는 거 문제 될 때 ExpressionUtils.as 사용해서 subquery사용가능
                ))
                .from(member)
                .fetch();

        for (UserDto s : fetch) {
            System.out.println("s = " + s);
        }
    }

    /**
     * @author: hyechan
     * @since: 2022/03/23 3:45 오후
     * @description
     */
    @Test
    public void findDtoByQueryProjection() throws Exception{
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
}
