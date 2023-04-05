package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMemberEntity.*;
import static study.querydsl.entity.QTeamEntity.teamEntity;

@SpringBootTest
@Transactional
class MemberEntityTest {

    @PersistenceContext
    private EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);

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

        em.flush();
        em.clear();

        List<MemberEntity> members = em.createQuery("select m from MemberEntity m", MemberEntity.class)
                                       .getResultList();

        for (MemberEntity member : members) {
            System.out.println("member = " + member);
            System.out.println("member.getTeam() = " + member.getTeam());
        }
    }

    @Test
    void startJPQL() {
        String query =
                "select m from MemberEntity m " +
                        "where m.username = :username";
        MemberEntity foundMember = em.createQuery(query, MemberEntity.class)
                                     .setParameter("username", "member1")
                                     .getSingleResult();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl() {
        MemberEntity foundMember =
                queryFactory.select(memberEntity)
                            .from(memberEntity)
                            .where(memberEntity.username.eq("member1"))
                            .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        MemberEntity foundMember = queryFactory
                .selectFrom(memberEntity)
                .where(memberEntity.username.eq("member1")
                                            .and(memberEntity.age.eq(10)))
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        MemberEntity foundMember = queryFactory
                .selectFrom(memberEntity)
                .where(
                        memberEntity.username.eq("member1"),
                        memberEntity.age.eq(10)
                )
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 다양한 QueryDsl 결과
     */
    @Test
    void resultFetch() {
//        List<MemberEntity> fetch = queryFactory
//                .selectFrom(memberEntity)
//                .fetch();
//
//        MemberEntity fetchOne = queryFactory
//                .selectFrom(memberEntity)
//                .fetchOne();
//
//        MemberEntity fetchFirst = queryFactory
//                .selectFrom(memberEntity)
//                .fetchFirst();
//
//        QueryResults<MemberEntity> fetchResults = queryFactory
//                .selectFrom(memberEntity)
//                .fetchResults();
//
//        fetchResults.getTotal();
//        List<MemberEntity> contents = fetchResults.getResults();

        long total = queryFactory
                .selectFrom(memberEntity)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 호원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        em.persist(new MemberEntity(null, 100));
        em.persist(new MemberEntity("member5", 100));
        em.persist(new MemberEntity("member6", 100));

        List<MemberEntity> result = queryFactory
                .selectFrom(memberEntity)
                .where(memberEntity.age.eq(100))
                .orderBy(memberEntity.age.desc(), memberEntity.username.asc().nullsLast())
                .fetch();

        MemberEntity member5 = result.get(0);
        MemberEntity member6 = result.get(1);
        MemberEntity memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * Paging
     */
    @Test
    void paging1() {
        List<MemberEntity> result = queryFactory
                .selectFrom(memberEntity)
                .orderBy(memberEntity.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);

        QueryResults<MemberEntity> fetchResults = queryFactory
                .selectFrom(memberEntity)
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(fetchResults.getTotal()).isEqualTo(4);
        assertThat(fetchResults.getLimit()).isEqualTo(2);
        assertThat(fetchResults.getOffset()).isEqualTo(1);
        assertThat(fetchResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(memberEntity.count(),
                        memberEntity.age.sum(),
                        memberEntity.age.avg(),
                        memberEntity.age.max(),
                        memberEntity.age.min())
                .from(memberEntity)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(memberEntity.count())).isEqualTo(4);
        assertThat(tuple.get(memberEntity.age.sum())).isEqualTo(100);
        assertThat(tuple.get(memberEntity.age.avg())).isEqualTo(25);
        assertThat(tuple.get(memberEntity.age.max())).isEqualTo(40);
        assertThat(tuple.get(memberEntity.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(teamEntity.name, memberEntity.age.avg())
                .from(memberEntity)
                .join(memberEntity.team, teamEntity)
                .groupBy(teamEntity.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(teamEntity.name)).isEqualTo("teamA");
        assertThat(teamA.get(memberEntity.age.avg())).isEqualTo(15);
        assertThat(teamB.get(teamEntity.name)).isEqualTo("teamB");
        assertThat(teamB.get(memberEntity.age.avg())).isEqualTo(35);
    }

    /**
     * Join
     */
    @Test
    public void join() {
        List<MemberEntity> result = queryFactory.selectFrom(memberEntity)
//                                                .join(memberEntity.team, teamEntity)
                                                .leftJoin(memberEntity.team, teamEntity)
                                                .where(teamEntity.name.eq("teamA"))
                                                .fetch();

        assertThat(result).extracting("username")
                          .contains("member1", "member2");
    }

    /**
     * 세타조인
     * - from 절에 여러 엔티티 선택해서 연관관계가 없는 엔티티 끼리 Join 가능
     * - 외부조인 불가능 => on절을 사용하면 외부조인 가능
     */
    @Test
    void theta_join() {
        em.persist(new MemberEntity("teamA"));
        em.persist(new MemberEntity("teamB"));
        em.persist(new MemberEntity("teamC"));

        List<MemberEntity> result = queryFactory.select(memberEntity)
                                                .from(memberEntity, teamEntity)
                                                .where(memberEntity.username.eq(teamEntity.name))
                                                .fetch();

        assertThat(result).extracting("username")
                          .contains("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM MemberEntity m LEFT JOIN m.team ON t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID = t.id and t.name = 'teamA'
     */
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory.select(memberEntity, teamEntity)
                                         .from(memberEntity)
                                         .leftJoin(memberEntity.team, teamEntity).on(teamEntity.name.eq("teamA"))
                                         .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    void join_on_no_relation() throws Exception {
        em.persist(new MemberEntity("teamA"));
        em.persist(new MemberEntity("teamB"));

        List<Tuple> result = queryFactory.select(memberEntity, teamEntity)
                                         .from(memberEntity)
                                         .leftJoin(teamEntity).on(memberEntity.username.eq(teamEntity.name))
                                         .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * Fetch Join
     */
    @Test
    void no_fetch_join() {
        em.flush();
        em.clear();

        MemberEntity findMember = queryFactory.selectFrom(memberEntity)
                                              .where(memberEntity.username.eq("member1"))
                                              .fetchOne();
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(isLoaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void use_fetch_join() {
        em.flush();
        em.clear();

        MemberEntity findMember = queryFactory.selectFrom(memberEntity)
                                              .join(memberEntity.team, teamEntity).fetchJoin()
                                              .where(memberEntity.username.eq("member1"))
                                              .fetchOne();
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(isLoaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 서브쿼리
     * - JPA JPQL 서브쿼리 한계점으로 from절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     */
    @Test
    void subQuery() {
        QMemberEntity memberSub = new QMemberEntity("memberSub");

        List<MemberEntity> result = queryFactory.selectFrom(memberEntity)
                                                .where(memberEntity.age.eq(
                                                        select(memberSub.age.max())
                                                                .from(memberSub)
                                                ))
                                                .fetch();

        List<MemberEntity> resultWithGoe = queryFactory.selectFrom(memberEntity)
                                                       .where(memberEntity.age.goe(
                                                               select(memberSub.age.avg())
                                                                       .from(memberSub)
                                                       ))
                                                       .fetch();


        List<MemberEntity> resultWithIn = queryFactory.selectFrom(memberEntity)
                                                      .where(memberEntity.age.in(
                                                              select(memberSub.age)
                                                                      .from(memberSub)
                                                                      .where(memberSub.age.gt(10))
                                                      ))
                                                      .fetch();

        List<Tuple> resultWithSelectSubQuery = queryFactory.select(memberEntity.username,
                                                                   select(memberSub.age.avg())
                                                                           .from(memberSub)
                                                           )
                                                           .from(memberEntity)
                                                           .fetch();

        assertThat(result).extracting("age")
                          .containsExactly(40);

        assertThat(resultWithGoe).extracting("age")
                                 .containsExactly(30, 40);

        assertThat(resultWithIn).extracting("age")
                                .containsExactly(20, 30, 40);

        for (Tuple tuple : resultWithSelectSubQuery) {
            System.out.println("username = " + tuple.get(memberEntity.username));
            System.out.println("age = " + tuple.get(select(memberSub.age.avg()).from(memberSub)));
        }
    }

    /**
     * Case 문
     */
    @Test
    void basicCase() {
        List<String> resultWithBasic = queryFactory.select(memberEntity.age
                                                 .when(10).then("열살")
                                                 .when(20).then("스무살")
                                                 .otherwise("기타"))
                                         .from(memberEntity)
                                         .fetch();

        for (String s : resultWithBasic) {
            System.out.println("s = " + s);
        }

        List<String> resultWithCaseBuilder = queryFactory.select(new CaseBuilder()
                                              .when(memberEntity.age.between(0, 20)).then("0~20살")
                                              .when(memberEntity.age.between(21, 30)).then("21~30살")
                                              .otherwise("기타")
                                      )
                                      .from(memberEntity)
                                      .fetch();

        for (String s : resultWithCaseBuilder) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수, 문자더하기
     */
    @Test
    void constantAndConcat() {
        Tuple resultWithConstant = queryFactory
                .select(memberEntity.username, Expressions.constant("A"))
                .from(memberEntity)
                .fetchFirst();

        System.out.println("resultWithConstant = " + resultWithConstant);

        String resultWithConcat = queryFactory
                .select(memberEntity.username.concat("_").concat(memberEntity.age.stringValue()))
                .from(memberEntity)
                .where(memberEntity.username.eq("member1"))
                .fetchOne();

        System.out.println("resultWithConcat = " + resultWithConcat);
    }

    /**
     * Projection
     * - 단일 타입
     * - Tuple
     * - DTO
     */
    @Test
    void projection() {
        // 단일타입
        System.out.println("단일타입");
        List<String> result = queryFactory.select(memberEntity.username)
                                         .from(memberEntity)
                                         .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }

        // Tuple
        System.out.println("Tuple 방식");
        List<Tuple> resultWithTuple = queryFactory.select(memberEntity.username, memberEntity.age)
                                        .from(memberEntity)
                                        .fetch();
        for (Tuple tuple : resultWithTuple) {
            String username = tuple.get(memberEntity.username);
            Integer age = tuple.get(memberEntity.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

        // DTO
        // 1. JPQL: new Operation을 사용하여 조회하는 방법
        System.out.println("JQPL Projection");
        List<MemberDto> resultWithNewOperation = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from MemberEntity m", MemberDto.class)
                                       .getResultList();

        for (MemberDto memberDto : resultWithNewOperation) {
            System.out.println("memberDto = " + memberDto);
        }

        /**
         * 2. QueryDSL Dto Projection
         * - setter 접근 방식
         * - field 접근 방식
         * - 생성자 접근 방식
         * - Other Dto
         */
        // - setter
        System.out.println("QueryDsl Projection - setter 방식");
        List<MemberDto> resultWithQueryDslBySetter = queryFactory.select(Projections.bean(MemberDto.class,
                                                    memberEntity.username,
                                                    memberEntity.age))
                                            .from(memberEntity)
                                            .fetch();
        for (MemberDto memberDto : resultWithQueryDslBySetter) {
            System.out.println("memberDto = " + memberDto);
        }

        // - fields
        System.out.println("QueryDsl Projection - fields 방식");
        List<MemberDto> resultWithQueryDslByField = queryFactory.select(Projections.fields(MemberDto.class,
                                                                         memberEntity.username,
                                                                         memberEntity.age))
                                                                 .from(memberEntity)
                                                                 .fetch();
        for (MemberDto memberDto : resultWithQueryDslBySetter) {
            System.out.println("memberDto = " + memberDto);
        }

        // - constructor
        System.out.println("QueryDsl Projection - 생성자 방식");
        List<MemberDto> resultWithQueryDslByConstructor = queryFactory.select(Projections.constructor(MemberDto.class,
                                                                        memberEntity.username,
                                                                        memberEntity.age))
                                                                .from(memberEntity)
                                                                .fetch();
        for (MemberDto memberDto : resultWithQueryDslByConstructor) {
            System.out.println("memberDto = " + memberDto);
        }

        // - otherDto
        System.out.println("QueryDsl Projection - 필드명이 다른 Dto 매칭 & scalar SubQuery");
        QMemberEntity memberSub = new QMemberEntity("memberSub");
        List<UserDto> resultWithQueryDslByOtherDto =
                queryFactory.select(Projections.constructor(UserDto.class,
                                    memberEntity.username.as("name"),
                                    ExpressionUtils.as(select(memberSub.age.max()).from(memberSub), "age")))
                            .from(memberEntity)
                            .fetch();
        for (UserDto userDto : resultWithQueryDslByOtherDto) {
            System.out.println("memberDto = " + userDto);
        }
    }

    /**
     * Query Project By Dto
     */
    @Test
    void findByQDtoQueryProjection() {
        List<MemberDto> resultWithQDto
                = queryFactory.select(new QMemberDto(memberEntity.username, memberEntity.age))
                              .from()
                              .fetch();

        for (MemberDto memberDto : resultWithQDto) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * QueryDsl 동적쿼리
     * - BooleanBuilder
     * - where 다중 파라미터
     */
    @Test
    void dynamicQueryDsl() {
        String usernameParam = "member1";
        Integer agePram = null;

        System.out.println("BuilderBuilder 사용");
        List<MemberEntity> result = selectMember1(usernameParam, agePram);
        for (MemberEntity entity : result) {
            System.out.println("entity = " + entity);
        }

        System.out.println("where 다중 파라미터 사용");
        List<MemberEntity> result2 = selectMember2(usernameParam, agePram);
        for (MemberEntity entity : result2) {
            System.out.println("entity = " + entity);
        }

    }

    private List<MemberEntity> selectMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(memberEntity.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(memberEntity.age.eq(ageCond));
        }
        return queryFactory.select(memberEntity)
                    .from(memberEntity)
                    .where(builder)
                    .fetch();
    }
    private List<MemberEntity> selectMember2(String usernameCond, Integer ageCond) {
        return queryFactory.select(memberEntity)
                .from(memberEntity)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))  // 조건들을 조립하여 새로운 조건 생성 가능
                .fetch();

    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ? null : memberEntity.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : memberEntity.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 수정, 삭제 벌크 연산
     * - 벌크연산의 경우 DB에 직접쿼리 하기 때문에 DB와 영속성컨텍스트와 차이가 발생한다.
     * - 때문에 벌크연산 후에는 영속성컨텍스트를 flush, clear 하여 DB와 동기화시켜주는 작업이 필요하다.
     */
    @Test
    @Commit
    void bulkUpdate() {
        // member1 = 10 -> 비회원
        // member2 = 20 -> 비회원
        // member3 = 30 -> 유지
        // member4 = 40 -> 유지
        long count =
                queryFactory.update(memberEntity)
                            .set(memberEntity.username, "비회원")
//                            .set(memberEntity.age, memberEntity.age.add(1))
                            .where(memberEntity.age.lt(28))
                            .execute();

        em.flush();
        em.clear();

        // 1 member1 = 10 -> DB 비회원
        // 2 member2 = 20 -> DB 비회원
        // 3 member3 = 30 -> DB 유지
        // 4 member4 = 40 -> DB 유지

        // 조회시 영속성컨텍스트가 우선권을 가지기 때문에 이전 데이터가 조회된다.
        List<MemberEntity> result = queryFactory.selectFrom(memberEntity)
                                                .fetch();
        for (MemberEntity entity : result) {
            System.out.println("entity = " + entity);
        }
    }

    @Test
    void bulkDelete() {
        long result =
                queryFactory.delete(memberEntity)
                            .where(memberEntity.age.gt(18))
                            .execute();
    }

    /**
     * SQL Function 호출하기
     * - SqlDialect에 등록된 함수만 사용할 수 있다.
     */
    @Test
    void sqlFunction() {
        List<String> result =
                queryFactory.select(Expressions.stringTemplate(
                                    "function('replace', {0}, {1}, {2})",
                                    memberEntity.username, "member", "M"))
                            .from(memberEntity)
                            .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {

        List<MemberEntity> result = queryFactory.select(memberEntity)
                                               .from(memberEntity)
//                                               .where(memberEntity.username.eq(Expressions.stringTemplate("function('lower', {0})", memberEntity.username)))
                                               .where(memberEntity.username.eq(memberEntity.username.lower()))  // ANSI 표준 함수인 경우 QueryDsl에서 지원하는 경우가 많다.
                                               .fetch();
        for (MemberEntity entity : result) {
            System.out.println("entity = " + entity);
        }
    }

}