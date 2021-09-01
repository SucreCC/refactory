package com.factory.longMethod.conditional.example2.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BusinessAuditDAO extends JpaRepository<BusinessAudit, Integer>, JpaSpecificationExecutor {

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("delete from BusinessAudit where cardSms.id=:cardId")
    int deleteByCardSmsId(@Param("cardId") int cardId);

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("delete from BusinessAudit where smartMenuScheme.id=:schemeId")
    int deleteBySmartMenuSchemeId(@Param("schemeId") int schemeId);
}
