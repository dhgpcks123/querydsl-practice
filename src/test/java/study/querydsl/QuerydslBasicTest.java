package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

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

    @Test
    @DisplayName("jpql")
    public void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("Querydsl")
    public void startQuerydsl() {
//        QMember m = new QMember("m"); // 별칭 직접 지정
//        QMember member = QMember.member;
        // 인스턴스 바로 static으로 사용
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 자동처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("querydsl search")
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();

        // between, nq, lt, like, startsWith, in, goe, contains(%contains%)
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.like("member1%"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // 결과 조회
    /*
        fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
        fetchOne() : 단 건 조회, 결과가 없으면 null. 둘 이상 이면 NonUniqueResultException
        fetchFirst() : limit(1).fetchOne()
        fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
        fetchCount : count 쿼리로 변경해서 count 수 조회
     */

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();
        //        return limit(1).fetchOne();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();

//        results.getTotal();
//        List<Member> results1 = results.getResults()
        List<Member> result = queryFactory
                .selectFrom(member)
                .offset(2)
                .limit(2)
                .fetch();

        long l = queryFactory
                .selectFrom(member)
                .fetchCount();
        System.out.println(l);

        //totalcount querydsl에서 어떻게 사용하지?

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls lasts)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc().nullsLast(), member.age.desc())
                .fetch();
        //nullsFirst()

        Member member = result.get(0);
        Member memberNull = result.get(6);
        assertThat(member.getUsername()).isEqualTo("member1");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);

        for (Member member : result) {
            System.out.println(member.toString());
        }
    }

    @Test
    public void paging2total() {
        //total query 분리한다. 이걸 사용하는지 잘모르겠다...
        //TODO : 최적화 학습 필요
        long count = queryFactory.selectFrom(member)
                .stream().count();
        System.out.println(count);
//        tuple로 꺼내는 방법도 있음.
//        select(member.count()).from(member).fetchOne()
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min(),
                        member.age.avg()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        //실무에서는 tuple 잘 안 쓰고 dto 뽑아서 쓴대.
    }
    /**
     * @author: hyechan
     * @since: 2022/03/14 7:02 오후
     * @description
     */
    @Test
    @DisplayName("팀의 이름과 각 팀의 평균 연령을 구하라")
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
    

    /**
     * @author: hyechan
     * @since: 2022/03/14 8:48 오후
     * @description Use join 팀 A에 소속된 모든 회원
     */
    @Test
    @DisplayName("use join")
    public void join() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                // = innerjoin
                // leftJoin, rightJoin
                .where(team.name.eq("teamA"))
                .fetch();

        for (Member member1 : result) {
            System.out.println(member1.toString());
        }
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 9:18 오후
     * @description Theta_join/Cross_join 회원 이름이 팀 이름과 같은 회원 조회
     * 연관관계가 없는 테이블 join하기
     */
    @Test
    @DisplayName("theta_join")
    public void theta_join() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //모든 회원, 모든 팀 가져와서 다 join한 뒤 where조인으로 걸러냄
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 9:27 오후
     * @description Join on (jpa2.1~)
     * 1. 조인 대상 필터링
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA 팀만 조인 회원은 모두 조회
     * 2. 연관관계 없는 엔티티 조인(많이 사용)
     */
    @Test
    @DisplayName("join on (jpa2.1~)")
    public void join_on_filtering() throws Exception{
        List<Tuple> innerJoin = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();
        //=
        List<Tuple> join = queryFactory
                .select(member, member.team)
                .from(member)
                .where(member.team.name.eq("teamA"))
                .fetch();
        // 두 경우 다 innerJoin이 일어남.

        //leftJoin
        List<Tuple> fetch = queryFactory
                .select(member, member.team)
                .from(member)
                .leftJoin(member.team, team)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }

        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        // 다 가져오는데 teamA에 있는 정보만 가져와 나머진 null로 해줘.
        // 여러가지 타입이어서 Tuple
        // 내부조인은 where쓰고 외부조인 필요한 경우 on을 사용한다. leftJoin으로 on절 사용한다.
        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 10:05 오후
     * @description Join_on_no_relation
     * 2. 연관관계 없는 엔티티 조인(많이 사용)
     * 회원의 이름이 팀 이름과 같은 회원 조인
     */
    @Test
    @DisplayName("join_on_no_relation")
    public void join_on_no_relation() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
        // 일반조인은 member.team, team 들어가는데
        // leftJoin-on은 leftJoin(조인대상).on(xxx) 들어간다.
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * @author: hyechan
     * @since: 2022/03/14 10:25 오후
     * @description FetchJoinNo
     */
    @Test
    @DisplayName("fetchJoinNo")
    public void fetchJoinNo() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 10:25 오후
     * @description FetchJoinUse
     */
    @Test
    @DisplayName("fetchJoinUse")
    public void fetchJoinUse() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();


        em.flush();
        em.clear();
        Member fetchJoin = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded1 = emf.getPersistenceUnitUtil().isLoaded(fetchJoin.getTeam());
        assertThat(loaded1).isFalse();
        //fetchJoin()붙이지 않으면 무조건 lazyLoading발생
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 10:44 오후
     * @description SubQuery 나이가 가장 많은 회원 조회
     */
    @Test
    @DisplayName("subQuery")
    public void subQuery() throws Exception{

        QMember memberSub = new QMember("memberSub");

        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetchOne();

        assertThat(member1).extracting("age").isEqualTo(40);
    }

    /**
     * @author: hyechan
     * @since: 2022/03/14 10:48 오후
     * @description SubQuery Goe 나이가 평균 이상인 회원
     */
    @Test
    @DisplayName("subQuery Goe")
    public void subQueryGoe() throws Exception{

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.gt(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }
    
    /**
     * @author: hyechan
     * @since: 2022/03/14 10:58 오후
     * @description SubQuery
     */
    @Test
    @DisplayName("subQuery In" )
    public void subQueryIn() throws Exception{
        QMember memberSub = new QMember("MemberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(fetch).extracting("age")
                .containsExactly(20, 30, 40);
    }
    
    /**
     * @author: hyechan
     * @since: 2022/03/14 11:01 오후
     * @description
     */
    @Test
    public void selectSubQuery() throws Exception{
        QMember memberSub = new QMember("MemberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }
    //querydsl은 jpa 빌더 역할을 할 뿐!
    //from 절의 서브쿼리는 jpa에서 사용할 수 없음.
    //1. 서브쿼리를 join으로 변경한다. (높은 확률로 사용가능하더라)
    //2. 쿼리를 2번 분리해서 실행한다. (성능 이슈 없다면 사용)
    //3. nativeSQL을 사용한다. (nativeQuery 짜라)

    // 실시간 트래픽이 중요하다?
    // 화면에서 최대한 캐시처리 해놨을꺼야..
    // sql 안티패턴스 : 개발자가 알아야 할 25가지 SQL 함정과 해법
    // 정말 복잡한 한방 쿼리 쪼개서 호출하면.. 몇 백줄 몇백 줄 줄여서 오히려 좋은 쿼리가 될 수 있다.
}
