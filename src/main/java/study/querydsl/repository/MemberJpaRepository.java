package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.MemberEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMemberEntity.*;
import static study.querydsl.entity.QTeamEntity.*;

@Repository
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(MemberEntity entity) {
        em.persist(entity);
    }

    public Optional<MemberEntity> findById(Long id) {
        MemberEntity foundMemberEntity = em.find(MemberEntity.class, id);
        return Optional.ofNullable(foundMemberEntity);
    }

    public List<MemberEntity> findAll() {
        return em.createQuery("select m from MemberEntity m", MemberEntity.class)
                 .getResultList();
    }

    public List<MemberEntity> findAllWithQueryDsl() {
        return queryFactory.selectFrom(memberEntity)
                           .fetch();
    }

    public List<MemberEntity> findByUsername(String username) {
        return em.createQuery("select m from MemberEntity m where m.username = :username")
                 .setParameter("username", username)
                 .getResultList();
    }

    public List<MemberEntity> findByUsernameWithQueryDsl(String username) {
        return queryFactory.selectFrom(memberEntity)
                           .where(memberEntity.username.eq(username))
                           .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(memberEntity.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(teamEntity.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(memberEntity.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(memberEntity.age.loe(condition.getAgeLoe()));
        }

        return queryFactory.select(new QMemberTeamDto(
                                   memberEntity.id,
                                   memberEntity.username,
                                   memberEntity.age,
                                   teamEntity.id,
                                   teamEntity.name
                           ))
                           .from(memberEntity)
                           .leftJoin(memberEntity.team, teamEntity)
                           .where(builder)
                           .fetch();
    }

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
