package com.factory.longMethod.conditional.example2.service;

import com.github.wenhao.jpa.Specifications;
import com.xuanwu.smp.common.dao.BaseInfoDAO;
import com.xuanwu.smp.common.dao.BusinessAuditDAO;
import com.xuanwu.smp.common.dao.CardSmsButtonDAO;
import com.xuanwu.smp.common.dao.CardSmsDAO;
import com.xuanwu.smp.common.dao.ChannelDAO;
import com.xuanwu.smp.common.dao.SmartMenuSchemeDAO;
import com.xuanwu.smp.common.dto.req.BaseInfoReq;
import com.xuanwu.smp.common.dto.req.BaseReq;
import com.xuanwu.smp.common.dto.req.BusinessAuditReq;
import com.xuanwu.smp.common.dto.req.remote.CardButtonAuditResultReq;
import com.xuanwu.smp.common.dto.req.remote.CardButtonPushReq;
import com.xuanwu.smp.common.dto.req.remote.CardSmsPushReq;
import com.xuanwu.smp.common.dto.req.remote.ChannelPushReq;
import com.xuanwu.smp.common.dto.req.remote.SmartMenuSchemaPushReq;
import com.xuanwu.smp.common.dto.resp.BusinessAuditResp;
import com.xuanwu.smp.common.dto.resp.PageResp;
import com.xuanwu.smp.common.dto.resp.rest.RestResp;
import com.xuanwu.smp.common.entity.BusinessAudit;
import com.xuanwu.smp.common.entity.CardSms;
import com.xuanwu.smp.common.entity.CardSmsButton;
import com.xuanwu.smp.common.enumeration.AuditStateEnum;
import com.xuanwu.smp.common.enumeration.BusinessAuditStateEnum;
import com.xuanwu.smp.common.enumeration.CardSmsStateEnum;
import com.xuanwu.smp.common.enumeration.ChannelStateEnum;
import com.xuanwu.smp.common.enumeration.SmartMenuSchemeStateEnum;
import com.xuanwu.smp.common.template.PageRespTemplate;
import com.xuanwu.smp.server.dto.request.QuickAppAuditRequest;
import com.xuanwu.smp.server.service.quick.QuickAppStateContext;
import com.xuanwu.smp.server.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author FangWenJie
 * @date 2021/2/24 10:39
 */
@Service
public class BusinessAuditService {

    @Autowired
    private UserService userService;
    @Autowired
    private ChannelService channelService;
    @Autowired
    private CardSmsService cardSmsService;
    @Autowired
    private BaseInfoService baseInfoService;
    @Autowired
    private SmartMenuService smartMenuService;
    @Autowired
    private ChannelDAO channelDAO;
    @Autowired
    private CardSmsDAO cardSmsDAO;
    @Autowired
    private BaseInfoDAO baseInfoDAO;
    @Autowired
    private BusinessAuditDAO businessAuditDAO;
    @Autowired
    private CardSmsButtonDAO cardSmsButtonDAO;
    @Autowired
    private SmartMenuSchemeDAO smartMenuSchemeDAO;
    @Autowired
    private CardButtonService cardButtonService;
    @Autowired
    private QuickAppStateContext quickAppStateContext;

    public void save(BusinessAudit businessAudit) {
        businessAudit.setCreateTime(new Date());
        businessAudit.setCommitUser(userService.getUserById(SessionUtil.getCurrentUser().getUserId()));
        businessAudit.setAuditState(BusinessAuditStateEnum.MARKETING_DEPARTMENT_VERIFYING);
        businessAuditDAO.save(businessAudit);
    }

    public PageResp getUnauditedData(BaseReq baseReq) {
        Specification<BusinessAudit> specification = Specifications.<BusinessAudit>and()
                .eq("auditState", BusinessAuditStateEnum.MARKETING_DEPARTMENT_VERIFYING)
                .build();
        PageResp<BusinessAudit> pageResp = PageRespTemplate.pageQuery(businessAuditDAO, specification, baseReq, new BusinessAuditResp());
        return pageResp;
    }

    public PageResp getAuditedData(BaseReq baseReq) {
        Specification<BusinessAudit> specification = Specifications.<BusinessAudit>and()
                .ne("auditState", BusinessAuditStateEnum.MARKETING_DEPARTMENT_VERIFYING)
                .build();
        PageResp<BusinessAudit> pageResp = PageRespTemplate.pageQuery(businessAuditDAO, specification, baseReq, new BusinessAuditResp(), Sort.by(Sort.Order.desc("auditTime")));
        return pageResp;
    }

    public boolean auditById(BusinessAuditReq businessAuditReq) throws IOException {
        BusinessAudit businessAudit = businessAuditDAO.getOne(businessAuditReq.getId());
        if (BusinessAuditStateEnum.VERIFY_SUCCEED.getIndex() == businessAuditReq.getAuditState()) {
            if (pushAuditInfo(businessAudit).getCode() != 0) {
                return false;
            }
        }
        businessAudit.setAuditTime(new Date());
        businessAudit.setAuditSuggestions(businessAuditReq.getAuditSuggestions());
        businessAudit.setAuditUser(userService.getUserById(SessionUtil.getCurrentUser().getUserId()));
        businessAudit.setAuditState(BusinessAuditStateEnum.getState(businessAuditReq.getAuditState()));
        updateAuditState(businessAudit);
        return true;
    }

    private void auditQuickApp(BusinessAudit audit) {
        boolean approved = BusinessAuditStateEnum.VERIFY_SUCCEED == audit.getAuditState();
        QuickAppAuditRequest auditRequest = new QuickAppAuditRequest()
                .setTargetId(audit.getQuickApp().getId())
                .setApproved(approved)
                .setComment("");
        quickAppStateContext.audit(auditRequest);
    }

    private void auditCardButton(BusinessAudit audit) {
        boolean approved = BusinessAuditStateEnum.VERIFY_SUCCEED == audit.getAuditState();
        CardButtonAuditResultReq request = new CardButtonAuditResultReq()
                .setButtonId(audit.getCardSmsButton().getId())
                .setCardId(audit.getCardSmsButton().getCardSms().getId())
                .setApprove(approved);
        cardButtonService.pushBackButtonAuditResult(request);
    }

    @Transactional(rollbackFor = Exception.class)
    protected void updateAuditState(BusinessAudit businessAudit) {
        AuditStateEnum auditStateEnum = AuditStateEnum.convertBusinessAuditState(businessAudit.getAuditState());
        switch (businessAudit.getInfoType()) {
            case BASE_INFO:
                baseInfoDAO.updateAuditState(auditStateEnum, businessAudit.getBaseInfo().getId());
                break;
            case CHANNEL_INFO:
                channelDAO.updateAuditState(auditStateEnum.getIndex(), businessAudit.getChannel().getId());
                break;
            case CARD_SMS:
                updateCardAndButtonState(businessAudit);
                break;
            case SMART_MENU:
                smartMenuSchemeDAO.updateAuditState(SmartMenuSchemeStateEnum.convertBusinessAuditState(businessAudit.getAuditState()), businessAudit.getSmartMenuScheme().getId());
                break;
            case QUICK_APP:
                auditQuickApp(businessAudit);
                break;
            case CARD_BUTTON:
                auditCardButton(businessAudit);
                break;
            default:
                break;
        }
        businessAuditDAO.save(businessAudit);
    }

    private void updateCardAndButtonState(BusinessAudit businessAudit) {
        AuditStateEnum auditStateEnum = AuditStateEnum.convertBusinessAuditState(businessAudit.getAuditState());
        cardSmsDAO.updateAuditState(auditStateEnum.getIndex(), businessAudit.getCardSms().getId());
        List<CardSmsButton> cardButtons = cardSmsButtonDAO.findByCardSmsId(businessAudit.getCardSms().getId());
        if (cardButtons != null) {
            for (CardSmsButton cardButton : cardButtons) {
                cardButton.setButtonState(auditStateEnum.getIndex());
            }
        }
        cardSmsButtonDAO.saveAll(cardButtons);
    }

    private RestResp pushAuditInfo(BusinessAudit businessAudit) throws IOException {
        RestResp response = new RestResp();
        switch (businessAudit.getInfoType()) {
            case BASE_INFO:
                BaseInfoReq baseInfoReq = new BaseInfoReq().convert(businessAudit.getBaseInfo());
                response = baseInfoService.pushBaseInfo(baseInfoService.getBaseInfoPushReq(baseInfoReq));
                break;
            case CHANNEL_INFO:
                Integer channelId = businessAudit.getChannel().getId();
                ArrayList<Integer> ids = new ArrayList<>();
                ids.add(channelId);
                List<ChannelPushReq> channelPushReqs = channelService.getChannelPushReqs(ids, ChannelStateEnum.VERIFYING.getIndex());
                response = channelService.pushChannelInfo(channelPushReqs);
                break;
            case CARD_SMS:
                CardSmsPushReq cardSmsPushReq = cardSmsService.getCardSmsPushReq(businessAudit.getCardSms().getId(), CardSmsStateEnum.VERIFYING.getIndex());
                response = cardSmsService.pushCardSms(cardSmsPushReq);
                break;
            case SMART_MENU:
                SmartMenuSchemaPushReq smartMenuSchemaPushReq = smartMenuService.getSmartMenuSchemaPushReq(businessAudit.getSmartMenuScheme().getId(), SmartMenuSchemeStateEnum.VERIFYING.getIndex());
                response = smartMenuService.pushSmartMenu(smartMenuSchemaPushReq);
                break;
            case CARD_BUTTON:
                CardSmsButton button = businessAudit.getCardSmsButton();
                CardSms card = button.getCardSms();
                CardButtonPushReq request = new CardButtonPushReq()
                        .setButtonId(button.getId())
                        .setButtonName(button.getName())
                        .setButtonType(button.getButtonType())
                        .setCardId(card.getId())
                        .setCardName(card.getName())
                        .setCardContext(card.getContext())
                        .setEventType(2);
                cardButtonService.pushCardButton(request);
                break;
            default:
                break;
        }
        return response;
    }

    public String getAuditSuggestions(BusinessAudit businessAuditReq) {
        Specification<BusinessAudit> specification = Specifications.<BusinessAudit>and()
                .ne("auditState", BusinessAuditStateEnum.MARKETING_DEPARTMENT_VERIFYING)
                .eq("infoType", businessAuditReq.getInfoType())
                .eq(businessAuditReq.getBaseInfo() != null, "baseInfo", Optional.ofNullable(businessAuditReq.getBaseInfo()).map(baseInfo -> baseInfo.getId()).orElse(-1))
                .eq(businessAuditReq.getChannel() != null, "channel", Optional.ofNullable(businessAuditReq.getChannel()).map(channel -> channel.getId()).orElse(-1))
                .eq(businessAuditReq.getCardSms() != null, "cardSms", Optional.ofNullable(businessAuditReq.getCardSms()).map(cardSms -> cardSms.getId()).orElse(-1))
                .eq(businessAuditReq.getSmartMenuScheme() != null, "smartMenuScheme", Optional.ofNullable(businessAuditReq.getSmartMenuScheme()).map(scheme -> scheme.getId()).orElse(-1))
                .build();
        List<BusinessAudit> all = businessAuditDAO.findAll(specification, Sort.by(Sort.Direction.DESC, "auditTime"));
        return all.isEmpty() ? null : "【市场部】" + all.get(0).getAuditSuggestions();
    }
}
