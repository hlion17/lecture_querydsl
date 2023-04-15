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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicTest() {
        // Given
        MemberEntity member = new MemberEntity("member1", 10);
        memberRepository.save(member);

        // When
        MemberEntity foundMemberEntity = memberRepository.findById(member.getId()).get();
        List<MemberEntity> foundMemberByUsername = memberRepository.findByUsername("member1");
        List<MemberEntity> allMembers = memberRepository.findAll();

        // Then
        assertThat(foundMemberEntity).isEqualTo(member);
        assertThat(foundMemberByUsername.get(0).getUsername()).isEqualTo("member1");
        assertThat(allMembers).extracting("username").containsExactly("member1");
    }

    @Test
    void searchTest() {
        // given
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

        // when
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition);

        // then
        assertThat(result).extracting("username").containsExactly("member4");
    }
}
