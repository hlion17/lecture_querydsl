package study.querydsl.repository;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.MemberEntity;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMemberEntity.memberEntity;
import static study.querydsl.entity.QTeamEntity.teamEntity;

/**
 * PostFix '-Impl' 을 지켜줘야 한다.
 */
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory.select(
                                   new QMemberTeamDto(
                                           memberEntity.id,
                                           memberEntity.username,
                                           memberEntity.age,
                                           teamEntity.id,
                                           teamEntity.name
                                   )
                           )
                           .from(memberEntity)
                           .leftJoin(memberEntity.team, teamEntity)
                           .where(
                                   usernameEq(condition.getUsername()),
                                   teamNameEq(condition.getTeamName()),
                                   ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                           )
                           .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(
                        new QMemberTeamDto(
                                memberEntity.id,
                                memberEntity.username,
                                memberEntity.age,
                                teamEntity.id,
                                teamEntity.name
                        )
                )
                .from(memberEntity)
                .leftJoin(memberEntity.team, teamEntity)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(
                        new QMemberTeamDto(
                                memberEntity.id,
                                memberEntity.username,
                                memberEntity.age,
                                teamEntity.id,
                                teamEntity.name
                        )
                )
                .from(memberEntity)
                .leftJoin(memberEntity.team, teamEntity)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<MemberEntity> countQuery = queryFactory
                .select(memberEntity)
                .from(memberEntity)
                .leftJoin(memberEntity.team, teamEntity)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                );

        // CountQuery 성능 최적화: Paging 상태를 판단하여 CountQuery가 필요없을 경우 수행하지 않음
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
//        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        if (ageLoe != null && ageGoe != null) {
            return ageGoe(ageGoe).and(ageLoe(ageLoe));
        } else if (ageLoe != null) {
            return ageLoe(ageLoe);
        } else if (ageGoe != null) {
            return ageGoe(ageGoe);
        } else {
            return null;
        }
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? memberEntity.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? teamEntity.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGeo) {
        return (ageGeo != null) ? memberEntity.age.goe(ageGeo) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return (ageLoe != null) ? memberEntity.age.loe(ageLoe) : null;
    }
}
