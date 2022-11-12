package com.example.test;

import com.example.test.entity.Member;
import com.example.test.entity.QMember;
import com.example.test.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;


    JPAQueryFactory queryFactory;

    QMember member;
    QMember qMember;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        member = new QMember("m");
        qMember = QMember.member;


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

        Assertions.assertEquals(findMember.getUsername(), "member1");
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
        Assertions.assertEquals(findMember.getUsername(), "member1");
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
        Assertions.assertEquals(findMember.getUsername(), "member");
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
        Assertions.assertEquals(findMember.getUsername(), "member1");
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
        Assertions.assertEquals(findMember.getUsername(), "member1");
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

        Assertions.assertEquals(result1.size(), 1);
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
        Assertions.assertEquals(member5.getUsername(), "member5");
        Assertions.assertEquals(member6.getUsername(), "member6");
        Assertions.assertNull(memberNull.getUsername());
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

        Assertions.assertEquals(result.size(), 2);
    }
}
