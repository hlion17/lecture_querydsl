package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.MemberEntity;
import study.querydsl.entity.QMemberEntity;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMemberEntity.*;
import static study.querydsl.entity.QTeamEntity.teamEntity;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {
    public MemberTestRepository() {
        super(QMemberEntity.class);
    }

    public List<MemberEntity> basicSelect() {
        return select(memberEntity)
                .from(memberEntity)
                .fetch();
    }

    public List<MemberEntity> basicSelectFrom() {
        return selectFrom(memberEntity)
                .fetch();
    }

    /**
     *  CustomQueryDslSupport Class 사용 전 코드
     */
    public Page<MemberEntity> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<MemberEntity> query =
                selectFrom(memberEntity)
                        .leftJoin(memberEntity.team, teamEntity)
                        .where(
                                usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe())
                        );

        List<MemberEntity> content = getQuerydsl().applyPagination(pageable, query)
                                                  .fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    /**
     *  CustomQueryDslSupport Class 사용 후 코드
     */
    public Page<MemberEntity> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable, query ->
                query.selectFrom(memberEntity)
                     .leftJoin(memberEntity.team, teamEntity)
                     .where(
                             usernameEq(condition.getUsername()),
                             teamNameEq(condition.getTeamName()),
                             ageGoe(condition.getAgeGoe()),
                             ageLoe(condition.getAgeLoe())
                     ));
    }

    /**
     *  CustomQueryDslSupport Class 사용 후 코드
     *  - Complex version
     */
    public Page<MemberEntity> applyPaginationV2(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
                contentQuery ->
                        contentQuery.selectFrom(memberEntity)
                                    .leftJoin(memberEntity.team, teamEntity)
                                    .where(
                                            usernameEq(condition.getUsername()),
                                            teamNameEq(condition.getTeamName()),
                                            ageGoe(condition.getAgeGoe()),
                                            ageLoe(condition.getAgeLoe())
                                    ),
                countQuery
                        -> countQuery.selectFrom(memberEntity)
                                     .leftJoin(memberEntity.team, teamEntity)
                                     .where(
                                             usernameEq(condition.getUsername()),
                                             teamNameEq(condition.getTeamName()),
                                             ageGoe(condition.getAgeGoe()),
                                             ageLoe(condition.getAgeLoe())
                                     )
        );
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
