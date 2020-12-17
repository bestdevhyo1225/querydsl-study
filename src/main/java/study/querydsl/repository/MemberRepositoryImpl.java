package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Objects;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Member.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return getMemberTeamDtoJpaQuery(condition).fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = getMemberTeamDtoJpaQuery(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults(); // SELECT 쿼리, SELECT COUNT 쿼리가 2번 나감

        List<MemberTeamDto> content = results.getResults();
        long totalCount = results.getTotal();

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> query = queryDsl().applyPagination(pageable, getMemberTeamDtoJpaQuery(condition));
        QueryResults<MemberTeamDto> queryResults = query.fetchResults();
        List<MemberTeamDto> content = queryResults.getResults();
        long totalCount = queryResults.getTotal();

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = getMemberTeamDtoJpaQuery(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();// SELECT 쿼리 1번만 나감

        // count 쿼리를 join을 사용하지 간단하게 사용해도 되는 경우가 있는데 (조회하는 쿼리의 갯수와 문제 없는 경우)
        // 그런 상황에서 사용한다.
        // --> 최적화를 위함
        return PageableExecutionUtils.getPage(content, pageable, getCountQuery(condition)::fetchCount);
    }

    private JPAQuery<MemberTeamDto> getMemberTeamDtoJpaQuery(MemberSearchCondition condition) {
        return queryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")
                        )
                )
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
    }

    private JPAQuery<Member> getCountQuery(MemberSearchCondition condition) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private Querydsl queryDsl() {
        return Objects.requireNonNull(getQuerydsl());
    }

}
