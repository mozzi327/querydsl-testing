package com.example.test;

import com.example.test.entity.Member;
import com.example.test.entity.QMember;
import com.example.test.entity.QTeam;
import com.example.test.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;


    JPAQueryFactory queryFactory;

    QMember member;
    QTeam team;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        member = new QMember("m");
        team = new QTeam("t");


        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        QMember m = new QMember("m");
    }


    @Test
    @DisplayName("멤버 찾기 테스트1")
    public void startJPQL() {
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    @DisplayName("멤버 찾기 테스트2")
    public void startQuerydsl() {
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("멤버 찾기 테스트3")
    public void startQuerydsl2() {
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member");
    }

    @Test
    @DisplayName("멤버 찾기 테스트4")
    public void startQuerydsl3() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    @DisplayName("멤버 검색 조건 테스트")
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    @DisplayName("JPQL이 제공하는 검색조건")
    public void conditionTest() {
        System.out.println("username = member1 : " + member.username.eq("member1"));
        System.out.println("username != member1 : " + member.username.ne("member1"));
        System.out.println("username != member1 : " + member.username.eq("member1").not());

        System.out.println("age in (10, 20) : " + member.age.in(10, 20));
        System.out.println("age not in (10, 20) : " + member.age.notIn(10, 20));
        System.out.println("age between (10, 30) : " + member.age.between(10, 30));

        System.out.println("age >= 30 : " + member.age.goe(30));
        System.out.println("age > 30 : " + member.age.gt(30));
        System.out.println("age <= 30 : " + member.age.loe(30));
        System.out.println("age < 30 : " + member.age.lt(30));

        System.out.println("like 검색 : " + member.username.like("member%"));
        System.out.println("like %member% 검색 : " + member.username.contains("member"));
        System.out.println("like member% 검색 : " + member.username.startsWith("member"));
    }


    @Test
    @DisplayName("AND 조건을 파라미터로 처리")
    public void searchAndParam() {
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetch();

        assertThat(result1.size()).isEqualTo(1);
    }


    @Test
    @DisplayName("다양한 결과 조회")
    public void differentResult() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();


        Member findMember = queryFactory
                .selectFrom(member)
                .fetchFirst();


        Long count = queryFactory
                .select(member.count())
                .from(member).fetchOne();

        System.out.println("fetch : " + fetch);
        System.out.println("findMember : " + findMember);
        System.out.println("count : " + count);
    }


    /**
     * 회원 정렬 순서<br>
     * 1. 회원 나이 내림차순(desc)<br>
     * 2. 회원 이름 올림차순(asc)<br><br>
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)<br>
     * */
    @Test
    @DisplayName("정렬 테스트")
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    @DisplayName("페이징 테스트1")
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이징 테스트2")
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .select(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                // fetchResult는 deprecated 되어 사용하지 않는 것을 추천 -> 차라리 select[From]()에서 count()를 쓸 것
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL<br>
     * select<br>
     * COUNT(m), //회원수<br>
     * SUM(m.age), //나이 합<br>
     * AVG(m.age), //평균 나이<br>
     * MAX(m.age), //최대 나이<br>
     * MIN(m.age) //최소 나이 * from Member m<br>
     */
    @Test
    @DisplayName("집합 테스트")
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 팀의 평균 영령을 구하는 테스팅
     * @throws Exception
     */
    @Test
    @DisplayName("GroupBy 테스트")
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);


        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("기본 조인 테스트")
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        System.out.println(result);
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인) <br>
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * */
    @Test
    @DisplayName("세타 조인 테스트")
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        System.out.println(result);

    }


    //// 조인 - on 절
}
