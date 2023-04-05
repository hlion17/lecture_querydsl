package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    /**
     * - (장점)QueryDsl 작성시 컴파일 시점에 Projection 관련 오류를 확인할 수 있다.
     * - (단점)데이터 운반을 위한 단순한 객체에 QueryDsl 라이브러리 의존성이 생긴다.
     */
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
