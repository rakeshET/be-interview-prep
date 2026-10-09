package com.edstem.interviewprep.shortener;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Increments in a single UPDATE so the database serialises concurrent visits on the row lock.
     * A read-modify-write (load, +1, save) would lose updates when two visits read the same value.
     */
    @Modifying
    @Query("update ShortUrl s set s.visitCount = s.visitCount + 1 where s.code = :code")
    int incrementVisitCount(@Param("code") String code);
}
