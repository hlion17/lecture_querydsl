package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.MemberEntity;
import study.querydsl.entity.TeamEntity;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void basicTest() {
        MemberEntity member = new MemberEntity("member1", 10);
        memberJpaRepository.save(member);

        MemberEntity foundMemberEntity = memberJpaRepository.findById(member.getId()).get();

        assertThat(foundMemberEntity).isEqualTo(member);

//        List<MemberEntity> foundMemberList = memberJpaRepository.findAll();
        List<MemberEntity> foundMemberList = memberJpaRepository.findAllWithQueryDsl();
        assertThat(foundMemberList).containsExactly(member);

//        List<MemberEntity> foundMemberList2 = memberJpaRepository.findByUsername("member1");
        List<MemberEntity> foundMemberList2 = memberJpaRepository.findByUsernameWithQueryDsl("member1");
        assertThat(foundMemberList2).containsExactly(member);
    }

    @Test
    void searchTest() {
        TeamEntity teamA = new TeamEntity("teamA");
        TeamEntity teamB = new TeamEntity("teamB");
        em.persist(teamA);
        em.persist(teamB);

        MemberEntity member1 = new MemberEntity("member1", 10, teamA);
        MemberEntity member2 = new MemberEntity("member2", 20, teamA);
        MemberEntity member3 = new MemberEntity("member3", 30, teamB);
        MemberEntity member4 = new MemberEntity("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        List<MemberTeamDto> result = memberJpaRepository.search(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }

}