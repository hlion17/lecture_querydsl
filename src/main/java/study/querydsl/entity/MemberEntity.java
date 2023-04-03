package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

import static javax.persistence.FetchType.*;

@Entity
@Getter @Setter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class MemberEntity {
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    public MemberEntity(String username) {
        this(username, 0);
    }

    public MemberEntity(String username, int age) {
        this(username, age, null);
    }

    public MemberEntity(String username, int age, TeamEntity team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(TeamEntity team) {
        this.team = team;
        team.getMembers().add(this);
    }

}
