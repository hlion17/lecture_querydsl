package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.HelloEntity;
import study.querydsl.entity.QHelloEntity;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        HelloEntity helloEntity = new HelloEntity();
        em.persist(helloEntity);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHelloEntity qHelloEntity = QHelloEntity.helloEntity;

        HelloEntity result = query.selectFrom(qHelloEntity)
                                  .fetchOne();

		assertThat(result).isEqualTo(helloEntity);
		assertThat(result.getId()).isEqualTo(helloEntity.getId());
    }

}
