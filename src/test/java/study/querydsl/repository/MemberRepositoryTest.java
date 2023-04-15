package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.MemberEntity;
import study.querydsl.entity.QMemberEntity;
import study.querydsl.entity.TeamEntity;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMemberEntity.*;

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

    @Test
    void searchPageSimple() {
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
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        // then
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    /**
     * SpringDataJPA에서 QueryDsl을 지원하는 기능
     * - 실무에서 사용하기에는 아직은 무리가 있다.
     * -> Join이 들어가게 되고 복잡한 쿼리가 되면 동작하지 않을 수 있다.
     * -> Service계층에서 특정 라이브러리(QueryDsl)에 의존적인 코드가 작성되게 된다.
     */
    @Test
    void QueryDslPredicateExecutorTest() {
        // given
        MemberEntity member1 = new MemberEntity("member1", 10);
        MemberEntity member2 = new MemberEntity("member2", 20);
        MemberEntity member3 = new MemberEntity("member3", 30);
        MemberEntity member4 = new MemberEntity("member4", 40);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // when
        Iterable<MemberEntity> result =
                memberRepository.findAll(
                        memberEntity.age.between(20, 40)
                                        .and(memberEntity.username.eq("member1")));

        //then
        for (MemberEntity memberEntity : result) {
            System.out.println("memberEntity = " + memberEntity);
        }
    }
}
