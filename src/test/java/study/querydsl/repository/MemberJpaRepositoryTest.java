package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Test
    void basic() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        // JPQL 쿼리 실행하면, 플러시 자동으로 호출됨
        Member findMember = memberJpaRepository.findById(member.getId())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 Member입니다."));

        assertThat(findMember).isEqualTo(member);

        List<Member> members = memberJpaRepository.findAll_Querydsl();

        assertThat(members).containsExactly(member);

        List<Member> members2 = memberJpaRepository.findByUsername_Querydsl("member1");

        assertThat(members2).containsExactly(member);
    }

    @Test
    void searchByBuilder() {
        // 데이터 추가
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);

//        assertThat(memberTeamDtos).extracting("username").containsExactly("member4");
        assertThat(memberTeamDtos).extracting("username").containsExactly("member3", "member4");
    }

    @Test
    void search() {
        // 데이터 추가
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);

        assertThat(memberTeamDtos).extracting("username").containsExactly("member4");
    }

}
