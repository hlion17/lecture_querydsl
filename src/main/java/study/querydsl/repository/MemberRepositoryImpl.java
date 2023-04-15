package study.querydsl.repository;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

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
