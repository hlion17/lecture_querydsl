package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.MemberEntity;
import study.querydsl.entity.TeamEntity;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {
    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            TeamEntity teamA = new TeamEntity("teamA");
            TeamEntity teamB = new TeamEntity("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 200; i++) {
                TeamEntity selectedTeam = (i % 2 == 0) ? teamA : teamB;
                MemberEntity memberEntity = new MemberEntity("member" + i, i, selectedTeam);
                em.persist(memberEntity);
            }
        }
    }
}
