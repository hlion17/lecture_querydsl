package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.MemberEntity;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<MemberEntity> {

    // select m from MemberEntity m where m.username = :username;
    List<MemberEntity> findByUsername(String username);

    @Override
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
