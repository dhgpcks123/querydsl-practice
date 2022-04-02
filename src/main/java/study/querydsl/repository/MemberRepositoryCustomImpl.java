package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * fileName    : MemberRepositoryImpl
 * author      : hyechan
 * date        : 2022/03/31
 * description :
 * ====================================================
 * DATE              AUTHOR               NOTE
 * ----------------------------------------------------
 * 2022/03/31 9:00 오후  hyechan        최초 생성
 */
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username, member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName"))
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//         5 count 만 조회
        long total = getTotal(condition);
//        return new PageImpl<>(results, pageable, total);
        //카운트 쿼리 생략가능!
        // 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
        // 마지막 페이지 일 때 offset+컨텐츠 사이즈 더해서 전체 사이즈 구함.

//        JPAQuery<Member> countQuery = queryFactory
//                .selectFrom(member)
//                .where(
//                        usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe())
//                );

        JPAQuery<Long> where = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

//        return PageableExecutionUtils.getPage(results, pageable, () -> countQuery.fetch().size());
        return PageableExecutionUtils.getPage(results, pageable, where::fetchOne);
//        return PageableExecutionUtils.getPage(results, pageable, where::fetchOne() );
//        return PageableExecutionUtils.getPage(results, pageable, countQuery::fetch().size());

    }

    /*
    강사님 안녕하세요. 강사님 덕분에 JPA 로드맵 강의를 알차게 배웠습니다.
    갑자기 뜬금없이 생각났는데 컨트롤러에서 Pageable를 바로 사용할 경우 max size를 제한할 수 있는 방법은 없는건지요.
    악의적으로 query String에 size값을 100000000 이렇게 찍어놓고 막무가내로 요청을 여러번 보낸다면 왠지 서버가 뻗을 것 같다는 생각이 들어서요.
    Pageable에서 최대 사이즈 막을 수 있는 방법이 없다면 따로 VO를 통해서 제한해 줘야하는 건지 궁금해서 질문드립니다.


    => 막을 수 있습니다. ㅎㅎ 다음 링크를 참고해주세요. 기본 값은 2000입니다.
    https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#spring.data.web.pageable.max-page-size
    감사합니다.
     */


    private long getTotal(MemberSearchCondition condition) {
        return queryFactory
                .select(member)
                .from(member)
//                .leftJoin(member.team, team) 쪼개면 장점, 필요없는 거 join 할 필요가 없다.
//              + 꼬일 때 있어서 fetchCount()없어짐
//                카운트쿼리 성능... 영향 큼
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ).fetch().size();
    }
    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null ;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}
