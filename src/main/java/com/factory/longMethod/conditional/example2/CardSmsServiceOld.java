package com.factory.longMethod.conditional.example2;

import com.github.wenhao.jpa.Specifications;
import com.xuanwu.smp.common.dao.BusinessAuditDAO;
import com.xuanwu.smp.common.dao.CardSmsButtonDAO;
import com.xuanwu.smp.common.dao.CardSmsDAO;
import com.xuanwu.smp.common.dto.req.CardSmsReq;
import com.xuanwu.smp.common.dto.req.remote.CardSmsPushReq;
import com.xuanwu.smp.common.dto.req.remote.SmpRemoteSynReq;
import com.xuanwu.smp.common.dto.resp.CardSmsResp;
import com.xuanwu.smp.common.dto.resp.PageResp;
import com.xuanwu.smp.common.dto.resp.rest.RestResp;
import com.xuanwu.smp.common.entity.BusinessAudit;
import com.xuanwu.smp.common.entity.CardSms;
import com.xuanwu.smp.common.entity.CardSmsButton;
import com.xuanwu.smp.common.entity.User;
import com.xuanwu.smp.common.enumeration.ButtonStateEnum;
import com.xuanwu.smp.common.enumeration.ButtonTypeEnum;
import com.xuanwu.smp.common.enumeration.CardSmsStateEnum;
import com.xuanwu.smp.common.enumeration.SmsPushBusinessTypeEnum;
import com.xuanwu.smp.common.service.remote.RemoteSynService;
import com.xuanwu.smp.common.template.PageRespTemplate;
import com.xuanwu.smp.common.util.DateUtil;
import com.xuanwu.smp.common.util.JacksonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 卡片短信服务
 *
 * @author SunFengTao
 * @date 2020/03/31 16:24
 */
@Service
public class CardSmsServiceOld {

    @Autowired
    private CardSmsDAO cardSmsDAO;

    @Autowired
    private UserService userService;

    @Autowired
    private CardSmsButtonDAO cardSmsButtonDAO;

    @Resource(name = "smpTemplateRemoteSynServiceImpl")
    private RemoteSynService remoteSynService;

    @Autowired
    private BusinessAuditService businessAuditService;

    @Autowired
    private BusinessAuditDAO businessAuditDAO;

    public PageResp findCardSms(CardSmsReq req) {
        Specification<CardSms> specification = Specifications.<CardSms>and()
                .predicate((root, query, cb) -> {
                    List<Predicate> predicateList = new ArrayList<>();
                    Predicate[] predicates = getPredicates(req, root, cb, predicateList);
                    return cb.and(predicateList.toArray(predicates));
                })
                .build();
        PageResp<CardSmsResp> pageResp = PageRespTemplate.pageQuery(cardSmsDAO, specification, req, new CardSmsResp());
        setAuditSuggestions(pageResp);
        return pageResp;
    }

    private void setAuditSuggestions(PageResp<CardSmsResp> pageResp) {
        pageResp.getList().forEach(cardSmsResp -> {
            CardSms cardSms = new CardSms();
            cardSms.setId(cardSmsResp.getId());
            BusinessAudit businessAudit = new BusinessAudit();
            businessAudit.setCardSms(cardSms);
            businessAudit.setInfoType(SmsPushBusinessTypeEnum.CARD_SMS);
            cardSmsResp.setAuditSuggestions(businessAuditService.getAuditSuggestions(businessAudit));
        });
    }

    public long countCardSmsByName(CardSmsReq req) {
        Specification<CardSms> specification = Specifications.<CardSms>and()
                .ne(req.getId() != null, "id", req.getId())
                .eq("name", req.getName().trim())
                .build();
        return cardSmsDAO.count(specification);
    }

    public CardSms getCardSmsById(Integer id) {
        return cardSmsDAO.getOne(id);
    }

    private Predicate[] getPredicates(CardSmsReq req, Root root, CriteriaBuilder cb, List<Predicate> predicateList) {
        if (StringUtils.isNotEmpty(req.getName())) {
            Predicate namePre = cb.like(root.get("name"), "%" + req.getName() + "%");
            predicateList.add(namePre);
        }
        if (Objects.nonNull(req.getStartDate()) && Objects.nonNull(req.getEndDate())) {
            Date startDate = DateUtil.getStartTime(req.getStartDate());
            Date endDate = DateUtil.getEndTime(req.getEndDate());
            Predicate timePre = cb.between(root.get("commitTime"), startDate, endDate);
            predicateList.add(timePre);
        }
        if (Objects.nonNull(req.getType())) {
            Predicate type = cb.equal(root.get("type"), req.getType());
            predicateList.add(type);
        }
        return new Predicate[predicateList.size()];
    }

    public int offlineCardSms(CardSmsReq req) {
        Collection<Integer> ids = req.getIds();
        if (CollectionUtils.isEmpty(req.getIds())) {
            return 0;
        }
        updateButtonState(req, ButtonStateEnum.OFFLINE_HANDING.getIndex());
        int result = 0;
        Integer state = req.getState();
        Integer id = ids.stream().findFirst().get();
        RestResp response = pushCardSms(getCardSmsPushReq(id, state));
        if (response.getCode() == 0) {
            result = cardSmsDAO.updateStateByIds(state, ids);
        } else {
            result = response.getCode();
        }
        return result;
    }

    public int onlineCardSms(CardSmsReq req) {
        Collection<Integer> ids = req.getIds();
        Integer id = ids.stream().findFirst().get();
        CardSms cardSms = cardSmsDAO.findById(id).get();
        BusinessAudit businessAudit = new BusinessAudit();
        businessAudit.setInfoType(SmsPushBusinessTypeEnum.CARD_SMS);
        businessAudit.setCardSms(cardSms);
        businessAuditService.save(businessAudit);
        updateButtonState(req, ButtonStateEnum.MARKETING_DEPARTMENT_VERIFYING.getIndex());
        return cardSmsDAO.updateStateByIds(req.getState(), ids);
    }

    private void updateButtonState(CardSmsReq req, Integer state) {
        List<CardSmsButton> cardButtons = cardSmsButtonDAO.findByCardSmsIds(req.getIds());
        if (cardButtons != null) {
            for (CardSmsButton cardButton : cardButtons) {
                cardButton.setButtonState(state);
            }
        }
        cardSmsButtonDAO.saveAll(cardButtons);
    }

    @Transactional(rollbackFor = Exception.class)
    public CardSms addCardSms(CardSmsReq req) {
        CardSms cardSms = req.translate();
        User user = userService.getUserById(req.getUserId());
        cardSms.setUser(user);
        CardSms cardSmsResp = cardSmsDAO.save(cardSms);
        saveCardSmsButton(cardSmsResp, req);
        return cardSmsResp;
    }






    private void saveCardSmsButton(CardSms cardSms, CardSmsReq req) {
        String buttonName1 = req.getButtonName1();
        String buttonName2 = req.getButtonName2();
        CardSmsButton cardSmsButton = new CardSmsButton();
        cardSmsButton.setCardSms(cardSms);
        cardSmsButton.setButtonState(ButtonStateEnum.NOT_ON_LINE.getIndex());

        if (StringUtils.isNotEmpty(buttonName1)) {
            cardSmsButton.setName(req.getButtonName1());
            cardSmsButton.setUrl(req.getButtonUrl1());
            cardSmsButton.setButtonType(ButtonTypeEnum.BUTTON_TYPE_ONE.getIndex());
            cardSmsButtonDAO.save(cardSmsButton);
        }
        if (StringUtils.isNotEmpty(buttonName2)) {
            cardSmsButton.setName(req.getButtonName2());
            cardSmsButton.setUrl(req.getButtonUrl2());
            cardSmsButton.setButtonType(ButtonTypeEnum.BUTTON_TYPE_TWO.getIndex());
            cardSmsButtonDAO.save(cardSmsButton);
        }
    }


    /**
     * 原先的处理方式是当需要更新的时候先删除原有的按钮,再按照前台传递过来的信息创建新的按钮 ，会产生如下问题：
     *     1.没次创建时都会生成新的id,可能会出现某id对应的按钮审核通过后，数据库没有这个id对应的按钮。
     *     2.会创建空按钮
     *
     * @param cardSms
     * @param req
     */



    private void updateCardSmsButton(CardSms cardSms, CardSmsReq req) {
        CardSmsButton cardSmsButton1 = null;
        CardSmsButton cardSmsButton2 = null;
        String buttonName1 = req.getButtonName1();
        String buttonName2 = req.getButtonName2();

        List<CardSmsButton> cardSmsButtons = cardSmsButtonDAO.findByCardSmsId(cardSms.getId());
        if (cardSmsButtons != null) {
            for (CardSmsButton cardSmsButton : cardSmsButtons) {
                if (cardSmsButton.getButtonType() == ButtonTypeEnum.BUTTON_TYPE_ONE.getIndex()) {
                    cardSmsButton1 = cardSmsButton;
                } else if (cardSmsButton.getButtonType() == ButtonTypeEnum.BUTTON_TYPE_TWO.getIndex()) {
                    cardSmsButton2 = cardSmsButton;
                }
            }
        }

        if (cardSmsButton1 != null) {
            if (StringUtils.isNotEmpty(buttonName1)) {
                if (StringUtils.isNotEmpty(cardSmsButton1.getName())) {
                    cardSmsButton1.setName(req.getButtonName1());
                    cardSmsButton1.setUrl(req.getButtonUrl1());
                    cardSmsButtonDAO.save(cardSmsButton1);
                }
            }
            if (!StringUtils.isNotEmpty(buttonName1)) {
                if (StringUtils.isNotEmpty(cardSmsButton1.getName())) {
                    cardSmsButtonDAO.deleteByCardButtonId(cardSmsButton1.getId());
                }
            }
        } else {
            if (StringUtils.isNotEmpty(buttonName1)) {
                cardSmsButton1 = new CardSmsButton();
                cardSmsButton1.setCardSms(cardSms);
                cardSmsButton1.setName(req.getButtonName1());
                cardSmsButton1.setUrl(req.getButtonUrl1());
                cardSmsButton1.setButtonState(ButtonStateEnum.NOT_ON_LINE.getIndex());
                cardSmsButton1.setButtonType(ButtonTypeEnum.BUTTON_TYPE_ONE.getIndex());
                cardSmsButtonDAO.save(cardSmsButton1);
            }
        }

        if (cardSmsButton2 != null) {
            if (StringUtils.isNotEmpty(buttonName2)) {
                if (StringUtils.isNotEmpty(cardSmsButton2.getName())) {
                    cardSmsButton2.setName(req.getButtonName2());
                    cardSmsButton2.setUrl(req.getButtonUrl2());
                    cardSmsButtonDAO.save(cardSmsButton2);
                }
            }
            if (!StringUtils.isNotEmpty(buttonName2)) {
                if (StringUtils.isNotEmpty(cardSmsButton2.getName())) {
                    cardSmsButtonDAO.deleteByCardButtonId(cardSmsButton2.getId());
                }
            }
        } else {
            if (StringUtils.isNotEmpty(buttonName2)) {
                cardSmsButton2 = new CardSmsButton();
                cardSmsButton2.setCardSms(cardSms);
                cardSmsButton2.setName(req.getButtonName2());
                cardSmsButton2.setUrl(req.getButtonUrl2());
                cardSmsButton2.setButtonState(ButtonStateEnum.NOT_ON_LINE.getIndex());
                cardSmsButton2.setButtonType(ButtonTypeEnum.BUTTON_TYPE_ONE.getIndex());
                cardSmsButtonDAO.save(cardSmsButton2);
            }
        }


    }







    @Transactional(rollbackFor = Exception.class)
    public CardSms updateCardSms(CardSmsReq req) {
        CardSms cardSms = cardSmsDAO.getOne(req.getId());
        cardSms.setName(req.getName());
        cardSms.setContext(req.getContext());
        cardSms.setState(CardSmsStateEnum.NOT_ON_LINE.getIndex());
        CardSms cardSmsResp = cardSmsDAO.save(cardSms);
        updateCardSmsButton(cardSmsResp, req);
        return cardSmsResp;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCardSms(Integer id) {
        cardSmsDAO.deleteById(id);
        cardSmsButtonDAO.deleteByCardSmsId(id);
        businessAuditDAO.deleteByCardSmsId(id);
    }

    protected RestResp pushCardSms(CardSmsPushReq cardSmsPushReq) {
        SmpRemoteSynReq smpRemoteSynReq = new SmpRemoteSynReq();
        userService.setTokenSmpRemoteSynReq(smpRemoteSynReq);
        smpRemoteSynReq.setCardSmsPushReq(cardSmsPushReq);
        smpRemoteSynReq.setBusinessType(SmsPushBusinessTypeEnum.CARD_SMS.getIndex());
        return JacksonUtils.fromJson(remoteSynService.pushData(smpRemoteSynReq), RestResp.class);
    }

    protected CardSmsPushReq getCardSmsPushReq(Integer id, Integer state) {
        CardSms cardSms = cardSmsDAO.findById(id).get();
        List<CardSmsButton> cardSmsButtonList = cardSmsButtonDAO.findByCardSmsId(id);
        CardSmsPushReq cardSmsPushReq = new CardSmsPushReq().translate(cardSms, cardSmsButtonList);
        cardSmsPushReq.setState(state);
        return cardSmsPushReq;
    }
}
